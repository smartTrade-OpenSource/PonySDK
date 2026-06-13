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

package com.ponysdk.core.server.websocket;

import com.ponysdk.core.model.ClientToServerModel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Reads binary-encoded client-to-server messages from a ByteBuffer.
 * <p>
 * Wire format per field:
 * - 1 byte: model key (ordinal of ClientToServerModel)
 * - 1 byte: value type tag (0=null, 1=boolean, 2=int, 3=double, 4=string)
 * - N bytes: value payload
 * <p>
 * Messages are terminated by a 0xFF sentinel byte.
 */
public class ClientBinaryReader {

    private static final int TAG_NULL = 0;
    private static final int TAG_BOOLEAN = 1;
    private static final int TAG_INT = 2;
    private static final int TAG_DOUBLE = 3;
    private static final int TAG_STRING = 4;
    public static final int END_MARKER = 0xFF;

    private static final ClientToServerModel[] MODELS = ClientToServerModel.values();

    private final ByteBuffer buffer;

    public ClientBinaryReader(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Returns true if there are more fields to read in the current message.
     */
    public boolean hasNext() {
        return buffer.hasRemaining() && (buffer.get(buffer.position()) & 0xFF) != END_MARKER;
    }

    /**
     * Skips the end-of-message sentinel. Call after processing all fields.
     */
    public void skipEnd() {
        if (buffer.hasRemaining()) {
            final int b = buffer.get() & 0xFF;
            if (b != END_MARKER) {
                throw new IllegalStateException("Expected END_MARKER (0xFF), got: " + b);
            }
        }
    }

    /**
     * Returns true if there are more messages in the buffer.
     */
    public boolean hasMoreMessages() {
        return buffer.hasRemaining();
    }

    /**
     * Reads the next field key.
     *
     * @throws IllegalStateException if the ordinal does not map to a known {@link ClientToServerModel}
     *                               (untrusted input must not index the array out of bounds)
     */
    public ClientToServerModel readKey() {
        final int ordinal = buffer.get() & 0xFF;
        if (ordinal >= MODELS.length) {
            throw new IllegalStateException("Invalid ClientToServerModel ordinal: " + ordinal
                    + " (valid range 0.." + (MODELS.length - 1) + ")");
        }
        return MODELS[ordinal];
    }

    /**
     * Reads the value type tag and returns it.
     */
    public int readTag() {
        return buffer.get() & 0xFF;
    }

    /**
     * Reads a boolean value (after tag has been read).
     */
    public boolean readBoolean() {
        return buffer.get() != 0;
    }

    /**
     * Reads an int value (after tag has been read).
     */
    public int readInt() {
        return buffer.getInt();
    }

    /**
     * Reads a double value (after tag has been read).
     */
    public double readDouble() {
        return buffer.getDouble();
    }

    /**
     * Reads a string value (after tag has been read).
     */
    public String readString() {
        final int len = readLength();
        final byte[] bytes = new byte[len];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Skips the value payload based on the tag.
     *
     * @throws IllegalStateException on an unknown tag (which would otherwise desync the parser)
     */
    public void skipValue(final int tag) {
        switch (tag) {
            case TAG_NULL -> { /* nothing */ }
            case TAG_BOOLEAN -> buffer.get();
            case TAG_INT -> buffer.getInt();
            case TAG_DOUBLE -> buffer.getDouble();
            case TAG_STRING -> {
                final int len = readLength(); // advances past the 4-byte prefix first
                buffer.position(buffer.position() + len);
            }
            default -> throw new IllegalStateException("Unknown value type tag: " + tag);
        }
    }

    /**
     * Reads a length prefix from the (untrusted) stream and validates it. A length must be
     * non-negative and must not exceed the bytes actually remaining in the buffer — otherwise a
     * malicious or buggy client could claim a huge length and trigger an unbounded allocation
     * ({@code new byte[len]}) or a negative array size, i.e. an OutOfMemoryError / crash (DoS).
     *
     * @throws IllegalStateException if the prefix is truncated or the length is out of bounds
     */
    private int readLength() {
        if (buffer.remaining() < Integer.BYTES) {
            throw new IllegalStateException("Truncated length prefix");
        }
        final int len = buffer.getInt();
        if (len < 0 || len > buffer.remaining()) {
            throw new IllegalStateException("Invalid length: " + len + " (bytes remaining: " + buffer.remaining() + ")");
        }
        return len;
    }

    // Convenience: tag constants for external use
    public static int tagNull() { return TAG_NULL; }
    public static int tagBoolean() { return TAG_BOOLEAN; }
    public static int tagInt() { return TAG_INT; }
    public static int tagDouble() { return TAG_DOUBLE; }
    public static int tagString() { return TAG_STRING; }
}
