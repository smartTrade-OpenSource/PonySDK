/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.terminal.ui;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.tools.URL;

import elemental.client.Browser;
import elemental.html.WebSocket;

public class PTWebSocket extends AbstractPTObject {

    private WebSocket webSocket;

    @Override
    public void create(final ReaderBuffer buffer, final int id, final UIBuilder uiBuilder) {
        super.create(buffer, id, uiBuilder);

        final Map<String, String> parameters = new HashMap<>();
        parameters.put(ClientToServerModel.OBJECT_ID.toStringValue(), String.valueOf(objectID));
        parameters.put(ClientToServerModel.UI_CONTEXT_ID.toStringValue(), String.valueOf(PonySDK.get().getContextId()));

        webSocket = Browser.getWindow().newWebSocket(URL.getWebsocketURL(parameters));
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        super.add(buffer, ptObject);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        return super.update(buffer, binaryModel);
    }

}
