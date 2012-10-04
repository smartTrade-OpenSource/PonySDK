/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.ui.terminal.ui;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.socket.WebSocketCallback;
import com.ponysdk.ui.terminal.socket.WebSocketClient;

public class PTPusher extends AbstractPTObject {

    private final static Logger log = Logger.getLogger(UIBuilder.class.getName());

    private WebSocketClient socketClient;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        if (!WebSocketClient.isSupported()) {
            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.setObjectID(create.getObjectID());
            eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            eventInstruction.put(PROPERTY.ERROR_MSG, "WebSocket not supported");
            uiService.triggerEvent(eventInstruction);

            final int delay = 1000;
            if (create.containsKey(PROPERTY.FIXDELAY)) create.getInt(PROPERTY.FIXDELAY);

            Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                @Override
                public boolean execute() {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(create.getObjectID());
                    eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    eventInstruction.put(PROPERTY.POLL, true);
                    uiService.triggerEvent(eventInstruction);
                    return true;
                }
            }, delay);

            return;
        }

        final String wsServerURL = GWT.getHostPageBaseURL().replaceFirst("http:", "ws:") + "ws" + "?" + APPLICATION.VIEW_ID + "=" + UIBuilder.sessionID;

        super.create(create, uiService);

        socketClient = new WebSocketClient(new WebSocketCallback() {

            @Override
            public void message(final String message) {
                final JSONObject data = JSONParser.parseLenient(message).isObject();
                uiService.update(data);
            }

            @Override
            public void disconnected() {
                log.info("Disconnected from: " + wsServerURL);
                uiService.unRegisterObject(getObjectID());
            }

            @Override
            public void connected() {
                log.info("Connected to: " + wsServerURL);
            }
        });

        log.info("Connecting to: " + wsServerURL);
        socketClient.connect(wsServerURL);
    }
}
