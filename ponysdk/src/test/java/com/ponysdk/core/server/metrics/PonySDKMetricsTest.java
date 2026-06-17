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

package com.ponysdk.core.server.metrics;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Verifies {@link PonySDKMetrics#utf8Length(String)} matches the real UTF-8 wire size
 * (the bytes-received metric must count bytes, not characters).
 */
public class PonySDKMetricsTest {

    @Test
    public void utf8LengthMatchesActualEncoding() {
        assertEquals(0, PonySDKMetrics.utf8Length(null));
        assertEquals(0, PonySDKMetrics.utf8Length(""));
        check("ascii");
        check("caf\u00e9");                 // é = 2 bytes
        check("\u20ac");                    // € = 3 bytes
        check("\uD83D\uDE80");              // 🚀 = 4 bytes (surrogate pair)
        check("a\u00e9\u20ac\uD83D\uDE80z"); // mixed
        check("{\"k\":\"\u00e9\uD83D\uDE80\"}");
    }

    private static void check(final String s) {
        assertEquals("UTF-8 length mismatch for: " + s,
            s.getBytes(StandardCharsets.UTF_8).length, PonySDKMetrics.utf8Length(s));
    }
}
