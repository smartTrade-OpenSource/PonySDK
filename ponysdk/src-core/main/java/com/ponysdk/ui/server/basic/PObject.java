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

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Create;
import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.WidgetType;

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

    public long getID() {
        return ID;
    }

    public void bindTerminalFunction(final String functionName) {
        if (nativeBindingFunction != null) throw new IllegalAccessError("Object already bind to native function: " + nativeBindingFunction);

        final Update update = new Update(getID());
        update.put(Dictionnary.PROPERTY.BIND, functionName);
        Txn.get().getTxnContext().save(update);

        nativeBindingFunction = functionName;
    }

    public void sendToNative(final JSONObject data) {

        if (nativeBindingFunction == null) throw new IllegalAccessError("Object not bind to a native function");

        final Update update = new Update(getID());
        update.put(Dictionnary.PROPERTY.NATIVE, data);
        Txn.get().getTxnContext().save(update);
    }

    public void addNativeHandler(final PNativeHandler handler) {
        if (nativeHandlers == null) nativeHandlers = new ListenerCollection<PNativeHandler>();

        nativeHandlers.register(handler);
    }

    public void onClientData(final JSONObject event) throws JSONException {
        if (event.has(Dictionnary.PROPERTY.NATIVE)) {
            final JSONObject jsonObject = event.getJSONObject(Dictionnary.PROPERTY.NATIVE);
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

    @Override
    public String toString() {
        return "PObject [ID=" + ID + ", widgetType=" + getWidgetType().name() + "]";
    }

}
