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

package com.ponysdk.core.ui.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.json.JsonObject;

import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.*;

/**
 * A {@link PSuggestBox} is a text box or text area which displays a
 * pre-configured set of selections that match the user's input. Each
 * {@link PSuggestBox} is associated with a single {@link PSuggestOracle}. The
 * {@link PSuggestBox} is used to provide a set of selections given a specific
 * query string.
 * <p>
 * By default, the {@link PSuggestBox} uses a {@link PMultiWordSuggestOracle} as
 * its oracle. Below we show how a {@link PMultiWordSuggestOracle} can be
 * configured:
 * </p>
 *
 * <pre>
 * PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
 * oracle.add(&quot;Cat&quot;);
 * oracle.add(&quot;Dog&quot;);
 * oracle.add(&quot;Horse&quot;);
 * oracle.add(&quot;Canary&quot;);
 *
 * PSuggestBox box = new PSuggestBox(oracle);
 * </pre>
 *
 * Using the example above, if the user types "C" into the text widget, the
 * oracle will configure the suggestions with the "Cat" and "Canary"
 * suggestions. Specifically, whenever the user types a key into the text
 * widget, the value is submitted to the <code>PMultiWordSuggestOracle</code>.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-SuggestBox</dt>
 * <dd>the suggest box itself</dd>
 * </dl>
 * TODO(pdr): Add SafeHtml support to this and implementing classes.
 *
 * @see PSuggestOracle
 * @see PMultiWordSuggestOracle
 * @see PTextBoxBase
 */
public class PSuggestBox extends PWidget
        implements Focusable, HasPValueChangeHandlers<String>, PSelectionHandler<PSuggestOracle.PSuggestion>, HasPSelectionHandlers<PSuggestOracle.PSuggestion> {

    private List<PSelectionHandler<PSuggestOracle.PSuggestion>> selectionHandler;

    private final PSuggestOracle suggestOracle;
    private PTextBox textBox;

    private int limit;

    public PSuggestBox() {
        this(new PMultiWordSuggestOracle());
    }

    public PSuggestBox(final PSuggestOracle suggestOracle) {
        this.suggestOracle = suggestOracle;
    }

    @Override
    protected void init0() {
        super.init0();
        saveAddHandler(HandlerModel.HANDLER_STRING_VALUE_CHANGE);
        saveAddHandler(HandlerModel.HANDLER_STRING_SELECTION);
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.ORACLE, suggestOracle.getID());

        // TODO nciaravola

        // if (textBox == null) {
        // textBox = new PTextBox();
        // }
        //
        // parser.parse(Model.TEXTBOX_ID, textBox.getID());
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())) {
            final String text = instruction.getString(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue());
            textBox.fireOnValueChange(new PValueChangeEvent<>(this, text));
        } else if (instruction.containsKey(ClientToServerModel.HANDLER_STRING_SELECTION.toStringValue())) {
            String replacementString = instruction.getString(ClientToServerModel.REPLACEMENT_STRING.toStringValue());
            String displayString = instruction.getString(ClientToServerModel.HANDLER_STRING_SELECTION.toStringValue());
            this.textBox.setText(replacementString);
            final MultiWordSuggestion suggestion = new MultiWordSuggestion(replacementString, displayString);
            onSelection(new PSelectionEvent<PSuggestOracle.PSuggestion>(this, suggestion));
        } else {
            super.onClientData(instruction);
        }
    }

    @Override
    public void onSelection(final PSelectionEvent<PSuggestOracle.PSuggestion> event) {
        if (selectionHandler != null) {
            for (final PSelectionHandler<PSuggestOracle.PSuggestion> handler : selectionHandler) {
                handler.onSelection(event);
            }
        }
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SUGGESTBOX;
    }

    @Override
    public void setFocus(final boolean focused) {
        textBox.setFocus(focused);
    }

    public PTextBox getTextBox() {
        return textBox;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.LIMIT, limit);
        });
    }

    public void setText(final String text) {
        textBox.setText(text);
    }

    public int getLimit() {
        return limit;
    }

    public String getText() {
        return textBox.getText();
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        textBox.addValueChangeHandler(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        if (handler != null) {
            return false;
        } else {
            return textBox.removeValueChangeHandler(handler);
        }
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return textBox.getValueChangeHandlers();
    }

    public PSuggestOracle getSuggestOracle() {
        return suggestOracle;
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<PSuggestOracle.PSuggestion> handler) {
        if (selectionHandler == null) {
            selectionHandler = new ArrayList<>(0);
        }
        this.selectionHandler.add(handler);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<PSuggestOracle.PSuggestion> handler) {
        if (selectionHandler != null) {
            this.selectionHandler.remove(handler);
        }
    }

    @Override
    public Collection<PSelectionHandler<PSuggestOracle.PSuggestion>> getSelectionHandlers() {
        if (selectionHandler == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableCollection(selectionHandler);
        }
    }

    public static class MultiWordSuggestion implements PSuggestOracle.PSuggestion {

        private final String displayString;

        private final String replacementString;

        public MultiWordSuggestion(final String replacementString, final String displayString) {
            this.replacementString = replacementString;
            this.displayString = displayString;
        }

        @Override
        public String getDisplayString() {
            return displayString;
        }

        @Override
        public String getReplacementString() {
            return replacementString;
        }
    }

    public static class PMultiWordSuggestOracle extends PSuggestOracle {

        public PMultiWordSuggestOracle() {
        }

        @Override
        public void add(final String suggestion) {
            saveUpdate((writer) -> {
                writer.writeModel(ServerToClientModel.SUGGESTION, suggestion);
            });
        }

        @Override
        public void addAll(final Collection<String> collection) {
            //            saveUpdate((writer) -> {
            //                writer.writeModel(ServerToClientModel.SUGGESTIONS, collection);
            //            });
        }

        public void setDefaultSuggestions(final Collection<String> collection) {
            //            saveUpdate((writer) -> {
            //                writer.writeModel(ServerToClientModel.DEFAULT_SUGGESTIONS, collection);
            //            });
        }

        public void clear() {
            saveUpdate((writer) -> {
                writer.writeModel(ServerToClientModel.CLEAR);
            });
        }

        @Override
        protected WidgetType getWidgetType() {
            return WidgetType.MULTIWORD_SUGGEST_ORACLE;
        }
    }
}
