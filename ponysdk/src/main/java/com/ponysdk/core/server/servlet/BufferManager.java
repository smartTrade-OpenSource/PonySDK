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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferManager {

    private static final Logger log = LoggerFactory.getLogger(BufferManager.class);

    private static final int NUMBER_OF_BUFFERS = 50;
    private static final int MAX_BUFFERS = 500;
    private static final int DEFAULT_BUFFER_SIZE = 512000;

    private final BlockingQueue<ByteBuffer> bufferPool = new LinkedBlockingQueue<>(MAX_BUFFERS);
    private final List<ByteBuffer> buffers = new ArrayList<>(MAX_BUFFERS);

    public BufferManager() {
        log.info("Initializing Buffer allocation ...");

        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            bufferPool.add(createBuffer());
        }
        buffers.addAll(bufferPool);

        log.info("Buffer allocation initialized {}", DEFAULT_BUFFER_SIZE * bufferPool.size());
    }

    public ByteBuffer allocate() {
        ByteBuffer buffer = bufferPool.poll();

        if (buffer == null) {
            if (buffers.size() < MAX_BUFFERS) {
                log.debug("No more available buffer (size : {}), allocating a new one", buffers.size());
                buffer = createBuffer();
                buffers.add(buffer);
            } else {
                log.warn("No more available buffer, max allocation reached ({}), waiting an available one", MAX_BUFFERS);
                try {
                    buffer = bufferPool.poll(25, TimeUnit.SECONDS);
                    if (buffer == null) throw new IllegalStateException("No more buffer available");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        }

        // Verify the buffer is good to be used
        if (buffer.position() == 0 && buffer.limit() == DEFAULT_BUFFER_SIZE) {
            return buffer;
        } else {
            // Discard this buffer, let it be gc
            log.warn("A zombie buffer has been detected, it will be eliminated",
                    new IllegalStateException("Zombie buffer detected : " + buffer.toString()));
            buffers.remove(buffer);
            return allocate();
        }
    }

    public void send(final RemoteEndpoint remote, final ByteBuffer buffer) {
        buffer.flip();
        remote.sendBytes(buffer, new WriteCallback() {

            @Override
            public void writeSuccess() {
                release(buffer);
            }

            @Override
            public void writeFailed(final Throwable e) {
                log.error("Can't write the buffer on the websocket", e);
                release(buffer);
            }
        });
    }

    public void release(final ByteBuffer buffer) {
        if (bufferPool.contains(buffer)) return;
        buffer.clear();
        try {
            bufferPool.put(buffer);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static final ByteBuffer createBuffer() {
        return ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
    }

}
