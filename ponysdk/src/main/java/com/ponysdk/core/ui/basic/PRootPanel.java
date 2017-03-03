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

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.WebsocketEncoder;
import com.ponysdk.core.ui.main.EntryPoint;

/**
 * The panel to which all other widgets must ultimately be added. RootPanels are never created
 * directly. Rather, they are accessed via {@link PRootPanel#get(int)} .
 * <p>
 * Most applications will add widgets to the default root panel in their
 * {@link EntryPoint#start(UIContext)} methods.
 * </p>
 */
public class PRootPanel extends PAbsolutePanel {

    private static final String KEY = PRootPanel.class.getSimpleName();

    private final String id;

    private PRootPanel(final int windowID, final String id) {
        this.windowID = windowID;
        this.id = id;
    }

    protected final static PRootPanel get(final int windowID) {
        return get(windowID, null);
    }

    public final static PRootPanel get(final int windowID, final String id) {
        final Map<String, PRootPanel> childs = ensureChilds(windowID);
        PRootPanel defaultRoot = childs.get(id);
        if (defaultRoot == null) {
            defaultRoot = new PRootPanel(windowID, id);
            childs.put(id, defaultRoot);
        }
        return defaultRoot;
    }

    @Override
    public void add(final PWidget child) {
        assertNotMe(child);

        if (child.getWindowID() == PWindow.EMPTY_WINDOW_ID || child.getWindowID() == windowID) {
            child.removeFromParent();
            children.add(child);
            adopt(child);

            child.saveAdd(child.getID(), ID);
            if (initialized) child.attach(windowID);
        } else {
            if (initialized) {
                throw new IllegalAccessError("Can't attach widget " + child + " to window #" + windowID
                        + " because it's already attached to window #" + child.getWindowID());
            } else {
                throw new IllegalAccessError("Can't only attach widget " + child + " to window #" + child.getWindowID()
                        + ". Need to attach the new parent to the same window before");
            }
        }
    }

    private static Map<String, PRootPanel> ensureChilds(final int windowID) {
        final UIContext session = UIContext.get();

        final String key = KEY + windowID;

        Map<String, PRootPanel> rootByIDs = session.getAttribute(key);
        if (rootByIDs == null) {
            rootByIDs = new HashMap<>();
            session.setAttribute(key, rootByIDs);
        }

        return rootByIDs;
    }

    @Override
    protected void enrichOnInit(final WebsocketEncoder parser) {
        super.enrichOnInit(parser);
        if (id != null) parser.encode(ServerToClientModel.ROOT_ID, id);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ROOT_PANEL;
    }

    /**
     * Clears the rootPanel. If clearDom is true, then also remove any DOM elements that are not
     * widgets.
     * <p>
     * By default {@link #clear()} will only remove children that are widgets.
     * This method also provides the option to remove all children including the non-widget DOM
     * elements that are directly added.
     *
     * @param clearDom
     *            if {@code true} this method will also remove any DOM elements
     *            that are not widgets.
     */
    public void clear(final boolean clearDom) {
        clear();
        if (clearDom) saveUpdate(writer -> writer.writeModel(ServerToClientModel.CLEAR_DOM, clearDom));
    }
}
