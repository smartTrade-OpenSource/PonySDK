/*
 * Copyright (c) 2026 PonySDK
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class UpgradeRequestDataTest {

    @Test
    public void nullEverything_yieldsEmptyNeverNullCollections() {
        final UpgradeRequestData d = new UpgradeRequestData(null, null, null);
        assertTrue(d.getParameterMap().isEmpty());
        assertTrue(d.getHeaders().isEmpty());
        assertTrue(d.getCookies().isEmpty());
        assertNull(d.getHost());
        assertFalse(d.isSecure());
        assertNull(d.getRemoteSocketAddress());
        assertNull(d.getUserAgent());
        assertNull(d.getSession());
        assertNull(d.getHeader("anything"));
    }

    @Test
    public void headerLookupIsCaseInsensitive() {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", List.of("PonyProbe/1.0"));
        final UpgradeRequestData d = new UpgradeRequestData(null, headers, null, null, false, null, null, null);
        assertEquals("PonyProbe/1.0", d.getHeader("user-agent"));
        assertEquals("PonyProbe/1.0", d.getHeader("USER-AGENT"));
        assertEquals("PonyProbe/1.0", d.getHeader("User-Agent"));
    }

    @Test
    public void nullHeaderKeyIsSkipped_notNPE() {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put(null, List.of("ignored"));
        headers.put("X-Real", List.of("kept"));
        final UpgradeRequestData d = new UpgradeRequestData(null, headers, null, null, false, null, null, null);
        assertEquals("kept", d.getHeader("x-real"));
        assertEquals(1, d.getHeaders().size());
    }

    @Test
    public void nullValueListBecomesEmpty_notNPE() {
        final Map<String, List<String>> params = new HashMap<>();
        params.put("k", null);
        final UpgradeRequestData d = new UpgradeRequestData(params, null, null);
        assertTrue(d.getParameterMap().get("k").isEmpty());
    }

    @Test
    public void snapshotIsDetachedFromSourceMutation() {
        final Map<String, List<String>> params = new HashMap<>();
        final List<String> values = new ArrayList<>(List.of("v1"));
        params.put("p", values);
        final UpgradeRequestData d = new UpgradeRequestData(params, null, null);
        // mutate the source AFTER capture
        values.add("v2");
        params.put("p2", List.of("x"));
        assertEquals(1, d.getParameterMap().size());
        assertEquals(List.of("v1"), d.getParameterMap().get("p"));
    }

    @Test
    public void returnedCollectionsAreUnmodifiable() {
        final Map<String, List<String>> params = new HashMap<>();
        params.put("p", List.of("v1"));
        final UpgradeRequestData d = new UpgradeRequestData(params, null, null);
        try {
            d.getParameterMap().put("z", List.of("y"));
            fail("expected UnsupportedOperationException on map mutation");
        } catch (final UnsupportedOperationException expected) {
            // ok
        }
        try {
            d.getParameterMap().get("p").add("y");
            fail("expected UnsupportedOperationException on value-list mutation");
        } catch (final UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void scalarFieldsRoundTrip() {
        final List<HttpCookie> cookies = List.of(new HttpCookie("SESSION", "abc"));
        final InetSocketAddress addr = new InetSocketAddress("10.0.0.1", 443);
        final UpgradeRequestData d = new UpgradeRequestData(null, null, cookies, "example.com", true, addr,
                "UA", null);
        assertEquals("example.com", d.getHost());
        assertTrue(d.isSecure());
        assertEquals(addr, d.getRemoteSocketAddress());
        assertEquals("UA", d.getUserAgent());
        assertEquals(1, d.getCookies().size());
        assertEquals("SESSION", d.getCookies().get(0).getName());
    }
}
