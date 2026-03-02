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

package com.ponysdk.core.terminal.model;

import com.ponysdk.core.model.ClientToServerModel;

import elemental2.core.ArrayBuffer;
import elemental2.core.DataView;
import elemental2.core.Uint8Array;

/**
 * Binary writer for client-to-server messages.
 * Encodes ClientToServerModel keys and values into a compact binary format,
 * replacing the previous JSON text protocol for reduced bandwidth and faster parsing.
 * <p>
 * Wire format per field:
 * - 1 byte: model key (ordinal of ClientToServerModel)
 * - 1 byte: value type tag (0=null, 1=boolean, 2=int, 3=long/double, 4=string)
 * - N bytes: value payload
 * <p>
 * Messages are terminated by a 0xFF sentinel byte.
 */
public class WriterBuffer {

    private static final int INITIAL_CAPACITY = 512;
    private static final int TAG_NULL = 0;
    private static final int TAG_BOOLEAN = 1;
    private static final int TAG_INT = 2;
    private static final int TAG_DOUBLE = 3;
    private static final int TAG_STRING = 4;
    /** Sentinel marking end of a message. */
    public static final int END_MARKER = 0xFF;

    private Uint8Array buffer;
    private DataView dataView;
    private int position;

    public WriterBuffer() {
        allocate(INITIAL_CAPACITY);
    }

    private void allocate(final int capacity) {
        final ArrayBuffer ab = new ArrayBuffer(capacity);
        this.buffer = new Uint8Array(ab);
        this.dataView = new DataView(ab);
        this.position = 0;
    }

    private void ensureCapacity(final int needed) {
        if (position + needed > buffer.byteLength) {
            int newCap = buffer.byteLength * 2;
            while (newCap < position + needed) newCap *= 2;
            final ArrayBuffer ab = new ArrayBuffer(newCap);
            final Uint8Array newBuf = new Uint8Array(ab);
            newBuf.set(buffer);
            this.buffer = newBuf;
            this.dataView = new DataView(ab);
        }
    }

    /** Resets the buffer for reuse. */
    public void clear() {
        this.position = 0;
    }

    /** Writes a key with no value (null/flag). */
    public void putKey(final ClientToServerModel key) {
        ensureCapacity(2);
        buffer.setAt(position++, (double) key.ordinal());
        buffer.setAt(position++, (double) TAG_NULL);
    }

    /** Writes a key with a boolean value. */
    public void putBoolean(final ClientToServerModel key, final boolean value) {
        ensureCapacity(3);
        buffer.setAt(position++, (double) key.ordinal());
        buffer.setAt(position++, (double) TAG_BOOLEAN);
        buffer.setAt(position++, value ? 1.0 : 0.0);
    }

    /** Writes a key with an int value (4 bytes big-endian). */
    public void putInt(final ClientToServerModel key, final int value) {
        ensureCapacity(6);
        buffer.setAt(position++, (double) key.ordinal());
        buffer.setAt(position++, (double) TAG_INT);
        dataView.setInt32(position, value);
        position += 4;
    }

    /** Writes a key with a double value (8 bytes). */
    public void putDouble(final ClientToServerModel key, final double value) {
        ensureCapacity(10);
        buffer.setAt(position++, (double) key.ordinal());
        buffer.setAt(position++, (double) TAG_DOUBLE);
        dataView.setFloat64(position, value);
        position += 8;
    }

    /** Writes a key with a string value (UTF-8 encoded). */
    public void putString(final ClientToServerModel key, final String value) {
        final Uint8Array encoded = encodeUTF8(value);
        final int len = encoded.byteLength;
        // key(1) + tag(1) + length(4) + data(len)
        ensureCapacity(6 + len);
        buffer.setAt(position++, (double) key.ordinal());
        buffer.setAt(position++, (double) TAG_STRING);
        dataView.setInt32(position, len);
        position += 4;
        buffer.set(encoded, position);
        position += len;
    }

    /** Writes the end-of-message sentinel. */
    public void putEnd() {
        ensureCapacity(1);
        buffer.setAt(position++, (double) END_MARKER);
    }

    /** Returns a trimmed copy of the buffer ready to send. */
    public ArrayBuffer toArrayBuffer() {
        return ((Uint8Array) buffer.subarray(0, position)).buffer;
    }

    /** Returns current write position (message size in bytes). */
    public int size() {
        return position;
    }

    private static native Uint8Array encodeUTF8(String s) /*-{
        return new $wnd.TextEncoder().encode(s);
    }-*/;
}
