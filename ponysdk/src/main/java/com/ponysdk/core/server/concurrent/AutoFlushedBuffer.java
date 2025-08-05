/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.server.concurrent;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * A buffer that can be asynchronously flushed to another destination in a lock-free manner.<br>
 * Flushes are automatically triggered when there are enough pending data, although client code can
 * trigger a flush.<br>
 * Flushes are serialized: if a flush is already in progress, next flush will occurs when the
 * current one is finished.<br>
 * The putXXX() writing methods are allowed to block if the buffer is full because the flushing
 * logic is not efficient enough.
 * When the call is blocked for too long, a call to close() is triggered and the buffer is not
 * available for further writes.
 * This class is intended to be used by a single writer thread, client code should provide its own
 * synchronization between writers in a multi-threaded environment.<br>
 * The buffer usage can be observed by any thread, in order to avoid taking an external lock and
 * send a non-important message if a usage threshold has been reached.<br>
 */
public abstract class AutoFlushedBuffer implements Closeable {

    /**
     * The implementation of the flushing logic. Flushing should be done asynchronously and is
     * supposed to call the callback methods
     * {@link AutoFlushedBuffer#onFlushCompletion() onFlushCompletion} or
     * {@link AutoFlushedBuffer#onFlushFailure(Exception) onFlushFailed} afterwards.
     *
     * @param bufferToFlush a ready to read {@link ByteBuffer} that contains the data to flush.
     */
    protected abstract void doFlush(ByteBuffer bufferToFlush);

    /**
     * Release resources associated to the flushing mechanism. Will be called at most once.
     *
     * @throws IOException
     */
    protected abstract void closeFlusher() throws IOException;

    // the ringbuffer that holds data
    // invariant : the range [position, limit[ is always available for write, it cannot contains pending data
    private final ByteBuffer writeBuffer;

    private final int bufferSize;
    private final int maxChunkSize;
    private final ByteBuffer flushBuffer; // read-only view of write buffer used by the flushing logic. Invariant: flushBuffer.remaining() <= maxChunkSize

    private final long freeSpaceThreshold;
    private final long timeoutNanos; //0 means immediate failure if buffer is full, use Long.MAX_VALUE to implement infinite wait.

    // producer / consumer synchronization point to initiate new flush and stop flushing
    // If 0, there is no pending flush so producer thread is guaranteed to have a stable view of consumerIndex and is allow to write it
    // otherwise consumer thread is expected to flush until that index and CAS flushIndex to 0
    private final AtomicLong flushIndex = new AtomicLong(0L);

    // used to indicate that there is no data between the index and the end of the buffer and that part should be ignored by the flushing logic.
    // Set by producer thread and reset by flushing thread.
    // to avoid dirty read, the producer thread can assume it is 0 if consumerIndex is in the same buffer iteration. It can also be safely read after reading flushIndex == 0
    // the flushing thread can assume it is 0 if flushIndex is in the same buffer iteration (even if it is not 0, the value won't be needed to handle the flush)
    // it can be safely read by the flushing thread after observing that flushIndex is in the next buffer iteration.
    private long paddingIndex = 0;

    ////// flusher thread owned field
    private volatile long consumerIndex = 0; //invariant : flushIndex.get() == 0 || consumerIndex <= flushIndex.get()

    ////// producer thread owned fields.
    // invariants: producerIndex >= consumerIndex >= consumerIndexCache, producerIndex <= max(consumerIndex, flushIndex) + maxChunkSize
    private long producerIndex = 0L; //no need to use volatile since we expect a single producer thread, and flushing thread don't need it;
    private long consumerIndexCache = 0L; //cached value to avoid volatile read of consumerIndex

    private volatile Exception asyncException = null;
    private volatile boolean closed = false;
    private volatile Thread waiterThread = null;

    /**
     * Default constructor : 64kB direct buffer with a 4kB flush size, a 25% free space threshold
     * and a 30 second timeout
     */
    protected AutoFlushedBuffer() {
        this(1 << 16, true, 1 << 12, 0.25f, TimeUnit.SECONDS.toMillis(30));
    }

