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

import javax.json.JsonObject;

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject {

    protected final int ID = UIContext.get().nextID();

    private String nativeBindingFunction;

    private ListenerCollection<PNativeHandler> nativeHandlers;

    protected PWindow window;

    private boolean initialized = false;

    PObject() {
        this(null);
    }

    PObject(final PWindow window) {
        UIContext.get().registerObject(this);
        this.window = window;
    }

    protected void init() {
        if (initialized)
            return;

        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_CREATE, ID);
        parser.parse(ServerToClientModel.WIDGET_TYPE, getWidgetType().getValue());
        enrichOnInit(parser);
        parser.endObject();

        initialized = true;
    }

    public PWindow getWindow() {
        return window;
    }

    void attach(final PWindow window) {
        if (this.window != null && this.window != window) {
            throw new IllegalAccessError("Widget already attached to an other window");
        }

        this.window = window;

        saveUpdate(ServerToClientModel.WINDOW_ID, window.getID());
    }

    protected void enrichOnInit(final Parser parser) {
    }

    protected abstract WidgetType getWidgetType();

    public final int getID() {
        return ID;
    }

    public void bindTerminalFunction(final String functionName) {

        if (nativeBindingFunction != null)
            throw new IllegalAccessError("Object already bind to native function: " + nativeBindingFunction);

        nativeBindingFunction = functionName;

        saveUpdate(ServerToClientModel.BIND, functionName);
    }

    public void sendToNative(final JsonObject data) {

        if (nativeBindingFunction == null)
            throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(ServerToClientModel.NATIVE, data);
    }

    public void addNativeHandler(final PNativeHandler handler) {
        if (nativeHandlers == null)
            nativeHandlers = new ListenerCollection<>();

        nativeHandlers.register(handler);
    }

    public void onClientData(final JsonObject event) {
        if (nativeHandlers != null && !nativeHandlers.isEmpty()) {
            final String nativeKey = ClientToServerModel.NATIVE.toStringValue();
            if (event.containsKey(nativeKey)) {
                final PNativeEvent nativeEvent = new PNativeEvent(this, event.getJsonObject(nativeKey));
                for (final PNativeHandler handler : nativeHandlers) {
                    handler.onNativeEvent(nativeEvent);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (ID ^ ID >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PObject other = (PObject) obj;
        if (ID != other.ID)
            return false;
        return true;
    }

    protected void saveAddHandler(final HandlerModel type) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_ADD_HANDLER, type.getValue());
        parser.parse(ServerToClientModel.OBJECT_ID, ID);
        parser.endObject();
    }

    protected void saveRemoveHandler(final HandlerModel type) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_REMOVE_HANDLER, ID);
        parser.endObject();
    }

    protected void saveRemoveHandler(final ServerToClientModel type, final ServerToClientModel model, final Object value) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_REMOVE_HANDLER, ID);
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveRemove(final int objectID, final int parentObjectID) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_REMOVE, objectID);
        parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final ServerToClientModel model) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_ADD, objectID);
        parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        parser.parse(model, null);
        parser.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_ADD, objectID);
        parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();
        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final ServerToClientModel model, final Object value) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_ADD, objectID);
        parser.parse(ServerToClientModel.PARENT_OBJECT_ID, parentObjectID);
        parser.parse(model, value);
        parser.endObject();

        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveUpdate(final ServerToClientModel model) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_UPDATE, ID);
        parser.parse(model, null);
        parser.endObject();
    }

    protected void saveUpdate(final ServerToClientModel model, final Object value) {
        final Parser parser = Txn.get().getParser();
        parser.beginObject();
        if (window != null) parser.parse(ServerToClientModel.WINDOW_ID, window.getID());
        parser.parse(ServerToClientModel.TYPE_UPDATE, ID);
        parser.parse(model, value);
        parser.endObject();
    }

    @Override
    public String toString() {
        return "ID=" + ID + ", widgetType=" + getWidgetType().name();
    }

}
