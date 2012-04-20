
package com.ponysdk.ui.terminal.ui;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTSuggestBox extends PTWidget {

    final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new SuggestBox(oracle));
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.FOCUSED)) {
            cast().setFocus(update.getBoolean(PROPERTY.FOCUSED));
        } else if (update.containsKey(PROPERTY.TEXT)) {
            cast().setText(update.getString(PROPERTY.TEXT));
        } else if (update.containsKey(PROPERTY.SUGGESTION)) {
            oracle.add(update.getString(PROPERTY.SUGGESTION));
        } else if (update.containsKey(PROPERTY.LIMIT)) {
            cast().setLimit(update.getInt(PROPERTY.LIMIT));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {

        final String handler = addHandler.getString(HANDLER.KEY);

        if (HANDLER.STRING_VALUE_CHANGE_HANDLER.equals(handler)) {
            cast().addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.put(TYPE.KEY, TYPE.EVENT);
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(HANDLER.KEY, HANDLER.STRING_VALUE_CHANGE_HANDLER);
                    eventInstruction.put(PROPERTY.TEXT, event.getValue());
                    uiService.triggerEvent(eventInstruction);
                }
            });
        } else if (HANDLER.STRING_SELECTION_HANDLER.equals(handler)) {
            cast().addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {

                @Override
                public void onSelection(final SelectionEvent<Suggestion> event) {
                    final PTInstruction eventInstruction = new PTInstruction();
                    eventInstruction.setObjectID(addHandler.getObjectID());
                    eventInstruction.put(TYPE.KEY, TYPE.EVENT);
                    eventInstruction.put(HANDLER.KEY, HANDLER.STRING_SELECTION_HANDLER);
                    eventInstruction.put(PROPERTY.DISPLAY_STRING, event.getSelectedItem().getDisplayString());
                    eventInstruction.put(PROPERTY.REPLACEMENT_STRING, event.getSelectedItem().getReplacementString());
                    uiService.triggerEvent(eventInstruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    @Override
    public SuggestBox cast() {
        return (SuggestBox) uiObject;
    }
}
