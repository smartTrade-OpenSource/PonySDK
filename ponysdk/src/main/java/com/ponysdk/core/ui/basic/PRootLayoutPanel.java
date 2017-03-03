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

/**
 * A singleton implementation of {@link PLayoutPanel} that always attaches
 * itself to the document body (i.e. {@link PRootPanel#get(int)}).
 * <p>
 * NOTE: This widget will <em>only</em> work in standards mode, which requires
 * that the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 */
public class PRootLayoutPanel extends PLayoutPanel {

    private static final String KEY = PRootLayoutPanel.class.getSimpleName();

    private PRootLayoutPanel(final int windowID) {
        this.windowID = windowID;
    }

    private PRootLayoutPanel(final int windowID, final String id) {
        this(windowID);
        // TODO
    }

    public static PRootLayoutPanel get(final int windowID) {
        return get(windowID, null);
    }

    public static PRootLayoutPanel get(final int windowID, final String id) {
        final Map<String, PRootLayoutPanel> childs = ensureChilds(windowID);
        PRootLayoutPanel defaultRoot = childs.get(id);
        if (defaultRoot == null) {
            defaultRoot = new PRootLayoutPanel(windowID, id);
            childs.put(id, defaultRoot);
        }
        return defaultRoot;
    }

    private static Map<String, PRootLayoutPanel> ensureChilds(final int windowID) {
        final UIContext session = UIContext.get();

        final String key = KEY + windowID;

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
