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

package com.ponysdk.sample.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The code a card shows is the code that runs: a region of the page's own source, marked
 * {@code // region <name>} ... {@code // endregion}, read from the copy the build puts on the classpath (source/) and
 * highlighted. Each region is read once per JVM.
 */
final class Source {

    private static final Map<String, String> REGIONS = new ConcurrentHashMap<>();
    private static final Set<String> KEYWORDS = Set.of("final", "new", "for", "if", "else", "return", "var", "int",
        "long", "double", "boolean", "void", "true", "false", "null", "this", "static", "private", "public", "class",
        "try", "catch", "while", "switch", "case", "default", "instanceof");
    /** Comments, strings, numbers, words, method names: the tokens a line is cut into. */
    private static final Pattern TOKEN = Pattern.compile(
        "(//.*)|(\"(?:[^\"\\\\]|\\\\.)*\")|('(?:[^'\\\\]|\\\\.)')|(\\b\\d[\\d_]*(?:\\.\\d+)?[LDF]?\\b)|(\\b[A-Za-z_][A-Za-z0-9_]*\\b)(\\s*\\()?");

    private Source() {
    }

    /** The region's lines, highlighted, as the code panel's HTML. */
    static String code(final Class<?> page, final String region) {
        return REGIONS.computeIfAbsent(page.getSimpleName() + "#" + region, key -> highlight(lines(page, region)));
    }

    /** The region's lines, without its markers and its common indentation. */
    static List<String> lines(final Class<?> page, final String region) {
        final String file = "source/" + page.getSimpleName() + ".java";
        final String text;
        try (InputStream in = Source.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) throw new IllegalStateException(file + " is not on the classpath: the build copies it (sample/build.gradle)");
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final List<String> lines = new ArrayList<>();
        boolean inside = false;
        boolean found = false;
        for (final String line : text.split("\\R", -1)) {
            final String trimmed = line.trim();
            if (!inside && (trimmed.equals("// region " + region) || trimmed.startsWith("// region " + region + ":"))) {
                inside = found = true;
            } else if (inside && trimmed.startsWith("// endregion")) {
                inside = false;
            } else if (inside) {
                lines.add(line);
            }
        }
        if (!found) throw new IllegalStateException("No region '" + region + "' in " + file);
        int indent = Integer.MAX_VALUE;
        for (final String line : lines) if (!line.isBlank()) indent = Math.min(indent, line.length() - line.stripLeading().length());
        final List<String> out = new ArrayList<>(lines.size());
        for (final String line : lines) out.add(line.isBlank() ? "" : line.substring(indent));
        return out;
    }

    private static String highlight(final List<String> lines) {
        final StringBuilder html = new StringBuilder();
        for (final String line : lines) html.append("<span class='code-line'>").append(highlight(line)).append("</span>");
        return html.toString();
    }

    private static String highlight(final String line) {
        final StringBuilder out = new StringBuilder();
        final Matcher m = TOKEN.matcher(line);
        int at = 0;
        while (m.find()) {
            out.append(escape(line.substring(at, m.start())));
            if (m.group(1) != null) span(out, "code-comment", m.group(1));
            else if (m.group(2) != null || m.group(3) != null) span(out, "code-str", m.group());
            else if (m.group(4) != null) span(out, "code-num", m.group(4));
            else {
                final String word = m.group(5);
                if (KEYWORDS.contains(word)) span(out, "code-kw", word);
                else if (m.group(6) != null) span(out, "code-method", word);
                else if (Character.isUpperCase(word.charAt(0))) span(out, "code-type", word);
                else out.append(word);
                if (m.group(6) != null) out.append(escape(m.group(6)));
            }
            at = m.end();
        }
        return out.append(escape(line.substring(at))).toString();
    }

    private static void span(final StringBuilder out, final String style, final String text) {
        out.append("<span class='").append(style).append("'>").append(escape(text)).append("</span>");
    }

    private static String escape(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
