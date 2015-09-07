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

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

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

    private String id;

    private PRootPanel() {}

    private PRootPanel(final String id) {
        this.id = id;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        if (id != null) {
            parser.comma();
            parser.parse(Model.ID, id);
        }
    }

    public static PRootPanel get(final PWindow window) {
        return get(window.getID(), ROOTID);
    }

    public static PRootPanel get() {
        return get(PWindow.MAIN, ROOTID);
    }

    public static PRootPanel get(final String id) {
        return get(PWindow.MAIN, id);
    }

    public static PRootPanel get(final PWindow window, final String id) {
        return get(window.getID(), id);
    }

    public static PRootPanel get(final long windowID, final String id) {
        final Map<String, PRootPanel> childs = ensureChilds(windowID);
        PRootPanel defaultRoot = childs.get(id);
        if (defaultRoot == null) {
            if (id.equals(PWindow.MAIN)) defaultRoot = new PRootPanel();
            else defaultRoot = new PRootPanel(id);
            childs.put(id, defaultRoot);
        }
        return defaultRoot;
    }

    private static Map<String, PRootPanel> ensureChilds(final Long windowID) {
        final String rootID = ROOTID + "_" + windowID;
        final UIContext session = UIContext.get();
        Map<String, PRootPanel> rootByIDs = session.getAttribute(rootID);
        if (rootByIDs == null) {
            rootByIDs = new HashMap<>();
            session.setAttribute(rootID, rootByIDs);
        }
        return rootByIDs;
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ROOT_PANEL;
    }

    /**
     * Clears the rootPanel. If clearDom is true, then also remove any DOM elements that are not widgets.
     * <p>
     * By default {@link #clear()} will only remove children that are widgets. This method also provides the
     * option to remove all children including the non-widget DOM elements that are directly added.
     *
     * @param clearDom
     *            if {@code true} this method will also remove any DOM elements that are not widgets.
     */
    public void clear(final boolean clearDom) {
        clear();

        if (clearDom) {
            saveUpdate(Model.CLEAR_DOM, clearDom);
        }
    }
}
