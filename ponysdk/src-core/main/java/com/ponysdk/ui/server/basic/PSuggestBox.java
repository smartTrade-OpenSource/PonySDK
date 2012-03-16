
package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PSuggestBox extends PWidget implements Focusable, PValueChangeHandler<String>, HasPValueChangeHandlers<String>, PSelectionHandler<PSuggestion>, HasPSelectionHandlers<PSuggestion> {

    private final List<PValueChangeHandler<String>> valueChangeHandler = new ArrayList<PValueChangeHandler<String>>();

    private final List<PSelectionHandler<PSuggestion>> selectionHandler = new ArrayList<PSelectionHandler<PSuggestion>>();

    private PSuggestOracle suggestOracle;

    private int limit;

    private String text;

    private String replacementString;

    private String displayString;

    public PSuggestBox(final PSuggestOracle suggestOracle) {
        this.suggestOracle = suggestOracle;
        if (suggestOracle == null) {
            this.suggestOracle = new PMultiWordSuggestOracle();
        }

        getPonySession().stackInstruction(new AddHandler(getID(), HandlerType.STRING_VALUE_CHANGE_HANDLER));
        getPonySession().stackInstruction(new AddHandler(getID(), HandlerType.STRING_SELECTION_HANDLER));
    }

    public PSuggestBox() {
        this(null);
    }

    @Override
    public void onEventInstruction(final EventInstruction e) {
        if (HandlerType.STRING_VALUE_CHANGE_HANDLER.equals(e.getType())) {
            this.text = e.getMainProperty().getValue();
            onValueChange(new PValueChangeEvent<String>(this, text));
        } else if (HandlerType.STRING_SELECTION_HANDLER.equals(e.getType())) {
            this.replacementString = e.getMainProperty().getStringPropertyValue(PropertyKey.REPLACEMENT_STRING);
            this.displayString = e.getMainProperty().getStringPropertyValue(PropertyKey.DISPLAY_STRING);

            MultiWordSuggestion suggestion = new MultiWordSuggestion(replacementString, displayString);
            onSelection(new PSelectionEvent<PSuggestion>(this, suggestion));
        } else {
            super.onEventInstruction(e);
        }
    }

    @Override
    public void onSelection(final PSelectionEvent<PSuggestion> event) {
        for (final PSelectionHandler<PSuggestion> handler : selectionHandler) {
            handler.onSelection(event);
        }
    }

    @Override
    public void onValueChange(final PValueChangeEvent<String> event) {
        for (final PValueChangeHandler<String> handler : valueChangeHandler) {
            handler.onValueChange(event);
        }
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.SUGGESTBOX;
    }

    @Override
    public void setFocus(final boolean focused) {
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.FOCUSED, focused);
        getPonySession().stackInstruction(update);
    }

    public void setLimit(final int limit) {
        this.limit = limit;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.LIMIT, limit);
        getPonySession().stackInstruction(update);
    }

    public void setText(final String text) {
        this.text = text;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.TEXT, text);
        getPonySession().stackInstruction(update);
    }

    public int getLimit() {
        return limit;
    }

    public String getText() {
        return text;
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        valueChangeHandler.add(handler);
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        valueChangeHandler.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(valueChangeHandler);
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
            update.setMainPropertyValue(PropertyKey.SUGGESTION, suggestion);
            getPonySession().stackInstruction(update);
        }

        @Override
        public final void addAll(final Collection<String> collection) {
            for (String suggestion : collection) {
                add(suggestion);
            }
        }
    }
}
