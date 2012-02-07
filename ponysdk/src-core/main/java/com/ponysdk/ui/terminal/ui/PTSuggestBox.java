
package com.ponysdk.ui.terminal.ui;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTSuggestBox extends PTWidget {

    final MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new com.google.gwt.user.client.ui.SuggestBox(oracle));
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        switch (propertyKey) {
            case FOCUSED:
                cast().setFocus(property.getBooleanValue());
                break;
            case TEXT:
                cast().setText(property.getValue());
                break;
            case SUGGESTION:
                oracle.add(property.getValue());
                break;
            default:
                break;
        }

        super.update(update, uiService);
    }

    @Override
    public void addHandler(final AddHandler addHandler, final UIService uiService) {
        if (HandlerType.STRING_VALUE_CHANGE_HANDLER.equals(addHandler.getType())) {
            cast().addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.STRING_VALUE_CHANGE_HANDLER);
                    eventInstruction.setMainPropertyValue(PropertyKey.VALUE, event.getValue());
                    uiService.triggerEvent(eventInstruction);
                }
            });
        } else if (HandlerType.STRING_SELECTION_HANDLER.equals(addHandler.getType())) {
            cast().addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {

                @Override
                public void onSelection(final SelectionEvent<Suggestion> event) {
                    final EventInstruction eventInstruction = new EventInstruction(addHandler.getObjectID(), HandlerType.STRING_SELECTION_HANDLER);
                    eventInstruction.getMainProperty().setProperty(PropertyKey.DISPLAY_STRING, event.getSelectedItem().getDisplayString());
                    eventInstruction.getMainProperty().setProperty(PropertyKey.REPLACEMENT_STRING, event.getSelectedItem().getReplacementString());
                    uiService.triggerEvent(eventInstruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }

    }

    @Override
    public com.google.gwt.user.client.ui.SuggestBox cast() {
        return (com.google.gwt.user.client.ui.SuggestBox) uiObject;
    }
}
