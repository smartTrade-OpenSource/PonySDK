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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** A path template of a {@link Route}: literal segments and {@code :name} parameters. */
final class Template {

    private final String[] segments; // the text of a literal, the name of a parameter
    private final boolean[] parameter;
    /** One character a segment, 0 a literal and 1 a parameter: sorted, the most literal first. */
    private final String key;
    /** The literals and the places of the parameters: two templates with the same shape match the same paths. */
    private final String shape;

    private Template(final String[] segments, final boolean[] parameter) {
        this.segments = segments;
        this.parameter = parameter;
        final StringBuilder key = new StringBuilder();
        final StringBuilder shape = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            key.append(parameter[i] ? '1' : '0');
            shape.append('/').append(parameter[i] ? ":" : segments[i]);
        }
        this.key = key.toString();
        this.shape = shape.toString();
    }

    static Template parse(final String template) {
        if (template.indexOf('?') >= 0) throw new IllegalArgumentException("No query in a route: " + template);
        final String[] segments = Tokens.segments(template);
        final boolean[] parameter = new boolean[segments.length];
        final Set<String> names = new HashSet<>();
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].charAt(0) == ':') {
                parameter[i] = true;
                segments[i] = segments[i].substring(1);
                if (segments[i].isEmpty()) throw new IllegalArgumentException("A parameter without a name in " + template);
                if (!names.add(segments[i])) throw new IllegalArgumentException(segments[i] + " twice in " + template);
            }
        }
        return new Template(segments, parameter);
    }

    String key() {
        return key;
    }

    String shape() {
        return shape;
    }

    /** The values of the parameters when the path matches, null when it does not. */
    Map<String, String> match(final String[] path) {
        if (path.length != segments.length) return null;
        final Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < segments.length; i++) {
            final String text = Tokens.decodeSegment(path[i]);
            if (parameter[i]) values.put(segments[i], text);
            else if (!segments[i].equals(text)) return null;
        }
        return values;
    }

    /**
     * The path with the values in place of the parameters; the names it used are added to {@code used}.
     *
     * @throws IllegalArgumentException when a parameter has no value
     */
    String expand(final Map<String, ?> values, final Set<String> used) {
        final StringBuilder path = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) path.append('/');
            if (parameter[i]) {
                final Object value = values.get(segments[i]);
                if (value == null) throw new IllegalArgumentException("No value for the parameter " + segments[i]);
                used.add(segments[i]);
                path.append(Tokens.encodeSegment(value.toString()));
            } else {
                path.append(Tokens.encodeSegment(segments[i]));
            }
        }
        return path.toString();
    }
}
