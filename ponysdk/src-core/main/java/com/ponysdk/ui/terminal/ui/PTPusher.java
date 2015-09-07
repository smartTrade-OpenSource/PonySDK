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

import java.util.Date;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.event.CommunicationErrorEvent;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.socket.WebSocketCallback;
import com.ponysdk.ui.terminal.socket.WebSocketClient;

public class PTPusher extends AbstractPTObject implements CommunicationErrorEvent.Handler {

    private final static Logger log = Logger.getLogger(UIBuilder.class.getName());

    private WebSocketClient socketClient;
    private boolean hasCommunicationError = false;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        if (!WebSocketClient.isSupported()) {
            UIBuilder.getRootEventBus().addHandler(CommunicationErrorEvent.TYPE, this);

            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.setObjectID(create.getObjectID());
            eventInstruction.put(Model.TYPE_EVENT);
            eventInstruction.put(Model.ERROR_MSG, "WebSocket not supported");
            uiService.sendDataToServer(eventInstruction);

            int delay = 1000;
            if (create.containsKey(Model.FIXDELAY)) delay = create.getInt(Model.FIXDELAY);

            Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                @Override
                public boolean execute() {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(create.getObjectID());
                    eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.POLL, true);
                    uiService.sendDataToServer(eventInstruction);
                    return !hasCommunicationError;
                }
            }, delay);
        } else {
            super.create(create, uiService);

            final String wsServerURL = GWT.getHostPageBaseURL().replaceFirst("http", "ws") + "ws" + "?" + Model.APPLICATION_VIEW_ID + "=" + UIBuilder.sessionID;

            socketClient = new WebSocketClient(new WebSocketCallback() {

                @Override
                public void message(final String message) {
                    final JSONObject data = JSONParser.parseStrict(message).isObject();
                    if (data.containsKey(Model.APPLICATION_PING.getKey())) return;

                    uiService.update(data);
                }

                @Override
                public void disconnected() {
                    log.info("Disconnected from: " + wsServerURL);
                    uiService.onCommunicationError(new Exception("Websocket connection lost."));
                    uiService.unRegisterObject(getObjectID());
                }

                @Override
                public void connected() {
                    log.info("Connected to: " + wsServerURL);
                }
            });

            log.info("Connecting to: " + wsServerURL);
            socketClient.connect(wsServerURL);

            int ping = 1000;
            if (create.containsKey(Model.PINGDELAY)) ping = create.getInt(Model.PINGDELAY);

            if (ping > 0) {
                Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                    @Override
                    public boolean execute() {
                        final int timeStamp = (int) (new Date().getTime() * .001);
                        final JSONObject jso = new JSONObject();
                        jso.put(Model.APPLICATION_PING.getKey(), new JSONNumber(timeStamp));
                        jso.put(Model.APPLICATION_VIEW_ID.getKey(), new JSONNumber(UIBuilder.sessionID));
                        socketClient.send(jso.toString());
                        return !hasCommunicationError;
                    }
                }, ping);
            }
        }
    }

    @Override
    public void onCommunicationError(final CommunicationErrorEvent event) {
        hasCommunicationError = true;
    }
}
