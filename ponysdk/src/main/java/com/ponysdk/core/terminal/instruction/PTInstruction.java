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

package com.ponysdk.core.terminal.instruction;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.ponysdk.core.model.ClientToServerModel;

public class PTInstruction extends JSONObject {

    public PTInstruction() {
    }

    public PTInstruction(final int objectID) {
        put(ClientToServerModel.OBJECT_ID, objectID);
    }

    public PTInstruction(final JavaScriptObject javaScriptObject) {
        super(javaScriptObject);
    }

    public int getObjectID() {
        return (int) get(ClientToServerModel.OBJECT_ID.toStringValue()).isNumber().doubleValue();
    }

    public void put(final ClientToServerModel key) {
        put(key.toStringValue(), new JSONNumber(0)); // Use 0 instead of "" or null for performance purpose
    }

    public void put(final ClientToServerModel key, final boolean value) {
        put(key.toStringValue(), JSONBoolean.getInstance(value));
    }

    public void put(final ClientToServerModel key, final int value) {
        put(key.toStringValue(), new JSONNumber(value));
    }

    public void put(final ClientToServerModel key, final double value) {
        put(key.toStringValue(), new JSONNumber(value));
    }

    public void put(final ClientToServerModel key, final String value) {
        put(key.toStringValue(), new JSONString(value));
    }

    public void put(final ClientToServerModel key, final JavaScriptObject value) {
        put(key.toStringValue(), new JSONObject(value));
    }

    public void put(final ClientToServerModel key, final JSONValue jsonValue) {
        put(key.toStringValue(), jsonValue);
    }

}
