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

package com.ponysdk.core.terminal.request;

import com.google.gwt.json.client.JSONValue;

import elemental.client.Browser;
import elemental.events.MessageEvent;
import elemental.html.Uint8Array;

public abstract class ParentRequestBuilder implements RequestBuilder {

    public ParentRequestBuilder(final String id, final RequestCallback callback) {
        // From Main terminal to the matching window terminal
        Browser.getWindow().setOnmessage(event -> callback.onDataReceived((Uint8Array) ((MessageEvent) event).getData()));

        setReady(id);
    }

    /**
     * To Main terminal
     */
    public abstract void setReady(String id);

    /**
     * To Main terminal
     */
    @Override
    public void send(final JSONValue requestData) {
        sendToParent(requestData.toString());
    }

    /**
     * To Main terminal
     */
    public abstract void sendToParent(String data);

}
