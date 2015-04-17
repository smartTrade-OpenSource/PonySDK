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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Create;
import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.PSuggestOracle.PSuggestion;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A {@link PSuggestBox} is a text box or text area which displays a pre-configured set of selections that
 * match the user's input. Each {@link PSuggestBox} is associated with a single {@link PSuggestOracle}. The
 * {@link PSuggestBox} is used to provide a set of selections given a specific query string.
 * <p>
 * By default, the {@link PSuggestBox} uses a {@link PMultiWordSuggestOracle} as its oracle. Below we show how
 * a {@link PMultiWordSuggestOracle} can be configured:
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
 * Using the example above, if the user types "C" into the text widget, the oracle will configure the
 * suggestions with the "Cat" and "Canary" suggestions. Specifically, whenever the user types a key into the
 * text widget, the value is submitted to the <code>PMultiWordSuggestOracle</code>. <h3>CSS Style Rules</h3>
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
public class PSuggestBox extends PWidget implements Focusable, HasPValueChangeHandlers<String>, PSelectionHandler<PSuggestion>, HasPSelectionHandlers<PSuggestion> {

    private final List<PSelectionHandler<PSuggestion>> selectionHandler = new ArrayList<PSelectionHandler<PSuggestion>>();

    private final PSuggestOracle suggestOracle;
    private PTextBox textBox;

    private int limit;

    private String replacementString;
    private String displayString;

    public PSuggestBox() {
        this(new PMultiWordSuggestOracle());
    }

    public PSuggestBox(final PSuggestOracle suggestOracle) {
        super(new EntryInstruction(PROPERTY.ORACLE, suggestOracle.getID()));

        this.suggestOracle = suggestOracle;

        Txn.get().getTxnContext().save(new AddHandler(getID(), HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER));
        Txn.get().getTxnContext().save(new AddHandler(getID(), HANDLER.KEY_.STRING_SELECTION_HANDLER));
    }

    @Override
    protected void enrichCreate(final Create create) {
        super.enrichCreate(create);

        if (textBox == null) {
            textBox = new PTextBox();
        }

        create.put(PROPERTY.TEXTBOX_ID, textBox.getID());
    }

    @Override
    public void onClientData(final JSONObject event) throws JSONException {
        String handlerKey = null;
        if (event.has(HANDLER.KEY)) {
            handlerKey = event.getString(HANDLER.KEY);
        }
        if (HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER.equals(handlerKey)) {
            final String text = event.getString(PROPERTY.TEXT);
            textBox.fireOnValueChange(new PValueChangeEvent<String>(this, text));
        } else if (HANDLER.KEY_.STRING_SELECTION_HANDLER.equals(handlerKey)) {
            this.replacementString = event.getString(PROPERTY.REPLACEMENT_STRING);
            this.displayString = event.getString(PROPERTY.DISPLAY_STRING);
            this.textBox.setText(replacementString);
            final MultiWordSuggestion suggestion = new MultiWordSuggestion(replacementString, displayString);
            onSelection(new PSelectionEvent<PSuggestion>(this, suggestion));
        } else {
            super.onClientData(event);
        }
    }

    @Override
    public void onSelection(final PSelectionEvent<PSuggestion> event) {
        for (final PSelectionHandler<PSuggestion> handler : selectionHandler) {
            handler.onSelection(event);
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
        final Update update = new Update(getID());
        update.put(PROPERTY.LIMIT, limit);
        Txn.get().getTxnContext().save(update);
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
    public void removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        textBox.removeValueChangeHandler(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return textBox.getValueChangeHandlers();
    }

    public PSuggestOracle getSuggestOracle() {
        return suggestOracle;
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<PSuggestion> handler) {
        this.selectionHandler.add(handler);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<PSuggestion> handler) {
        this.selectionHandler.remove(handler);
    }

    @Override
    public Collection<PSelectionHandler<PSuggestion>> getSelectionHandlers() {
        return Collections.unmodifiableCollection(selectionHandler);
    }

    public static class MultiWordSuggestion implements PSuggestion {

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

        @Override
        public void add(final String suggestion) {
            final Update update = new Update(getID());
            update.put(PROPERTY.SUGGESTION, suggestion);
            Txn.get().getTxnContext().save(update);
        }

        @Override
        public void addAll(final Collection<String> collection) {
            final Update update = new Update(getID());
            update.put(PROPERTY.SUGGESTIONS, collection);
            Txn.get().getTxnContext().save(update);
        }

        public void setDefaultSuggestions(final Collection<String> collection) {
            final Update update = new Update(getID());
            update.put(PROPERTY.DEFAULT_SUGGESTIONS, collection);
            Txn.get().getTxnContext().save(update);
        }

        public void clear() {
            final Update update = new Update(getID());
            update.put(PROPERTY.CLEAR, true);
            Txn.get().getTxnContext().save(update);
        }

        @Override
        protected WidgetType getWidgetType() {
            return WidgetType.MULTIWORD_SUGGEST_ORACLE;
        }
    }
}