    /**
     * @param bufferSize                 the size of the underlying buffer to allocate. Must be a power of 2 and greater
     *                                   than 32
     * @param useDirectBuffer            should the underlying buffer be allocated off heap. Should be true if there are
     *                                   real IO operations underneath, can be false for debugging purpose
     * @param maxChunkSize               the maximum size of pending data before triggering a flush automatically, must be
     *                                   between 8 and {@code bufferSize / 4}
     * @param urgentMessageReservedRatio used by {@link AutoFlushedBuffer#shouldOnlyWriteUrgentMessages()
     *                                   shouldOnlyWriteUrgentMessages}.
     *                                   If the free space ratio in the buffer is less than this value, that method should
     *                                   returns {@code true}
     * @param timeoutMillis              the timeout period in millisecond. If write to the buffer are blocked for longer
     *                                   than this, it will be automatically closed
     */
    protected AutoFlushedBuffer(final int bufferSize, final boolean useDirectBuffer, final int maxChunkSize,
                                final float urgentMessageReservedRatio, final long timeoutMillis) {
        if ((bufferSize & bufferSize - 1) != 0) {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }
        if (bufferSize < 32) {
            throw new IllegalArgumentException("bufferSize must be at least 32");
        }
        if (maxChunkSize < 8 || maxChunkSize > bufferSize / 4) {
            throw new IllegalArgumentException("maxChunkSize must be between 8 and bufferSize / 4");
        }
        this.bufferSize = bufferSize;
        this.maxChunkSize = maxChunkSize;
        writeBuffer = useDirectBuffer ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
        flushBuffer = writeBuffer.asReadOnlyBuffer();
        freeSpaceThreshold = (long) (urgentMessageReservedRatio * bufferSize);
        timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    }

    /**
     * Writes a {@code byte} in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full
     *
     * @param b the byte to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer put(final byte b) throws IOException {
        ensureCapacity(1);
        writeBuffer.put(b);
        notifyWrite(1);
        return this;
    }

    /**
     * Writes a {@code short} in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full
     *
     * @param s the short to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer putShort(final short s) throws IOException {
        ensureCapacity(2);
        writeBuffer.putShort(s);
        notifyWrite(2);
        return this;
    }

    /**
     * Writes an {@code int} in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full
     *
     * @param i the int to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer putInt(final int i) throws IOException {
        ensureCapacity(4);
        writeBuffer.putInt(i);
        notifyWrite(4);
        return this;
    }

    /**
     * Writes a {@code long} in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full
     *
     * @param l the long to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer putLong(final long l) throws IOException {
        ensureCapacity(8);
        writeBuffer.putLong(l);
        notifyWrite(8);
        return this;
    }

    /**
     * Writes a {@code char} in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full
     *
     * @param c the char to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer putChar(final char c) throws IOException {
        ensureCapacity(2);
        writeBuffer.putChar(c);
        notifyWrite(2);
        return this;
    }

    /**
     * Writes a {@code float} in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full
     *
     * @param f the float to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer putFloat(final float f) throws IOException {
        ensureCapacity(4);
        writeBuffer.putFloat(f);
        notifyWrite(4);
        return this;
    }

    /**
     * Writes a {@code double} in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full
     *
     * @param d the double to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer putDouble(final double d) throws IOException {
        ensureCapacity(8);
        writeBuffer.putDouble(d);
        notifyWrite(8);
        return this;
    }

    /**
     * Writes a {@code byte} array in the buffer. This method may block up to the configured timeout
     * period if the buffer is already full.
     * This method is an equivalent of
     *
     * <pre>
     * put(bytes, 0, bytes.length)
     * </pre>
     *
     * @param bytes the byte array to write
     * @return this buffer
     * @throws InterruptedIOException if the current thread is interrupted
     * @throws IOException            if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                mechanism reported an issue
     */
    public final AutoFlushedBuffer put(final byte[] bytes) throws IOException {
        return put(bytes, 0, bytes.length);
    }

