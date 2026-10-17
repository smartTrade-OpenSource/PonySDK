/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.websocket.ByteBufferPool;
import com.ponysdk.core.server.websocket.WebSocketPusher;

/** The exact bytes of protocol fields, from the framework's own encoder (not a copy of its rules). */
final class FrameAnatomy {

    /** One field of a frame: its model, its value and the bytes the encoder wrote for it. */
    record Field(ServerToClientModel model, Object value, byte[] bytes) {}

    private FrameAnatomy() {
    }

    /** Encodes the fields one by one, each flushed alone so that its bytes are known. */
    static List<Field> encode(final List<Field> fields) {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();
        final Session session = (Session) Proxy.newProxyInstance(Session.class.getClassLoader(), new Class<?>[] { Session.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "sendBinary" -> {
                    final ByteBuffer buffer = (ByteBuffer) args[0];
                    final byte[] chunk = new byte[buffer.remaining()];
                    buffer.get(chunk);
                    sent.writeBytes(chunk);
                    ((Callback) args[1]).succeed();
                    yield null;
                }
                case "isOpen" -> true;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            });
        final WebSocketPusher encoder = new WebSocketPusher(session, 1 << 12, 1_000, new ByteBufferPool(1 << 12, 1));
        final List<Field> out = new ArrayList<>(fields.size());
        try {
            for (final Field field : fields) {
                encoder.encode(field.model(), field.value());
                encoder.flush();
                out.add(new Field(field.model(), field.value(), sent.toByteArray()));
                sent.reset();
            }
            encoder.close();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    static String hex(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (final byte b : bytes) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)))
                .append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
        }
        return sb.toString();
    }
}
