
package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

public class PSuggestBox extends PWidget implements Focusable, HasPValueChangeHandlers<String>, PSelectionHandler<PSuggestion>, HasPSelectionHandlers<PSuggestion> {

    private final List<PSelectionHandler<PSuggestion>> selectionHandler = new ArrayList<PSelectionHandler<PSuggestion>>();

    private PSuggestOracle suggestOracle;
    private PTextBox textBox;

    private int limit;

    private String replacementString;
    private String displayString;

    public PSuggestBox(final PSuggestOracle suggestOracle) {

        this.suggestOracle = suggestOracle;
        if (suggestOracle == null) {
            this.suggestOracle = new PMultiWordSuggestOracle();
        }

        getPonySession().stackInstruction(new AddHandler(getID(), HANDLER.STRING_VALUE_CHANGE_HANDLER));
        getPonySession().stackInstruction(new AddHandler(getID(), HANDLER.STRING_SELECTION_HANDLER));

        create.put(PROPERTY.TEXTBOX_ID, textBox.getID());
    }

    @Override
    protected void init(final WidgetType widgetType) {
        this.textBox = new PTextBox();
        super.init(widgetType);
    }

    public PSuggestBox() {
        this(null);
    }

    @Override
    public void onEventInstruction(final JSONObject event) throws JSONException {
        String handlerKey = null;
        if (event.has(HANDLER.KEY)) {
            handlerKey = event.getString(HANDLER.KEY);
        }
        if (HANDLER.STRING_VALUE_CHANGE_HANDLER.equals(handlerKey)) {
            final String text = event.getString(PROPERTY.TEXT);
            textBox.onValueChange(new PValueChangeEvent<String>(this, text));
        } else if (HANDLER.STRING_SELECTION_HANDLER.equals(handlerKey)) {
            this.replacementString = event.getString(PROPERTY.REPLACEMENT_STRING);
            this.displayString = event.getString(PROPERTY.DISPLAY_STRING);
            this.textBox.setText(replacementString);
            final MultiWordSuggestion suggestion = new MultiWordSuggestion(replacementString, displayString);
            onSelection(new PSelectionEvent<PSuggestion>(this, suggestion));
        } else {
            super.onEventInstruction(event);
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
        getPonySession().stackInstruction(update);
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

    public abstract class PSuggestOracle {

        public abstract void add(final String suggestion);

        public abstract void addAll(final Collection<String> collection);
    }

    public class PMultiWordSuggestOracle extends PSuggestOracle {

        @Override
        public void add(final String suggestion) {
            final Update update = new Update(getID());
            update.put(PROPERTY.SUGGESTION, suggestion);
            getPonySession().stackInstruction(update);
        }

        @Override
        public final void addAll(final Collection<String> collection) {
            for (final String suggestion : collection) {
                add(suggestion);
            }
        }
    }
}
