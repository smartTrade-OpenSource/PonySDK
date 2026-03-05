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
 * Props for {@link PStack}.
 *
 * @param orientation   "vertical" or "horizontal"
 * @param gap           CSS gap between items (e.g. "1rem")
 * @param alignment     cross-axis alignment: "start", "center", "end", "stretch"
 * @param justification main-axis justification: "start", "center", "end", "space-between", "space-around"
 * @param wrap          whether items wrap to the next line
 * @param hideOnMobile  hide on mobile breakpoint
 * @param hideOnTablet  hide on tablet breakpoint
 * @param hideOnDesktop hide on desktop breakpoint
 */
public record StackProps(
    String orientation,
    String gap,
    String alignment,
    String justification,
    boolean wrap,
    boolean hideOnMobile,
    boolean hideOnTablet,
    boolean hideOnDesktop
) {
    public static StackProps defaults() {
        return new StackProps("vertical", "1rem", "stretch", "start", false, false, false, false);
    }
}
