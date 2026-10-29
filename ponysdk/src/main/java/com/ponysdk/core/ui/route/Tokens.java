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

package com.ponysdk.core.ui.route;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** The text of a history token: its segments, its query, and the percent-encoding of what they carry. */
final class Tokens {

    private Tokens() {
    }

    /** The segments of a path; empty segments ({@code a//b}, a leading or trailing slash) are dropped. */
    static String[] segments(final String path) {
        return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    static String normalize(final String path) {
        return String.join("/", segments(path));
    }

    /** A path segment's text: {@code %xx} decoded, {@code +} kept. A malformed escape is kept as written. */
    static String decodeSegment(final String segment) {
        return decode(segment.replace("+", "%2B"), segment);
    }

    /** A path segment's value, encoded. */
    static String encodeSegment(final String value) {
        return encode(value).replace("+", "%20");
    }

    static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** The parameters of a query ({@code a=1&b=2}, no leading {@code ?}); the first value of a name wins. */
    static Map<String, String> query(final String query) {
        final Map<String, String> parameters = new LinkedHashMap<>();
        for (final String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            final String[] nameAndValue = pair.split("=", 2);
            parameters.putIfAbsent(decode(nameAndValue[0], nameAndValue[0]),
                nameAndValue.length > 1 ? decode(nameAndValue[1], nameAndValue[1]) : "");
        }
        return parameters;
    }

    private static String decode(final String encoded, final String ifMalformed) {
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException e) {
            return ifMalformed;
        }
    }
}
