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

package com.ponysdk.core.server.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BannerPrinter implements Appendable {

    private static final Logger log = LoggerFactory.getLogger(BannerPrinter.class);

    private static final char LINE_SEPARATOR = '=';
    private static final char COLUMN_SEPARATOR = '/';
    private static final char NEW_LINE = '\n';
    private static final char SPACE = ' ';

    private final StringBuilder builder = new StringBuilder();

    private final int columnCount;

    public BannerPrinter(final int columnCount) {
        this.columnCount = columnCount;
    }

    public void appendLineSeparator() {
        for (int i = 0; i < columnCount; i++) {
            append(LINE_SEPARATOR);
        }
        append(NEW_LINE);
    }

    public void appendNewLine(final int lineCount) {
        for (int i = 0; i < lineCount; i++) {
            appendNewLine();
        }
    }

    private void appendNewLine() {
        for (int i = 0; i < columnCount; i++) {
            if (i == 0) append(COLUMN_SEPARATOR);
            else if (i == columnCount - 1) append(COLUMN_SEPARATOR);
            else append(SPACE);
        }
        append(NEW_LINE);
    }

    public void appendNewEmptyLine() {
        append(NEW_LINE);
    }

    public void appendCenteredLine(final String text) {
        final int startIndex = Math.abs((int) ((columnCount - text.length()) * .5));

        append(COLUMN_SEPARATOR);

        for (int i = 1; i < columnCount - 1; i++) {
            if (i < startIndex) append(SPACE);
            else if (i > startIndex + text.length() - 1) append(SPACE);
            else append(text.charAt(i - startIndex));
        }

        append(COLUMN_SEPARATOR);
        append(NEW_LINE);
    }

    @Override
    public Appendable append(final CharSequence arg0) {
        return builder.append(arg0);
    }

    @Override
    public Appendable append(final char arg0) {
        return builder.append(arg0);
    }

    @Override
    public Appendable append(final CharSequence arg0, final int arg1, final int arg2) {
        return builder.append(arg0, arg1, arg2);
    }

    public void print() {
        log.info(toString());
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
