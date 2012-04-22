/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.ui.server.form.renderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.event.PEventHandler;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.form.FormField;

public class RadioButtonGroupFormFieldRenderer implements FormFieldRenderer, HasPChangeHandlers {

    private final List<FormFieldComponent<PVerticalPanel>> fields = new ArrayList<FormFieldComponent<PVerticalPanel>>();

    private final List<PChangeHandler> changeHandlers = new ArrayList<PChangeHandler>();

    private final List<String> items = new ArrayList<String>();

    private final List<PRadioButton> radioButtons = new ArrayList<PRadioButton>();

    private final Map<String, Object> hiddenValueByItems = new HashMap<String, Object>();

    private final Map<Object, String> itemsByHiddenValue = new HashMap<Object, String>();

    private final String caption;

    private String debugID;

    private boolean enabled;

    private Object value;

    public RadioButtonGroupFormFieldRenderer(String caption) {
        this.caption = caption;
    }

    @Override
    public IsPWidget render(FormField formField) {
        final PVerticalPanel verticalPanel = new PVerticalPanel();
        final FormFieldComponent<PVerticalPanel> formFieldComponent = new FormFieldComponent<PVerticalPanel>(verticalPanel);
        formFieldComponent.setCaption(caption);

        fields.add(formFieldComponent);

        for (final String item : items) {
            final PRadioButton radioButton = new PRadioButton(caption, item);
            if (debugID != null) {
                radioButton.ensureDebugId(debugID + "[" + item + "]");
            }

            radioButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(PClickEvent event) {
                    selectItem(item);
                }
            });
            formFieldComponent.getInput().add(radioButton);
            radioButtons.add(radioButton);
        }
        setEnabled(enabled);
        selectValue(value);
        return formFieldComponent;
    }

    @Override
    public void reset() {}

    @Override
    public void addErrorMessage(String errorMessage) {
        for (final FormFieldComponent<PVerticalPanel> field : fields) {
            field.addErrorMessage(errorMessage);
        }
    }

    @Override
    public void clearErrorMessage() {
        for (final FormFieldComponent<PVerticalPanel> field : fields) {
            field.clearErrors();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {}

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
        selectValue(value);
    }

    private void selectItem(String item) {
        setValue(hiddenValueByItems.get(item));
    }

    private void selectValue(Object value) {
        for (final PRadioButton radioButton : radioButtons) {
            if (radioButton.getText().equals(itemsByHiddenValue.get(value))) {
                radioButton.setValue(true);
            }
        }
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void ensureDebugID(String id) {
        this.debugID = id;
    }

    public void addItem(final String item) {
        for (final FormFieldComponent<PVerticalPanel> field : fields) {
            final PRadioButton radioButton = new PRadioButton(caption, item);
            field.getInput().add(radioButton);
        }
        items.add(item);
    }

    public void addItem(final String item, Object hiddenValue) {
        for (final FormFieldComponent<PVerticalPanel> field : fields) {
            final PRadioButton radioButton = new PRadioButton(caption, item);
            field.getInput().add(radioButton);
        }
        items.add(item);
        hiddenValueByItems.put(item, hiddenValue);
        itemsByHiddenValue.put(hiddenValue, item);
    }

    @Override
    public void addChangeHandler(PChangeHandler handler) {
        changeHandlers.add(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return changeHandlers;
    }

    @Override
    public <H extends PEventHandler> void addDomHandler(H handler, Type<H> type) {}
}