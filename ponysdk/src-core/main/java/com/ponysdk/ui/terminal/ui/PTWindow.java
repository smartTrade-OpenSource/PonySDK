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
import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONParser;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.Window;

public class PTWindow extends AbstractPTObject implements EventListener {

    private final static Logger log = Logger.getLogger(PTWindow.class.getName());

    private Window window;
    private String url;
    private String name;
    private String features;

    private UIService uiService;
    private int id;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        this.id = create.getObjectID();
        this.uiService = uiService;

        if (create.containsKey(Model.URL)) url = create.getString(Model.URL);
        else url = GWT.getHostPageBaseURL() + "?wid=" + create.getObjectID();

        if (create.containsKey(Model.NAME)) name = create.getString(Model.NAME);
        else name = "";

        if (create.containsKey(Model.FEATURES)) features = create.getString(Model.FEATURES);
        else features = "";
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.OPEN)) {
            window = Browser.getWindow().open(url, name, features);
            window.addEventListener("beforeunload", this, true);
            window.addEventListener("message", this, true);
        } else if (update.containsKey(Model.TEXT)) {
            window.postMessage(update.getString(Model.TEXT), "*");
        } else if (update.containsKey(Model.CLOSE)) {
            window.close();
        }
    }

    private native void onDataReceived(Element win, final String text) /*-{win.onDataReceived(text);}-*/;

    @Override
    public void handleEvent(final Event event) {
        com.google.gwt.user.client.Window.alert(event.getType());
        if (event.getSrcElement() == window) {

            if (event.getType().equals("onbeforeunload")) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(id);
                instruction.put(Model.HANDLER_CLOSE_HANDLER);
                uiService.sendDataToServer(instruction);
            } else if (event.getType().equals("message")) {
                final MessageEvent messageEvent = (MessageEvent) event;
                uiService.update(JSONParser.parseStrict((String) messageEvent.getData()).isObject());
            } else if (event.getType().equals("onload")) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(id);
                instruction.put(Model.HANDLER_OPEN_HANDLER);
                uiService.sendDataToServer(instruction);
            }
        }
    }
}
