/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import java.nio.ByteBuffer;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.internal.PerMessageDeflateExtension;

public class PonyPerMessageDeflateExtension extends PerMessageDeflateExtension {

    public static final String NAME = new PonyPerMessageDeflateExtension().getName();
    private WebSocket.Listener webSocketListener;

    public PonyPerMessageDeflateExtension() {
        super();
    }

    @Override
    public void onFrame(Frame frame, Callback callback) {
        if (webSocketListener != null)
            webSocketListener.onIncomingWebSocketFrame(getFrameHeaderLength(frame), frame.getPayloadLength());
        super.onFrame(frame, callback);
    }

    @Override
    public void sendFrame(Frame frame, Callback callback, boolean batch) {
        if (webSocketListener != null)
            webSocketListener.onOutgoingWebSocketFrame(getFrameHeaderLength(frame), frame.getPayloadLength());
        super.sendFrame(frame, callback, batch);
    }

    /**
     * Inspired by Jetty's 9 {@link Generator#generateHeaderBytes(Frame, ByteBuffer)}
     *
     * <pre>
     *    0                   1                   2                   3
     *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *   +-+-+-+-+-------+-+-------------+-------------------------------+
     *   |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
     *   |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
     *   |N|V|V|V|       |S|             |   (if payload len==126/127)   |
     *   | |1|2|3|       |K|             |                               |
     *   +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
     *   |     Extended payload length continued, if payload len == 127  |
     *   + - - - - - - - - - - - - - - - +-------------------------------+
     *   |                               |Masking-key, if MASK set to 1  |
     *   +-------------------------------+-------------------------------+
     *   | Masking-key (continued)       |          Payload Data         |
     *   +-------------------------------- - - - - - - - - - - - - - - - +
     *   :                     Payload Data continued ...                :
     *   + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
     *   |                     Payload Data continued ...                |
     *   +---------------------------------------------------------------+
     * </pre>
     */
    private static int getFrameHeaderLength(final Frame frame) {
        final int payloadLength = frame.getPayloadLength();
        int length = 1;

        // if length is over 65535 then its a 7 + 64 bit length
        if (payloadLength > 0xFF_FF) length += 9;
        // if payload is greater that 126 we have a 7 + 16 bit length
        else if (payloadLength >= 0x7E) length += 3;
        // we have a 7 bit length
        else length += 1;

        if (frame.isMasked()) length++;

        return length;
    }

    void setWebSocketListener(final WebSocket.Listener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }
}
