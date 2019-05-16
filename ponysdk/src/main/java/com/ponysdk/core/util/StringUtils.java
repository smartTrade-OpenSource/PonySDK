/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.util;

public class StringUtils {

    //Utilities class
    private StringUtils() {

    }

    public static boolean containsIgnoreCase(final String value, final String str) {
        if (str.isEmpty()) return true;
        final char first = str.charAt(0);
        final int max = value.length() - str.length();
        for (int i = 0; i <= max; i++) {
            // Look for first character.
            if (!equalsIgnoreCase(value.charAt(i), first)) {
                while (++i <= max && !equalsIgnoreCase(value.charAt(i), first))
                    ;
            }
            // Found first character, now look at the rest of value
            if (i <= max) {
                int j = i + 1;
                final int end = j + str.length() - 1;
                for (int k = 1; j < end && equalsIgnoreCase(value.charAt(j), str.charAt(k)); j++, k++)
                    ;
                if (j == end) {
                    // Found whole string.
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean equalsIgnoreCase(final char a, final char b) {
        if (a == b) return true;
        if (a >= 'A' && a <= 'Z') return b + 'A' - 'a' == a;
        if (a >= 'a' && a <= 'z') return b + 'a' - 'A' == a;
        return false;
    }
}
