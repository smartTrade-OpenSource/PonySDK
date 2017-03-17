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

package com.ponysdk.core.terminal.ui;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTSuggestBox extends PTWidget<SuggestBox> {

    private static final Map<Integer, SuggestOracle> oracleByID = new HashMap<>();
    private PTTextBox ptTextBox;
    private SuggestOracle oracle;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        // ServerToClientModel.ORACLE
        final int oracleID = buffer.readBinaryModel().getIntValue();
        // ServerToClientModel.TEXTBOX_ID
        ptTextBox = (PTTextBox) uiService.getPTObject(buffer.readBinaryModel().getIntValue());

        oracle = oracleByID.get(oracleID);
        if (oracle == null) throw new RuntimeException("Oracle #" + oracleID + " not registered");

        super.create(buffer, objectId, uiService);
    }

    @Override
    protected SuggestBox createUIObject() {
        return new SuggestBox(oracle, ptTextBox.cast());
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.LIMIT.ordinal() == modelOrdinal) {
            uiObject.setLimit(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_STRING_VALUE_CHANGE.equals(handlerModel)) {
            uiObject.addValueChangeHandler(event -> {
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, event.getValue());
                uiBuilder.sendDataToServer(uiObject, eventInstruction);
            });
        } else if (HandlerModel.HANDLER_STRING_SELECTION.equals(handlerModel)) {
            uiObject.addSelectionHandler(event -> {
                final PTInstruction eventInstruction = new PTInstruction(getObjectID());
                eventInstruction.put(ClientToServerModel.HANDLER_STRING_SELECTION, event.getSelectedItem().getDisplayString());
                eventInstruction.put(ClientToServerModel.REPLACEMENT_STRING, event.getSelectedItem().getReplacementString());
                uiBuilder.sendDataToServer(uiObject, eventInstruction);
            });
        } else {
            super.addHandler(buffer, handlerModel);
        }
    }

    @Override
    public void removeHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_STRING_VALUE_CHANGE.equals(handlerModel)) {
            // TODO Remove HANDLER_STRING_VALUE_CHANGE
        } else if (HandlerModel.HANDLER_STRING_SELECTION.equals(handlerModel)) {
            // TODO Remove HANDLER_STRING_SELECTION
        } else {
            super.removeHandler(buffer, handlerModel);
        }
    }

    public static final class PTMultiWordSuggestOracle extends AbstractPTObject {

        private MultiWordSuggestOracle oracle;

        @Override
        public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
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
             * FIXME else if (Model.SUGGESTIONS.equals(binaryModel.getModel())) {
             * final JSONArray jsonArray = binaryModel.get().isArray(); for (int
             * i = 0; i < jsonArray.size(); i++) {
             * oracle.add(jsonArray.get(i).isString().stringValue()); } return
             * true; }
             */
            /*
             * FIXME else if (Model.DEFAULT_SUGGESTIONS.equals(binaryModel.getModel())) {
             * final List<String> defaultSuggestions = new ArrayList<>(); final
             * JSONArray jsonArray = binaryModel.get().isArray(); for (int i =
             * 0; i < jsonArray.size(); i++) {
             * defaultSuggestions.add(jsonArray.get(i).isString().stringValue())
             * ; } oracle.setDefaultSuggestionsFromText(defaultSuggestions);
             * return true; }
             */
            else if (ServerToClientModel.CLEAR.equals(binaryModel.getModel())) {
                oracle.clear();
                return true;
            } else {
                return super.update(buffer, binaryModel);
            }
        }

    }
}
