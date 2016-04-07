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
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTFileUpload extends PTWidget<FormPanel> {

    private static Frame frame;

    final FileUpload fileUpload = new FileUpload();

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        this.uiObject = new FormPanel();
        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);

        uiObject.setEncoding(FormPanel.ENCODING_MULTIPART);
        uiObject.setMethod(FormPanel.METHOD_POST);
        final VerticalPanel panel = new VerticalPanel();
        panel.setSize("100%", "100%");
        uiObject.setWidget(panel);
        panel.add(fileUpload);

        uiObject.addSubmitCompleteHandler(new SubmitCompleteHandler() {

            @Override
            public void onSubmitComplete(final SubmitCompleteEvent event) {
                final PTInstruction instruction = new PTInstruction();
                instruction.setObjectID(objectId);
                instruction.put(Model.HANDLER_SUBMIT_COMPLETE_HANDLER);
                uiService.sendDataToServer(uiObject, instruction);
            }
        });
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_CHANGE_HANDLER.equals(handlerModel)) {
            fileUpload.addChangeHandler(new ChangeHandler() {

                @Override
                public void onChange(final ChangeEvent event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(HandlerModel.HANDLER_CHANGE_HANDLER);
                    eventInstruction.put(Model.FILE_NAME, fileUpload.getFilename());
                    uiService.sendDataToServer(fileUpload, eventInstruction);
                }
            });
        } else if (HandlerModel.HANDLER_STREAM_REQUEST_HANDLER.equals(handlerModel)) {
            final String action = GWT.getHostPageBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&"
                    + Model.STREAM_REQUEST_ID + "=" + buffer.getBinaryModel().getIntValue();
            getFrame().setUrl(action);
        } else if (HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST_HANDLER.equals(handlerModel)) {
            final String action = GWT.getHostPageBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&"
                    + Model.STREAM_REQUEST_ID + "=" + buffer.getBinaryModel().getIntValue();
            uiObject.setAction(action);
            uiObject.submit();
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.NAME.equals(binaryModel.getModel())) {
            fileUpload.setName(binaryModel.getStringValue());
            return true;
        }
        if (Model.ENABLED.equals(binaryModel.getModel())) {
            fileUpload.setEnabled(binaryModel.getBooleanValue());
            return true;
        }
        return super.update(buffer, binaryModel);
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