    /**
     * Writes a part of a {@code byte} array in the buffer. This method may block up to the
     * configured timeout period if the buffer is already full.
     *
     * @param bytes  the byte array to write
     * @param offset the offset within the array of the first byte to write;
     *               must be non-negative and no larger than bytes.length
     * @param length the number of bytes to write;
     *               must be non-negative and no larger than bytes.length - offset
     * @return this buffer
     * @throws IndexOutOfBoundsException if the preconditions on the offset and length parameters do not
     *                                   hold
     * @throws InterruptedIOException    if the current thread is interrupted
     * @throws IOException               if a timeout occurs or this buffer is already closed or the underlying flushing
     *                                   mechanism reported an issue
     */
    public final AutoFlushedBuffer put(final byte[] bytes, int offset, int length) throws IOException {
        while (length > 0) {
            ensureCapacity(length);
            final int chunkLength = Math.min(length, writeBuffer.remaining());
            writeBuffer.put(bytes, offset, chunkLength);
            length -= chunkLength;
            offset += chunkLength;
            notifyWrite(chunkLength);
        }
        return this;
    }

    /**
     * Indicate if the buffer usage has reach the configured threshold. The purpose is to avoid
     * filling the buffer with messages than can be throttled and deliver
     * most important message sooner instead (and probably write them in the buffer without blocking
     * since the buffer should not be full).<br>
     * This method can be called by any thread without any additional synchronization. As a
     * consequence it is only a rough indication,
     * especially when no manual flush is triggered and the buffer size is not a lot greater than
     * the flush size.
     */
    public final boolean shouldOnlyWriteUrgentMessages() {
        //if there is no pending flush, we assume that there is enough space to accept non-urgent message (i.e freeSpaceThreshold > maxChunkSize)
        //otherwise we use flushIndex as an approximation for producerIndex : that's the best approximation we can have without synchronization
        final long currentFlushIndex = flushIndex.get();

        //if there is an exception that is still not notified to the producer, we want it to try to write ASAP so we can notify it
        return asyncException == null
                && (currentFlushIndex == 0 || currentFlushIndex - consumerIndex + freeSpaceThreshold > bufferSize);
    }

    /**
     * Trigger an asynchronous flush. This method does not block. If there is already a flush in
     * progress, the actual flush will occurs on current flush completion.
     *
     * @throws IOException if already closed or if the flushing logic already reported an issue
     */
    public void flush() throws IOException {
        checkLiveness();
        tryStartFlush();
    }

    /**
     * Close this buffer and the underlying flushing mechanism. Any further attempt to write or
     * flush will cause an {@link IOException} to be thrown.
     * This method can be invoked at any time by any thread, and has no effect if the buffer is
     * already closed.
     */
    @Override
    public synchronized final void close() throws IOException {
        //FIXME: wait for flush on close ? or javadoc data may be lost
        if (!closed) {
            closed = true;
            LockSupport.unpark(waiterThread);
            closeFlusher();
        }
    }

    /**
     * Tells whether or not this buffer is closed.
     *
     * @return true if, and only if, this buffer is closed
     */
    public final boolean isClosed() {
        return closed;
    }

    /**
     * Callback that should be called by the flushing logic when it successfully flushed the data.
     * We expect that {@code flushBuffer.limit()} has not been modified by the flushing logic and
     * {@code flushBuffer.hasRemaining()} to return false.
     *
     * @see AutoFlushedBuffer#doFlush(ByteBuffer)
     */
    protected final void onFlushCompletion() {
        if (flushBuffer.hasRemaining()) {
            //bug in the underlying flush system such as a bad handling of interruptions
            onFlushFailure(new IOException("flush completed without exception, but flushBuffer still have remaining data"));
            return;
        }

        final int sizeMask = bufferSize - 1;
        final long currentConsumerIndex = consumerIndex;
        long newConsumerIndex = (currentConsumerIndex & ~sizeMask) + flushBuffer.limit();
        final long nextBufferIteration = (currentConsumerIndex | sizeMask) + 1;

        long currentFlushIndex = flushIndex.get();
        if (currentFlushIndex >= nextBufferIteration && newConsumerIndex == paddingIndex) {
            //fill padding gap. observing flushIndex >= nextBufferIteration is mandatory to avoid a dirty read of paddingIndex
            //this is an optimisation to avoid calling doFlush with an empty buffer in most case, however this can sometime happen
            //if we lose next CAS.
            newConsumerIndex = nextBufferIteration;
            paddingIndex = 0;
        }

        consumerIndex = newConsumerIndex;
        LockSupport.unpark(waiterThread);

        if (!flushIndex.compareAndSet(newConsumerIndex, 0) && !closed) {
            //a new flush has already been requested => continue flush

            currentFlushIndex = flushIndex.get();
            configureFlushBuffer(newConsumerIndex, currentFlushIndex);
            doFlush(flushBuffer);
        }
    }

