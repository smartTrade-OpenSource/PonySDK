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

import com.ponysdk.core.PonySession;
import com.ponysdk.core.instruction.Create;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * The superclass for all PonySDK objects.
 */
public abstract class PObject {

    protected long ID;

    protected Create create;

    PObject() {
        init(getWidgetType());
    }

    protected abstract WidgetType getWidgetType();

    protected void init(final WidgetType widgetType) {
        if (widgetType == null) { return; }
        ID = PonySession.getCurrent().nextID();
        create = new Create(ID, widgetType);
        if (this instanceof PAddOn) {
            create.setAddOnSignature(((PAddOn) this).getSignature());
        }
        PonySession.getCurrent().stackInstruction(create);
        registerObject();
    }

    protected void registerObject() {
        PonySession.getCurrent().registerObject(this);
    }

    public long getID() {
        return ID;
    }

    public void onEventInstruction(final JSONObject event) throws JSONException {
        // override
    }

    public PonySession getPonySession() {
        return PonySession.getCurrent();
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

    @Override
    public String toString() {
        return "PObject [ID=" + ID + ", widgetType=" + getWidgetType().name() + "]";
    }

}
