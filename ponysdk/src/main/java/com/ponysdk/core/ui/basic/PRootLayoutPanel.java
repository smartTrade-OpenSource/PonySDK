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

package com.ponysdk.core.ui.basic;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;

public class PRootLayoutPanel extends PLayoutPanel {

    private static final String KEY = PRootLayoutPanel.class.getSimpleName();

    private PRootLayoutPanel(final PWindow window) {
        this.window = window;
    }

    private PRootLayoutPanel(final PWindow window, final String id) {
        this(window);
    }

    public static PRootLayoutPanel get(final PWindow window) {
        return get(window, null);
    }

    public static PRootLayoutPanel get(final PWindow window, final String id) {
        final Map<String, PRootLayoutPanel> childs = ensureChilds(window);
        PRootLayoutPanel defaultRoot = childs.get(id);
        if (defaultRoot == null) {
            defaultRoot = new PRootLayoutPanel(window, id);
            childs.put(id, defaultRoot);
        }
        return defaultRoot;
    }

    private static Map<String, PRootLayoutPanel> ensureChilds(final PWindow window) {
        final UIContext session = UIContext.get();

        final String key = KEY + window.getID();

        Map<String, PRootLayoutPanel> rootByIDs = session.getAttribute(key);
        if (rootByIDs == null) {
            rootByIDs = new HashMap<>();
            session.setAttribute(key, rootByIDs);
        }

        return rootByIDs;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ROOT_LAYOUT_PANEL;
    }

}
