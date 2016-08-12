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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenEvent;
import com.ponysdk.core.ui.basic.event.POpenHandler;

public class PWindow extends PObject {

    static final int EMPTY_WINDOW_ID = -1;

    private final List<PCloseHandler> closeHandlers = new ArrayList<>();
    private final List<POpenHandler> openHandlers = new ArrayList<>();
    private final Queue<Runnable> stackedInstructions = new LinkedList<>();
    private String url;
    private String name;
    private String features;
    private boolean opened = false;

    PWindow() {
        initialized = true;
        PWindowManager.registerWindow(this);
    }

    public PWindow(final String url, final String name, final String features) {
        this.windowID = getMain().getID();
        this.url = url;
        this.name = name;
        this.features = features;

        init();
    }

    public static void initialize() {
        final UIContext uiContext = UIContext.get();
        PWindow mainWindow = uiContext.getAttribute(PWindow.class.getCanonicalName());
        if (mainWindow == null) {
            mainWindow = new PWindow() {

                @Override
                public boolean isOpened() {
                    return true;
                }

                @Override
                public void close() {
                }

            };
            uiContext.setAttribute(PWindow.class.getCanonicalName(), mainWindow);
        }
    }

    public static PWindow getMain() {
        return UIContext.get().getAttribute(PWindow.class.getCanonicalName());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WINDOW;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.URL, url);
        parser.parse(ServerToClientModel.NAME, name);
        parser.parse(ServerToClientModel.FEATURES, features);
    }

    public void open() {
        if (!opened) {
            PWindowManager.preregisterWindow(this);
            saveUpdate(writer -> writer.writeModel(ServerToClientModel.OPEN));
            Txn.get().flush();
        }
    }

    public void close() {
        if (opened) saveUpdate(writer -> writer.writeModel(ServerToClientModel.CLOSE));
    }

    @Override
    public void onClientData(final JsonObject event) {
        if (event.containsKey(ClientToServerModel.HANDLER_OPEN.toStringValue())) {
            PWindowManager.registerWindow(this);
            this.opened = true;

            while (!stackedInstructions.isEmpty()) {
                stackedInstructions.poll().run();
            }

            final POpenEvent e = new POpenEvent(this);
            openHandlers.forEach(handler -> handler.onOpen(e));
        } else if (event.containsKey(ClientToServerModel.HANDLER_CLOSE.toStringValue())) {
            PWindowManager.unregisterWindow(this);
            getPRootLayoutPanel().clear();
            getPRootPanel().clear();
            this.opened = false;
            final PCloseEvent e = new PCloseEvent(this);
            closeHandlers.forEach(handler -> handler.onClose(e));
        } else {
            super.onClientData(event);
        }
    }

    public void addCloseHandler(final PCloseHandler handler) {
        closeHandlers.add(handler);
    }

    public void addOpenHandler(final POpenHandler handler) {
        openHandlers.add(handler);
    }

    public void removeCloseHandler(final PCloseHandler handler) {
        closeHandlers.remove(handler);
    }

    public PRootLayoutPanel getPRootLayoutPanel() {
        return PRootLayoutPanel.get(getID());
    }

    public PRootPanel getPRootPanel() {
        return PRootPanel.get(getID());
    }

    public void add(final IsPWidget widget) {
        if (PWindowManager.getWindow(ID) == this) add0(widget);
        else stackedInstructions.add(() -> add0(widget));
    }

    private void add0(final IsPWidget widget) {
        getPRootPanel().add(widget);
    }

    public boolean isOpened() {
        return opened;
    }

    public static class TargetAttribut {

        static final String BLANK = "_blank";
        static final String PARENT = "_parent";
        static final String SELF = "_self";
        static final String TOP = "_top";
    }

}
