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
 * Props for {@link PContainer}.
 *
 * @param maxWidth      maximum width CSS value (e.g. "1200px", "80rem")
 * @param padding       CSS padding value (e.g. "1rem", "16px 24px")
 * @param centered      if true, the container is horizontally centered (margin: 0 auto)
 * @param hideOnMobile  hide on mobile breakpoint
 * @param hideOnTablet  hide on tablet breakpoint
 * @param hideOnDesktop hide on desktop breakpoint
 */
public record ContainerProps(
    String maxWidth,
    String padding,
    boolean centered,
    boolean hideOnMobile,
    boolean hideOnTablet,
    boolean hideOnDesktop
) {
    public static ContainerProps defaults() {
        return new ContainerProps("1200px", "1rem", true, false, false, false);
    }
}
