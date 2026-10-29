/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;
import static com.ponysdk.sample.client.Costs.*;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.sample.client.page.addon.FlexLayoutDemoEntryPoint;

/** FlexLayout: dockable panels, split views, themes. */
final class FlexLayoutPage {

    private FlexLayoutPage() {
    }

    static PFlowPanel build() {
        final PFlowPanel p = Element.newPFlowPanel();
        pageTitle(p, "FlexLayout",
            "Dockable panel layout add-on — drag tabs, split views, collapsible sidebars, themes. "
                + "Its creation arguments travel as typed binary values, no JSON. Its Java: "
                + "sample/src/main/java/com/ponysdk/sample/client/page/addon/FlexLayoutDemoEntryPoint.java.");
        // The FlexLayout panel positions its content absolutely, so it needs a parent with a
        // definite height; the showcase content area scrolls, so we give it a fixed-height box.
        // region flexlayout
        final PFlowPanel box = Element.newPFlowPanel();
        box.setStyleProperty("height", "78vh");
        box.setStyleProperty("min-height", "480px");
        box.setStyleProperty("margin-top", "16px");
        box.setStyleProperty("border", "1px solid var(--border)");
        box.setStyleProperty("border-radius", "10px");
        box.setStyleProperty("overflow", "hidden");
        box.add(FlexLayoutDemoEntryPoint.buildDemo(false)); // the layout, its tabs and its panels
        // endregion
        p.add(box);
        p.add(codeToggle("this page", Source.code(FlexLayoutPage.class, "flexlayout")));
        return p;
    }
}
