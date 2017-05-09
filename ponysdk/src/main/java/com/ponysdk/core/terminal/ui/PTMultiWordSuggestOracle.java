/*
 * Copyright (c) 2017 PonySDK
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
package com.ponysdk.core.terminal.ui;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public final class PTMultiWordSuggestOracle extends AbstractPTObject {

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