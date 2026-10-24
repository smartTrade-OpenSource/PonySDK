/*
 * Copyright (c) 2026 PonySDK
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The parser-free fast path for the terminal's latency message must accept exactly {"h":<integer>} and {"h":<integer>,"x":<integer>}. */
public class WebSocketControlMessageTest {

    private static final long NO = WebSocket.NOT_A_LATENCY_MESSAGE;

    @Test
    public void parsesCanonicalLatencyMessages() {
        assertEquals(0L, WebSocket.parseLatencyMessage("{\"h\":0}"));
        assertEquals(653306019432708L, WebSocket.parseLatencyMessage("{\"h\":653306019432708}"));
        assertEquals(-42L, WebSocket.parseLatencyMessage("{\"h\":-42}"));
        assertEquals(999_999_999_999_999_999L, WebSocket.parseLatencyMessage("{\"h\":999999999999999999}"));
    }

    /** ADR-0005: the time the probe waited in the terminal, 0 from an older terminal. */
    @Test
    public void parsesTheTerminalTime() {
        assertEquals(653306019432708L, WebSocket.parseLatencyMessage("{\"h\":653306019432708,\"x\":12}"));
        assertEquals(12L, WebSocket.parseTerminalTime("{\"h\":653306019432708,\"x\":12}"));
        assertEquals(0L, WebSocket.parseTerminalTime("{\"h\":653306019432708}"));
    }

    @Test
    public void anythingElseFallsBackToTheJsonParser() {
        for (final String m : new String[] { "{\"h\":}", "{\"h\":-}", "{\"h\": 12}", "{\"h\":12 }", "{\"h\":1.5}",
                "{\"h\":12,\"y\":1}", "{\"h\":12,\"x\":}", "{\"h\":12,\"x\":1.5}", "{\"h\":,\"x\":1}", "{\"v\":0}", "{\"h\":\"12\"}", "{\"h\":12", "\"h\":12}", "",
                "{\"h\":9999999999999999999}" /* would overflow */, "{\"hh\":1}" }) {
            assertEquals(NO, WebSocket.parseLatencyMessage(m), m);
        }
    }
}
