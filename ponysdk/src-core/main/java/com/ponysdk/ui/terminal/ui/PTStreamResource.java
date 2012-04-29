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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.RootPanel;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTStreamResource extends AbstractPTObject {

    private Frame frame;

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        final String action = GWT.getModuleBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&" + PROPERTY.STREAM_REQUEST_ID + "=" + addHandler.getLong(PROPERTY.STREAM_REQUEST_ID);
        frame = new Frame();
        frame.setWidth("0px");
        frame.setHeight("0px");
        frame.getElement().getStyle().setProperty("visibility", "hidden");
        frame.getElement().getStyle().setProperty("position", "fixed");
        RootPanel.get().add(frame);

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                frame.setUrl(action);
            }
        });
    }
}
