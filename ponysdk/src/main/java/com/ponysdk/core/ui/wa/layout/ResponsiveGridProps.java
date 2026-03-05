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

import java.util.Map;

/**
 * Props for {@link PResponsiveGrid}.
 *
 * @param columns     number of columns (default 12)
 * @param gap         CSS gap between cells (e.g. "1rem")
 * @param breakpoints map of breakpoint name to its configuration
 * @param hideOnMobile  hide this grid on mobile breakpoint
 * @param hideOnTablet  hide this grid on tablet breakpoint
 * @param hideOnDesktop hide this grid on desktop breakpoint
 */
public record ResponsiveGridProps(
    int columns,
    String gap,
    Map<String, BreakpointConfig> breakpoints,
    boolean hideOnMobile,
    boolean hideOnTablet,
    boolean hideOnDesktop
) {
    public static ResponsiveGridProps defaults() {
        return new ResponsiveGridProps(12, "1rem", Map.of(
            "mobile", new BreakpointConfig(0, 1, "0.5rem"),
            "tablet", new BreakpointConfig(600, 6, "0.75rem"),
            "desktop", new BreakpointConfig(1024, 12, "1rem")
        ), false, false, false);
    }
}