    /**
     * Callback that should be called by the flushing logic did not successfully flushed.
     * After invoking this method, the buffer is closed and any further try to write or flush the
     * buffer will cause an {@link IOException} to be thrown
     *
     * @see AutoFlushedBuffer#doFlush(ByteBuffer)
     */
    protected final void onFlushFailure(final Exception ex) {
        asyncException = ex == null ? new IOException() : ex;
        LockSupport.unpark(waiterThread);
    }

    private void checkLiveness() throws IOException {
        final Throwable t = asyncException;
        if (t != null) {
            close();
            asyncException = null;
            throw new IOException("Asynchronous flush failed", t);
        } else if (isClosed()) {
            throw new IOException("Already closed");
        }
    }

    // make sure (by waiting if needed) that at least writeBuffer.remaining() >= Math.min(length, maxChunkSize)
    private void ensureCapacity(final int length) throws IOException {
        if (Thread.interrupted()) {
            throw new InterruptedIOException();
        }
        checkLiveness();

        final int minLength = Math.min(length, maxChunkSize);
        if (writeBuffer.remaining() < minLength) {
            //update writeBuffer
            //we want to avoid that code path to be inlined ...
            doEnsureCapacity(minLength);
        }
    }

    private void doEnsureCapacity(final int minLength) throws IOException {
        //auto-flush if we reach end of buffer
        if (writeBuffer.position() + minLength > writeBuffer.capacity()) {
            final int paddingSize = writeBuffer.capacity() - writeBuffer.position();
            if (paddingSize != 0) {
                //before setting paddingIndex, we want it to have been reset by the consumer
                //however waiting for that without synchronization will be optimized by the JIT
                //and become an infinite loop
                //=> we wait for consumerIndex to be in the same buffer iteration than producer index instead.
                waitForConsumer(producerIndex & ~(bufferSize - 1));
                paddingIndex = producerIndex;
                producerIndex += paddingSize;
                tryStartFlush(); //flush until buffer end
            }
            writeBuffer.position(0);

        }

        //wait until there is enough space
        waitForConsumer(producerIndex - (bufferSize - minLength));
        updateWriteBufferLimit();
    }

    private void waitForConsumer(final long minConsumerIndex) throws IOException {
        if (consumerIndexCache < minConsumerIndex) {
            final long initialNanos = System.nanoTime();

            waiterThread = Thread.currentThread();
            try {
                while ((consumerIndexCache = consumerIndex) < minConsumerIndex) {
                    checkLiveness();
                    final long remainingNanos = initialNanos + timeoutNanos - System.nanoTime();
                    if (remainingNanos <= 0) {
                        close();
                        throw new IOException("Timeout: flushing mechanism didn't succeed to catchup in the required "
                                + TimeUnit.NANOSECONDS.toMillis(timeoutNanos) + "ms time budget");
                    }
                    LockSupport.parkNanos(remainingNanos);
                    if (Thread.interrupted()) {
                        close();
                        throw new InterruptedIOException();
                    }
                }
            } finally {
                waiterThread = null;
            }
        }
    }

