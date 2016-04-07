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
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;

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

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_CREATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.WIDGET_TYPE, getWidgetType().getValue());
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

        saveUpdate(Model.WINDOW_ID, window.getID());
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

        saveUpdate(Model.BIND, functionName);
    }

    public void sendToNative(final JsonObject data) {

        if (nativeBindingFunction == null)
            throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(Model.NATIVE, data);
    }

    public void addNativeHandler(final PNativeHandler handler) {
        if (nativeHandlers == null)
            nativeHandlers = new ListenerCollection<>();

        nativeHandlers.register(handler);
    }

    public void onClientData(final JsonObject event) {
        if (nativeHandlers != null && !nativeHandlers.isEmpty()) {
            final String nativeKey = Model.NATIVE.toStringValue();
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
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD_HANDLER, type.getValue());
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.endObject();
    }

    protected void saveRemoveHandler(final HandlerModel type) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE_HANDLER, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.endObject();
    }

    protected void saveRemoveHandler(final Model type, final Model model, final Object value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE_HANDLER, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveRemove(final int objectID, final int parentObjectID) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE, objectID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final Model model) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD, objectID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.parse(model);
        parser.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD, objectID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();
        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final Model model, final Object value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD, objectID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.parse(model, value);
        parser.endObject();

        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveUpdate(final Model model) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(model);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final Object value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(model, value);
        parser.endObject();
    }

    @Override
    public String toString() {
        return "[ID=" + ID + ", widgetType=" + getWidgetType().name() + "]";
    }

    public String toString(final String append) {
        return "[ID=" + ID + ", widgetType=" + getWidgetType().name() + ", " + append + "]";
    }

}
