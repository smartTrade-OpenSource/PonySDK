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

package com.ponysdk.core.ui.wa.layout;

/**
 * Responsive breakpoints for the Layout Manager.
 * <p>
 * Defines screen width thresholds that trigger layout changes:
 * <ul>
 *   <li>{@link #MOBILE} — 0 to 599px</li>
 *   <li>{@link #TABLET} — 600 to 1023px</li>
 *   <li>{@link #DESKTOP} — 1024px and above</li>
 * </ul>
 */
public enum Breakpoint {

    MOBILE(0, 599),
    TABLET(600, 1023),
    DESKTOP(1024, Integer.MAX_VALUE);

    private final int minWidth;
    private final int maxWidth;

    Breakpoint(final int minWidth, final int maxWidth) {
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    /**
     * Returns the breakpoint matching the given screen width.
     *
     * @param width screen width in pixels
     * @return the matching breakpoint
     */
    public static Breakpoint forWidth(final int width) {
        for (final Breakpoint bp : values()) {
            if (width >= bp.minWidth && width <= bp.maxWidth) {
                return bp;
            }
        }
        return DESKTOP;
    }
}
