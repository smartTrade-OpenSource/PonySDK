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

import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * The panel to which all other widgets must ultimately be added. RootPanels are never created directly.
 * Rather, they are accessed via {@link PRootPanel#get()} .
 * <p>
 * Most applications will add widgets to the default root panel in their {@link EntryPoint#start(UIContext)}
 * methods.
 * </p>
 */
public class PRootPanel extends PAbsolutePanel {

    private static final String ROOTID = "PRootPanel";
    private static final String DEFAULTID = "DEFAULT";

    private PRootPanel() {}

    private PRootPanel(final String id) {
        super();
        create.put(PROPERTY.ID, id);
    }

    public static PRootPanel get() {
        if (UIContext.getCurrentWindow() == null) return get(0, DEFAULTID);
        return get(UIContext.getCurrentWindow().getID(), DEFAULTID);
    }

    public static PRootPanel get(final String id) {
        if (UIContext.getCurrentWindow() == null) return get(0, id);
        return get(UIContext.getCurrentWindow().getID(), id);
    }

    private static PRootPanel get(final long windowID, final String id) {
        final Map<String, PRootPanel> childs = ensureChilds(windowID);
        PRootPanel defaultRoot = childs.get(id);
        if (defaultRoot == null) {
            if (id.equals(DEFAULTID)) defaultRoot = new PRootPanel();
            else defaultRoot = new PRootPanel(id);
            childs.put(id, defaultRoot);
        }
        return defaultRoot;
    }

    private static Map<String, PRootPanel> ensureChilds(final long windowID) {
        final String rootID = ROOTID + "_" + windowID;
        final UIContext session = UIContext.get();
        Map<String, PRootPanel> rootByIDs = session.getAttribute(rootID);
        if (rootByIDs == null) {
            rootByIDs = new HashMap<String, PRootPanel>();
            session.setAttribute(ROOTID, rootByIDs);
        }
        return rootByIDs;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ROOT_PANEL;
    }

}
