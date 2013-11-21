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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.UIContext;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A singleton implementation of {@link PLayoutPanel} that always attaches itself to the document body (i.e.
 * {@link PRootPanel#get()}).
 * <p>
 * NOTE: This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it
 * is run have an explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 */
public class PRootLayoutPanel extends PLayoutPanel {

    private static final String ROOT = "PRootLayoutPanel";

    private PRootLayoutPanel() {}

    public static PRootLayoutPanel get() {
        if (UIContext.getCurrentWindow() == null) return get(0);
        return get(UIContext.getCurrentWindow().getID());
    }

    private static PRootLayoutPanel get(final long windowID) {
        final String rootID = ROOT + "_" + windowID;
        final UIContext session = UIContext.get();
        PRootLayoutPanel root = session.getAttribute(rootID);
        if (root == null) {
            root = new PRootLayoutPanel();
            session.setAttribute(rootID, root);
        }
        return root;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ROOT_LAYOUT_PANEL;
    }

}
