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

package com.ponysdk.core.server.websocket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

/**
 * Tests the anti-CSWSH Origin validation logic of {@link WebSocketServlet#isOriginAllowed}.
 */
public class WebSocketServletTest {

    @Test
    public void missingOriginIsAllowed_nonBrowserClient() {
        // Non-browser clients (e.g. the Pony driver, tests) send no Origin and carry no ambient cookies.
        assertTrue(WebSocketServlet.isOriginAllowed(null, "app.example.com", null));
        assertTrue(WebSocketServlet.isOriginAllowed("", "app.example.com", null));
    }

    @Test
    public void sameOriginIsAllowed() {
        assertTrue(WebSocketServlet.isOriginAllowed("https://app.example.com", "app.example.com", null));
        assertTrue(WebSocketServlet.isOriginAllowed("https://app.example.com:8443", "app.example.com:8443", null));
        assertTrue(WebSocketServlet.isOriginAllowed("http://localhost:8081", "localhost:8081", null));
    }

    @Test
    public void crossOriginIsRejected() {
        assertFalse(WebSocketServlet.isOriginAllowed("https://evil.example.com", "app.example.com", null));
        assertFalse("different port is a different origin",
            WebSocketServlet.isOriginAllowed("https://app.example.com:9999", "app.example.com:8443", null));
    }

    @Test
    public void allowListPermitsExtraOrigins() {
        final Set<String> allow = Set.of("https://trusted.partner.com");
        assertTrue(WebSocketServlet.isOriginAllowed("https://trusted.partner.com", "app.example.com", allow));
        // Still rejects an origin that is neither same-origin nor allow-listed
        assertFalse(WebSocketServlet.isOriginAllowed("https://evil.example.com", "app.example.com", allow));
    }

    @Test
    public void malformedOriginOrMissingHostIsRejected() {
        assertFalse("no scheme → cannot verify", WebSocketServlet.isOriginAllowed("app.example.com", "app.example.com", null));
        assertFalse("null host → cannot verify", WebSocketServlet.isOriginAllowed("https://app.example.com", null, null));
    }
}
