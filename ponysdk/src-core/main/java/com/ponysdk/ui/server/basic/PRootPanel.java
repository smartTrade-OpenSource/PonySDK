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

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;

public class PRootPanel extends PAbsolutePanel {

    private static final String ROOTID = "PRootPanel";
    private static final String DEFAULTID = "DEFAULT";

    private PRootPanel() {}

    private PRootPanel(final String id) {
        super();
        create.getMainProperty().setProperty(PropertyKey.ID, id);
    }

    public static PRootPanel get() {
        return get(DEFAULTID);
    }

    public static PRootPanel get(final String id) {
        final Map<String, PRootPanel> childs = ensureChilds();
        PRootPanel defaultRoot = childs.get(id);
        if (defaultRoot == null) {
            if (id.equals(DEFAULTID)) defaultRoot = new PRootPanel();
            else defaultRoot = new PRootPanel(id);
            childs.put(id, defaultRoot);
        }
        return defaultRoot;
    }

    private static Map<String, PRootPanel> ensureChilds() {
        final PonySession session = PonySession.getCurrent();
        Map<String, PRootPanel> rootByIDs = session.getAttribute(ROOTID);
        if (rootByIDs == null) {
            rootByIDs = new HashMap<String, PRootPanel>();
            session.setAttribute(ROOTID, rootByIDs);
        }
        return rootByIDs;
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.ROOT_PANEL;
    }

}