    //update the data structure after a write to the buffer. Triggers flush automatically if needed
    private void notifyWrite(final int length) throws IOException {
        producerIndex += length;

        //auto-flush if more than maxChunkSize data
        int pendingDataLength = (int) (producerIndex - consumerIndexCache);
        //fast path, avoid volatile read of consumerIndex if possible
        if (pendingDataLength >= maxChunkSize) {
            //possible auto flush required => update consumerIndexCache and buffer limit
            consumerIndexCache = consumerIndex;
            pendingDataLength = (int) (producerIndex - consumerIndexCache);
            updateWriteBufferLimit();
        }

        //auto-flush if more than maxChunkSize data
        if (pendingDataLength >= maxChunkSize && producerIndex - flushIndex.get() >= maxChunkSize) {
            tryStartFlush();
        }

        //auto flush on end of buffer;
        //TODO: pad & flush if position + maxChunkSize > capacity ?
        if ((producerIndex & bufferSize - 1) == 0) {
            writeBuffer.position(0);
            updateWriteBufferLimit();
            tryStartFlush();
        }
    }

    //must be called whenever producer thread updates consumerIndexCache or writeBuffer.position() is set to 0 (cf ensureCapacity fast path ...)
    private void updateWriteBufferLimit() {

        final int limit = (int) consumerIndexCache & bufferSize - 1;
        final boolean sameBufferIteration = writeBuffer.position() > limit || consumerIndexCache == producerIndex;
        //if consumer and producer in the same buffer iteration => write OK until the end of buffer
        //if producer is one iteration late, write only to consumerIndex
        writeBuffer.limit(sameBufferIteration ? writeBuffer.capacity() : limit);

    }

    //called by producer thread to trigger a new flush if needed or to extend a current flush
    private void tryStartFlush() {

        final long currentFlushIndex = flushIndex.get();

        if (currentFlushIndex != 0) {
            //a flush is already in progress => try to update it
            final long nextFlushIndex = nextFlushIndex(currentFlushIndex);
            if (flushIndex.compareAndSet(currentFlushIndex, nextFlushIndex)) {
                //update successful, otherwise consumer would have updated flushIndex to 0
                return;
            }
        }

        //no pending flush -> configure flushBuffer and trigger a new one. consumerIndex and paddingIndex are guaranteed to be stable
        //since the successful CAS is the last instruction in onFlushCompletion().
        if ((consumerIndexCache = consumerIndex) != producerIndex) {
            //real flush
            configureFlushBuffer(consumerIndexCache, producerIndex);
            final long nextFlushIndex = nextFlushIndex(consumerIndexCache);
            flushIndex.set(nextFlushIndex);
            doFlush(flushBuffer);
        }
        updateWriteBufferLimit();
    }

    private long nextFlushIndex(final long lastFlushIndex) {
        long nextFlushIndex = producerIndex;
        if (producerIndex > lastFlushIndex + maxChunkSize && (producerIndex & bufferSize - 1) != 0) {
            //if pending data > maxChunkSize, that is an auto-flush (otherwise invariants are broken) => flush size = multiple of maxChunkSize
            //however if we trigger a flush just after configuring padding, we should go to producerIndex in any case (which is end of buffer)
            nextFlushIndex = producerIndex - (producerIndex - lastFlushIndex) % maxChunkSize;
        }
        return nextFlushIndex;
    }

    private void configureFlushBuffer(final long currentConsumerIndex, final long flushUpToIndex) {
        final int sizeMask = bufferSize - 1;
        long endOfBufferIndex = (currentConsumerIndex | sizeMask) + 1;
        //reading paddingIndex is not a dirty read since we read earlier than we should flush up to next buffer iteration
        if (flushUpToIndex >= endOfBufferIndex && paddingIndex != 0) {
            endOfBufferIndex = paddingIndex;
        }
        long endIndex = Math.min(flushUpToIndex, currentConsumerIndex + maxChunkSize);
        endIndex = Math.min(endIndex, endOfBufferIndex);

        flushBuffer.limit(flushBuffer.capacity()); // avoid exception when setting position
        flushBuffer.position((int) currentConsumerIndex & sizeMask);
        flushBuffer.limit(1 + ((int) (endIndex - 1) & sizeMask));
    }
}
