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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PTSuggestBox extends PTWidget<SuggestBox> {

    public static Map<Integer, SuggestOracle> oracleByID = new HashMap<>();
    private PTTextBox ptTextBox;
    private SuggestOracle oracle;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        // ServerToClientModel.ORACLE
        final int oracleID = buffer.getBinaryModel().getIntValue();
        // ServerToClientModel.TEXTBOX_ID
        ptTextBox = (PTTextBox) uiService.getPTObject(buffer.getBinaryModel().getIntValue());

        oracle = oracleByID.get(oracleID);
        if (oracle == null)
            throw new RuntimeException("Oracle #" + oracleID + " not registered");

        super.create(buffer, objectId, uiService);
    }

    @Override
    protected SuggestBox createUIObject() {
        return new SuggestBox(oracle, ptTextBox.cast());
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.LIMIT.equals(binaryModel.getModel())) {
            uiObject.setLimit(binaryModel.getIntValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_STRING_VALUE_CHANGE_HANDLER.equals(handlerModel)) {
            uiObject.addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.setObjectID(getObjectID());
                    eventInstruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE_HANDLER);
                    eventInstruction.put(ClientToServerModel.TEXT, event.getValue());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else if (HandlerModel.HANDLER_STRING_SELECTION_HANDLER.equals(handlerModel)) {
            uiObject.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {

                @Override
                public void onSelection(final SelectionEvent<Suggestion> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(ClientToServerModel.HANDLER_STRING_SELECTION_HANDLER);
                    eventInstruction.put(ClientToServerModel.DISPLAY_STRING, event.getSelectedItem().getDisplayString());
                    eventInstruction.put(ClientToServerModel.REPLACEMENT_STRING, event.getSelectedItem().getReplacementString());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    public static class PTMultiWordSuggestOracle extends AbstractPTObject {

        private MultiWordSuggestOracle oracle;

        @Override
        public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
            super.create(buffer, objectId, uiService);

            this.oracle = new MultiWordSuggestOracle();

            PTSuggestBox.oracleByID.put(objectID, oracle);
        }

        @Override
        public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
            if (ServerToClientModel.SUGGESTION.equals(binaryModel.getModel())) {
                oracle.add(binaryModel.getStringValue());
                return true;
            }
            /*
             * FIXME
             * if (Model.SUGGESTIONS.equals(binaryModel.getModel())) {
             * final JSONArray jsonArray = binaryModel.get().isArray();
             * for (int i = 0; i < jsonArray.size(); i++) {
             * oracle.add(jsonArray.get(i).isString().stringValue());
             * }
             * return true;
             * }
             */
            /*
             * FIXME
             * if (Model.DEFAULT_SUGGESTIONS.equals(binaryModel.getModel())) {
             * final List<String> defaultSuggestions = new ArrayList<>();
             * final JSONArray jsonArray = binaryModel.get().isArray();
             * for (int i = 0; i < jsonArray.size(); i++) {
             * defaultSuggestions.add(jsonArray.get(i).isString().stringValue());
             * }
             * oracle.setDefaultSuggestionsFromText(defaultSuggestions);
             * return true;
             * }
             */
            if (ServerToClientModel.CLEAR.equals(binaryModel.getModel())) {
                oracle.clear();
                return true;
            }
            return super.update(buffer, binaryModel);
        }

    }
}
