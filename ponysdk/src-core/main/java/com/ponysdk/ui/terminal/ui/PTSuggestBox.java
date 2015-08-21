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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTSuggestBox extends PTWidget<SuggestBox> {

    public static Map<Long, SuggestOracle> oracleByID = new HashMap<>();

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final PTTextBox ptTextBox = (PTTextBox) uiService.getPTObject(create.getLong(PROPERTY.TEXTBOX_ID));
        final long oracleID = create.getLong(PROPERTY.ORACLE);
        final SuggestOracle oracle = oracleByID.get(oracleID);
        if (oracle == null) throw new RuntimeException("Oracle #" + oracleID + " not registered");

        init(create, uiService, new SuggestBox(oracle, ptTextBox.cast()));
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.LIMIT)) {
            uiObject.setLimit(update.getInt(PROPERTY.LIMIT));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {

        final String handler = addHandler.getString(HANDLER.KEY);

        if (HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER.equals(handler)) {
            uiObject.addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER);
                    eventInstruction.put(PROPERTY.TEXT, event.getValue());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else if (HANDLER.KEY_.STRING_SELECTION_HANDLER.equals(handler)) {
            uiObject.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {

                @Override
                public void onSelection(final SelectionEvent<Suggestion> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    eventInstruction.put(HANDLER.KEY, HANDLER.KEY_.STRING_SELECTION_HANDLER);
                    eventInstruction.put(PROPERTY.DISPLAY_STRING, event.getSelectedItem().getDisplayString());
                    eventInstruction.put(PROPERTY.REPLACEMENT_STRING, event.getSelectedItem().getReplacementString());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    public static class PTMultiWordSuggestOracle extends AbstractPTObject {

        private MultiWordSuggestOracle oracle;

        @Override
        public void create(final PTInstruction create, final UIService uiService) {
            this.objectID = create.getObjectID();
            this.oracle = new MultiWordSuggestOracle();

            PTSuggestBox.oracleByID.put(objectID, oracle);
        }

        @Override
        public void update(final PTInstruction update, final UIService uiService) {
            if (update.containsKey(PROPERTY.SUGGESTION)) {
                oracle.add(update.getString(PROPERTY.SUGGESTION));
            } else if (update.containsKey(PROPERTY.SUGGESTIONS)) {
                final JSONArray jsonArray = update.get(PROPERTY.SUGGESTIONS).isArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    oracle.add(jsonArray.get(i).isString().stringValue());
                }
            } else if (update.containsKey(PROPERTY.DEFAULT_SUGGESTIONS)) {
                final List<String> defaultSuggestions = new ArrayList<>();
                final JSONArray jsonArray = update.get(PROPERTY.DEFAULT_SUGGESTIONS).isArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    defaultSuggestions.add(jsonArray.get(i).isString().stringValue());
                }
                oracle.setDefaultSuggestionsFromText(defaultSuggestions);
            } else if (update.containsKey(PROPERTY.CLEAR)) {
                oracle.clear();
            } else {
                super.update(update, uiService);
            }
        }

    }
}
