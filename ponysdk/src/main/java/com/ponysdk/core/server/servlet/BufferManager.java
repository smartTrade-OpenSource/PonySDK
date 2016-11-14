/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.server.servlet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferManager {

    private static final Logger log = LoggerFactory.getLogger(BufferManager.class);

    private static final int NUMBER_OF_BUFFERS = 300;
    private static final int MAX_BUFFERS = 1000;
    private static final int DEFAULT_BUFFER_SIZE = 512000;

    private final ExecutorCompletionService<Task> service;

    private final BlockingQueue<Buffer> bufferPool = new LinkedBlockingQueue<>();
    private final List<Buffer> buffers = new ArrayList<>(MAX_BUFFERS);

    public BufferManager() {
        log.info("Initializing Buffer allocation ...");

        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            bufferPool.add(new Buffer());
        }
        buffers.addAll(bufferPool);

        service = new ExecutorCompletionService<>(Executors.newSingleThreadExecutor());

        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Future<Task> future;
                while ((future = service.take()) != null) {
                    final Task task = future.get();
                    if (task.isDone()) {
                        task.buffer.clear();
                        bufferPool.offer(task.buffer);
                    } else {
                        service.submit(task);
                    }
                }
            } catch (final Exception e) {
                log.error(e.getMessage(), e);
            }
        });

        log.info("Buffer allocation initialized {}", DEFAULT_BUFFER_SIZE * bufferPool.size());
    }

    public Buffer allocate() {
        Buffer buffer = bufferPool.poll();
        if (buffer == null) {
            if (buffers.size() < MAX_BUFFERS) {
                log.info("No more available buffer (size : {}), allocating a new one", buffers.size());
                buffer = new Buffer();
                buffers.add(buffer);
            } else {
                log.info("No more available buffer, max allocation reached ({}), waiting an available one", MAX_BUFFERS);
                try {
                    return bufferPool.poll(25, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        }
        return buffer;
    }

    public void release(final Buffer buffer, final Future<Void> future) {
        if (future.isDone()) {
            buffer.clear();
            bufferPool.offer(buffer);
        } else {
            service.submit(new Task(buffer, future));
        }
    }

    public static final class Task implements Callable<Task> {

        final Buffer buffer;
        final Future<Void> future;

        public Task(final Buffer buffer, final Future<Void> future) {
            this.buffer = buffer;
            this.future = future;
        }

        @Override
        public Task call() throws Exception {
            return this;
        }

        public boolean isDone() {
            return future.isDone();
        }

    }

    public static final class Buffer {

        private final ByteBuffer socketBuffer;

        Buffer() {
            socketBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
        }

        public int position() {
            return socketBuffer.position();
        }

        public void put(final byte value) {
            socketBuffer.put(value);
        }

        public void putShort(final short value) {
            socketBuffer.putShort(value);
        }

        public void putInt(final int value) {
            socketBuffer.putInt(value);
        }

        public void putLong(final long value) {
            socketBuffer.putLong(value);
        }

        public void clear() {
            socketBuffer.clear();
        }

        ByteBuffer getRawBuffer() {
            return socketBuffer;
        }
    }

}
