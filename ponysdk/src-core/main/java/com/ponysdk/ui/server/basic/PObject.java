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

import java.util.Collection;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.WidgetType;
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
        if (initialized) return;

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_CREATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.WIDGET_TYPE, getWidgetType().ordinal());

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

        if (nativeBindingFunction == null) throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(Model.NATIVE, data);
    }

    public void addNativeHandler(final PNativeHandler handler) {
        if (nativeHandlers == null) nativeHandlers = new ListenerCollection<>();

        nativeHandlers.register(handler);
    }

    public void onClientData(final JsonObject event) {
        if (event.containsKey(Model.NATIVE.getKey())) {
            final JsonObject jsonObject = event.getJsonObject(Model.NATIVE.getKey());
            if (nativeHandlers != null) {
                for (final PNativeHandler handler : nativeHandlers) {
                    handler.onNativeEvent(new PNativeEvent(this, jsonObject));
                }
            }
        }
    }

    public UIContext getUIContext() {
        return UIContext.get();
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
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PObject other = (PObject) obj;
        if (ID != other.ID) return false;
        return true;
    }

    protected void saveAddHandler(final Model type) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD_HANDLER);
        parser.comma();
        parser.parse(type);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.endObject();
    }

    protected void saveRemoveHandler(final Model type) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE_HANDLER);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.endObject();
    }

    protected void saveRemoveHandler(final Model type, final Model model, final String value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE_HANDLER);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveRemove(final int objectID, final int parentObjectID) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, objectID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final Model model, final boolean value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.comma();
        parser.parse(Model.OBJECT_ID, objectID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.comma();
        parser.parse(model, value);
        parser.endObject();

        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final int objectID, final int parentObjectID) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.comma();
        parser.parse(Model.OBJECT_ID, objectID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();

        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final Model model, final int value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.comma();
        parser.parse(Model.OBJECT_ID, objectID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.comma();
        parser.parse(model, value);
        parser.endObject();

        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final int objectID, final int parentObjectID, final Model model, final String value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.comma();
        parser.parse(Model.OBJECT_ID, objectID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.comma();
        parser.parse(model, value);
        parser.endObject();

        // UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveUpdate(final Model model, final JsonObjectBuilder builder) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, builder);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final JsonObject json) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, json);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final boolean value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final Collection<String> value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final int value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final long value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final String value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(model);
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
