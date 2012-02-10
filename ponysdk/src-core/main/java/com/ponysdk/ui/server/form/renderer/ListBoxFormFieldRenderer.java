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

import com.ponysdk.core.event.EventHandler;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.form.FormField;

public class ListBoxFormFieldRenderer implements FormFieldRenderer, HasPChangeHandlers, PChangeHandler {

    private final List<FormFieldComponent<PListBox>> fields = new ArrayList<FormFieldComponent<PListBox>>();

    private final List<PChangeHandler> changeHandlers = new ArrayList<PChangeHandler>();

    private final List<String> items = new ArrayList<String>();

    private final Map<String, Object> hiddenValueByItems = new HashMap<String, Object>();

    private final Map<Object, String> itemsByHiddenValue = new HashMap<Object, String>();

    private boolean enabled = true;

    private final String caption;

    private Object value;

    private final boolean emptySelection;

    private final boolean multipleSelect;

    private String debugID;

    public ListBoxFormFieldRenderer() {
        this(null, true, false);
    }

    public ListBoxFormFieldRenderer(final boolean emptySelection) {
        this(null, emptySelection, false);
    }

    public ListBoxFormFieldRenderer(final String caption) {
        this(caption, true, false);
    }

    public ListBoxFormFieldRenderer(final String caption, final boolean emptySelection, final boolean multipleSelect) {
        this.caption = caption;
        this.emptySelection = emptySelection;
        this.multipleSelect = multipleSelect;
    }

    public ListBoxFormFieldRenderer(final String caption, final boolean emptySelection) {
        this(caption, emptySelection, false);
    }

    @Override
    public IsPWidget render(final FormField formField) {
        final PListBox listBox = new PListBox(emptySelection, multipleSelect);
        if (debugID != null) {
            listBox.ensureDebugId(debugID);
        }
        final FormFieldComponent<PListBox> formFieldComponent = new FormFieldComponent<PListBox>(listBox);
        formFieldComponent.getInput().addChangeHandler(this);
        formFieldComponent.setCaption(caption);

        fields.add(formFieldComponent);
        addListener(formFieldComponent.getInput());

        for (final String item : items) {
            formFieldComponent.getInput().addItem(item);
        }
        setEnabled(enabled);
        return formFieldComponent;
    }

    private void addListener(final PListBox listBox) {
        for (final PChangeHandler handler : changeHandlers) {
            listBox.addChangeHandler(handler);
        }
    }

    @Override
    public void addErrorMessage(final String errorMessage) {
        for (final FormFieldComponent<PListBox> field : fields) {
            field.addErrorMessage(errorMessage);
        }
    }

    @Override
    public void clearErrorMessage() {
        for (final FormFieldComponent<PListBox> field : fields) {
            field.clearErrors();
        }
    }

    public void clear() {
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().clear();
        }
        items.clear();
        hiddenValueByItems.clear();
        itemsByHiddenValue.clear();
    }

    @Override
    public void reset() {
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().setSelectedIndex(-1);
        }
        value = null;
    }

    public void addItem(final String item) {
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().addItem(item);
        }
        items.add(item);
    }

    public void addItem(final String item, final Object hiddenValue) {
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().addItem(item);
        }
        items.add(item);
        hiddenValueByItems.put(item, hiddenValue);
        itemsByHiddenValue.put(hiddenValue, item);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().setEnabled(enabled);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setSelectedItem(final String text, final boolean selected) {
        this.value = hiddenValueByItems.get(text);
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().setSelectedItem(text, selected);
        }
    }

    public void setSelectedItem(final String text) {
        setSelectedItem(text, true);
    }

    public void setSelectedValue(final Object value, final boolean selected) {
        this.value = value;
        final String item = itemsByHiddenValue.get(value);
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().setSelectedItem(item, selected);
        }
    }

    public void setSelectedValue(final Object value) {
        setSelectedValue(value, true);
    }

    public static ListBoxFormFieldRenderer newTrueFalseListBoxFormFieldRenderer(final String caption, final String trueCaption, final String falseCaption) {
        final ListBoxFormFieldRenderer listBoxFormFieldRenderer = new ListBoxFormFieldRenderer(caption, false, false);
        listBoxFormFieldRenderer.addItem(trueCaption, true);
        listBoxFormFieldRenderer.addItem(falseCaption, false);
        return listBoxFormFieldRenderer;
    }

    public static ListBoxFormFieldRenderer newTrueFalseListBoxFormFieldRenderer(final String caption) {
        return newTrueFalseListBoxFormFieldRenderer(caption, "true", "false");
    }

    public static ListBoxFormFieldRenderer newTrueFalseListBoxFormFieldRenderer() {
        return newTrueFalseListBoxFormFieldRenderer(null, "true", "false");
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        changeHandlers.add(handler);
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().addChangeHandler(handler);
        }
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return changeHandlers;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void onChange(final Object source, final int selectedIndex) {

        // Propagate to all listbox first
        for (final FormFieldComponent<PListBox> field : fields) {
            final PListBox listBox = field.getInput();
            if (!listBox.equals(source)) { // avoid call again the selected value update
                listBox.setSelectedIndex(selectedIndex);
            }
        }

        // Then pick up the value
        if (!fields.isEmpty()) {
            value = hiddenValueByItems.get(fields.get(0).getInput().getSelectedItem());
        }
    }

    @Override
    public void setValue(final Object value) {
        setSelectedValue(value);
    }

    @Override
    public void ensureDebugID(final String debugID) {
        this.debugID = debugID;
        if (fields.isEmpty()) return;

        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().ensureDebugId(debugID);
        }
    }

    @Override
    public <H extends EventHandler> void addDomHandler(final H handler, final Type<H> type) {
        for (final FormFieldComponent<PListBox> field : fields) {
            field.getInput().addDomHandler(handler, type);
        }
    }

}
