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
import com.ponysdk.ui.terminal.model.Model;

public class PTSuggestBox extends PTWidget<SuggestBox> {

    public static Map<Long, SuggestOracle> oracleByID = new HashMap<>();

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final PTTextBox ptTextBox = (PTTextBox) uiService.getPTObject(create.getLong(Model.TEXTBOX_ID));
        final long oracleID = create.getLong(Model.ORACLE);
        final SuggestOracle oracle = oracleByID.get(oracleID);
        if (oracle == null) throw new RuntimeException("Oracle #" + oracleID + " not registered");

        init(create, uiService, new SuggestBox(oracle, ptTextBox.cast()));
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.LIMIT)) {
            uiObject.setLimit(update.getInt(Model.LIMIT));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void addHandler(final PTInstruction instrcution, final UIService uiService) {
        if (instrcution.containsKey(Model.HANDLER_STRING_VALUE_CHANGE_HANDLER)) {
            uiObject.addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.setObjectID(instrcution.getObjectID());
                    eventInstruction.put(Model.HANDLER_STRING_VALUE_CHANGE_HANDLER);
                    eventInstruction.put(Model.TEXT, event.getValue());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else if (instrcution.containsKey(Model.HANDLER_STRING_SELECTION_HANDLER)) {
            uiObject.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {

                @Override
                public void onSelection(final SelectionEvent<Suggestion> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(instrcution.getObjectID());
                    // eventInstruction.put(Model.TYPE_EVENT);
                    eventInstruction.put(Model.HANDLER_STRING_SELECTION_HANDLER);
                    eventInstruction.put(Model.DISPLAY_STRING, event.getSelectedItem().getDisplayString());
                    eventInstruction.put(Model.REPLACEMENT_STRING, event.getSelectedItem().getReplacementString());
                    uiService.sendDataToServer(uiObject, eventInstruction);
                }
            });
        } else {
            super.addHandler(instrcution, uiService);
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
            if (update.containsKey(Model.SUGGESTION)) {
                oracle.add(update.getString(Model.SUGGESTION));
            } else if (update.containsKey(Model.SUGGESTIONS)) {
                final JSONArray jsonArray = update.get(Model.SUGGESTIONS).isArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    oracle.add(jsonArray.get(i).isString().stringValue());
                }
            } else if (update.containsKey(Model.DEFAULT_SUGGESTIONS)) {
                final List<String> defaultSuggestions = new ArrayList<>();
                final JSONArray jsonArray = update.get(Model.DEFAULT_SUGGESTIONS).isArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    defaultSuggestions.add(jsonArray.get(i).isString().stringValue());
                }
                oracle.setDefaultSuggestionsFromText(defaultSuggestions);
            } else if (update.containsKey(Model.CLEAR)) {
                oracle.clear();
            } else {
                super.update(update, uiService);
            }
        }

    }
}
