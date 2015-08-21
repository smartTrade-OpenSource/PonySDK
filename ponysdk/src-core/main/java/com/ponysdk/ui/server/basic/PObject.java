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

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Create;
import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.core.instruction.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.server.extension.PAddOn;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject {

    protected final long ID = UIContext.get().nextID();

    private String nativeBindingFunction;

    private ListenerCollection<PNativeHandler> nativeHandlers;

    private long parentWindowID;

    PObject(final EntryInstruction... entries) {
        UIContext.get().registerObject(this);
        final Create create = createInstruction(entries);
        enrichCreate(create);
        Txn.get().getTxnContext().save(create);
    }

    protected void enrichCreate(final Create create) {}

    protected Create createInstruction(final EntryInstruction... entries) {
        final Create create = new Create(ID, getWidgetType());

        if (this instanceof PAddOn) {
            create.setAddOnSignature(((com.ponysdk.ui.server.extension.PAddOn) this).getSignature());
        }

        if (entries != null) {
            for (final EntryInstruction entry : entries) {
                if (entry.value == null) continue;
                create.put(entry.key, entry.value);
            }
        }
        return create;
    }

    protected abstract WidgetType getWidgetType();

    public final long getID() {
        return ID;
    }

    public void bindTerminalFunction(final String functionName) {

        if (nativeBindingFunction != null) throw new IllegalAccessError("Object already bind to native function: " + nativeBindingFunction);

        nativeBindingFunction = functionName;

        saveUpdate(Model.BIND, functionName);
    }

    public void sendToNative(final JSONObject data) {

        if (nativeBindingFunction == null) throw new IllegalAccessError("Object not bind to a native function");

        saveUpdate(Model.NATIVE, data);
    }

    public void addNativeHandler(final PNativeHandler handler) {
        if (nativeHandlers == null) nativeHandlers = new ListenerCollection<>();

        nativeHandlers.register(handler);
    }

    public void onClientData(final JsonObject event) {
        if (event.has(Dictionnary.PROPERTY.NATIVE)) {
            final JsonObject jsonObject = event.getJSONObject(Dictionnary.PROPERTY.NATIVE);
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
        result = prime * result + (int) (ID ^ (ID >>> 32));
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

    public long getParentWindowID() {
        return parentWindowID;
    }

    void setParentWindowID(final long parentWindowID) {
        this.parentWindowID = parentWindowID;
    }

    protected void saveAddHandler(final Model type) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD_HANDLER);
        parser.parse(type);
        parser.parse(Model.OBJECT_ID, ID);
        parser.endObject();
    }

    protected void saveRemoveHandler(final Model type) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE_HANDLER);
        parser.parse(Model.OBJECT_ID, ID);
        parser.endObject();
    }

    protected void saveRemoveHandler(final Model type, final Model model, final String value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE_HANDLER);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveRemove(final long objectID, final long parentObjectID) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_REMOVE);
        parser.parse(Model.OBJECT_ID, objectID);
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();
    }

    protected void saveAdd(final long objectID, final long parentObjectID, final Model model, final boolean value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.parse(Model.OBJECT_ID, objectID);
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.parse(model, value);
        parser.endObject();

        UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final long objectID, final long parentObjectID) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.parse(Model.OBJECT_ID, objectID);
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.endObject();

        UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final long objectID, final long parentObjectID, final Model model, final int value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.parse(model, value);
        parser.endObject();

        UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveAdd(final long objectID, final long parentObjectID, final Model model, final String value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(Model.PARENT_OBJECT_ID, parentObjectID);
        parser.parse(model, value);
        parser.endObject();

        UIContext.get().assignParentID(objectID, parentObjectID);
    }

    protected void saveUpdate(final Model model, final JsonObject json) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(model, json);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final boolean value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final int value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final long value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model, final String value) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(model, value);
        parser.endObject();
    }

    protected void saveUpdate(final Model model) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
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
