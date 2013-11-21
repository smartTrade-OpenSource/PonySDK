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
import com.google.gwt.user.client.Element;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTWindow extends AbstractPTObject {

    private final static Logger log = Logger.getLogger(PTWindow.class.getName());

    private Element window = null;
    private String url;
    private String name;
    private String features;

    private UIService uiService;
    private Long id;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {

        this.id = create.getObjectID();
        this.uiService = uiService;

        if (create.containsKey(PROPERTY.URL)) url = create.getString(PROPERTY.URL);
        else url = GWT.getHostPageBaseURL() + "?wid=" + create.getObjectID();

        if (create.containsKey(PROPERTY.NAME)) name = create.getString(PROPERTY.NAME);
        else name = "";

        if (create.containsKey(PROPERTY.FEATURES)) features = create.getString(PROPERTY.FEATURES);
        else features = "";
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.OPEN)) {
            window = open(url, name, features);
            if (window != null) {
                checkWindowAlive();
            }
        } else if (update.containsKey(PROPERTY.TEXT)) {
            onDataReceived(window, update.getString(PROPERTY.TEXT));
        } else if (update.containsKey(PROPERTY.CLOSE)) {
            close(window);
        }
    }

    private void checkWindowAlive() {
        Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

            @Override
            public boolean execute() {
                if (isOpen(window)) return true;

                onClose();
                return false;
            }
        }, 2000);
    }

    public void onClose() {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(id);
        instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
        instruction.put(HANDLER.KEY, HANDLER.KEY_.CLOSE_HANDLER);
        uiService.sendDataToServer(instruction);
    }

    private native void onDataReceived(Element win, final String text) /*-{win.onDataReceived(text);}-*/;

    private native Element open(String url, String name, String features) /*-{
                                                                              var that = this;
                                                                              var w = $wnd.open(url, name, features);
                                                                              return w;
                                                                          }-*/;

    private native boolean isOpen(Element win) /*-{
                                                  return !! (win && !win.closed);
                                                  }-*/;

    private native void close(Element win) /*-{
                                              if (win) win.close();
                                              }-*/;

    private native void focus(Element win) /*-{
                                              if (win) win.focus();
                                              }-*/;
}
