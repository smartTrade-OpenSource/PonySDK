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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTFileUpload extends PTWidget<FormPanel> {

    private static Frame frame;

    private FormPanel wrappedFormPanel;

    final FileUpload fileUpload = new FileUpload();

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        wrappedFormPanel = new FormPanel();
        init(create, uiService, wrappedFormPanel);

        wrappedFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
        wrappedFormPanel.setMethod(FormPanel.METHOD_POST);
        final VerticalPanel panel = new VerticalPanel();
        panel.setSize("100%", "100%");
        wrappedFormPanel.setWidget(panel);
        panel.add(fileUpload);

        wrappedFormPanel.addSubmitCompleteHandler(new SubmitCompleteHandler() {

            @Override
            public void onSubmitComplete(final SubmitCompleteEvent event) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(create.getObjectID());
                // instruction.put(Model.TYPE_EVENT);
                instruction.put(Model.HANDLER_SUBMIT_COMPLETE_HANDLER);
                uiService.sendDataToServer(wrappedFormPanel, instruction);
            }
        });
    }

    @Override
    public void addHandler(final PTInstruction instruction, final UIService uiService) {
        if (instruction.containsKey(Model.HANDLER_CHANGE_HANDLER)) {
            fileUpload.addChangeHandler(new ChangeHandler() {

                @Override
                public void onChange(final ChangeEvent event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instruction.getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_CHANGE_HANDLER);
                    eventInstruction.put(Model.FILE_NAME, fileUpload.getFilename());
                    uiService.sendDataToServer(fileUpload, eventInstruction);
                }
            });
        } else if (instruction.containsKey(Model.HANDLER_STREAM_REQUEST_HANDLER)) {
            final String action = GWT.getHostPageBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&" + Model.STREAM_REQUEST_ID + "=" + instruction.getLong(Model.STREAM_REQUEST_ID);
            getFrame().setUrl(action);
        } else if (instruction.containsKey(Model.HANDLER_EMBEDED_STREAM_REQUEST_HANDLER)) {
            final String action = GWT.getHostPageBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&" + Model.STREAM_REQUEST_ID + "=" + instruction.getLong(Model.STREAM_REQUEST_ID);
            wrappedFormPanel.setAction(action);
            wrappedFormPanel.submit();
        } else {
            super.addHandler(instruction, uiService);
        }
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.NAME)) {
            fileUpload.setName(update.getString(Model.NAME));
        } else if (update.containsKey(Model.ENABLED)) {
            fileUpload.setEnabled(update.getBoolean(Model.ENABLED));
        } else {
            super.update(update, uiService);
        }
    }

    private static Frame getFrame() {
        /* Frame for stream resource handling */
        if (frame == null) {
            frame = new Frame();
            frame.setWidth("0px");
            frame.setHeight("0px");
            frame.getElement().getStyle().setProperty("visibility", "hidden");
            frame.getElement().getStyle().setProperty("position", "fixed");
        }
        return frame;

    }

}
