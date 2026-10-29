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

import java.util.Map;

/**
 * What a route matched: the named parameters of its path and the query of the token ({@code orders/42?status=open}),
 * read as the type the view needs. A path parameter hides a query parameter of the same name.
 */
public final class RouteParameters {

    private final Router router;
    private final String token;
    private final Map<String, String> path;
    private final Map<String, String> query;

    RouteParameters(final Router router, final String token, final Map<String, String> path, final Map<String, String> query) {
        this.router = router;
        this.token = token;
        this.path = path;
        this.query = query;
    }

    /** The router that builds the view, for the view to navigate from. */
    public Router router() {
        return router;
    }

    /** The history token these parameters were read from. */
    public String token() {
        return token;
    }

    /** The value of the parameter, null when there is none. */
    public String get(final String name) {
        final String value = path.get(name);
        return value != null ? value : query.get(name);
    }

    public String get(final String name, final String fallback) {
        final String value = get(name);
        return value != null ? value : fallback;
    }

    /** @throws IllegalArgumentException when the parameter is absent or not an integer */
    public int getInt(final String name) {
        return Integer.parseInt(required(name));
    }

    /** The parameter as an integer, the fallback when it is absent or not an integer. */
    public int getInt(final String name, final int fallback) {
        try {
            return getInt(name);
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }

    /** @throws IllegalArgumentException when the parameter is absent or not an integer */
    public long getLong(final String name) {
        return Long.parseLong(required(name));
    }

    /** The parameter as a long, the fallback when it is absent or not an integer. */
    public long getLong(final String name, final long fallback) {
        try {
            return getLong(name);
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }

    /** {@code true} or {@code false} in any case; the fallback for anything else, absent included. */
    public boolean getBoolean(final String name, final boolean fallback) {
        final String value = get(name);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return fallback;
    }

    private String required(final String name) {
        final String value = get(name);
        if (value == null) throw new IllegalArgumentException("No parameter " + name + " in " + token);
        return value;
    }

    @Override
    public String toString() {
        return token;
    }
}
