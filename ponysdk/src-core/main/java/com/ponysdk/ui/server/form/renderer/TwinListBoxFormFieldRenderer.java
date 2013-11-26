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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ponysdk.core.event.EventHandler;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.HasPChangeHandlers;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class TwinListBoxFormFieldRenderer<T> implements FormFieldRenderer, HasPChangeHandlers, PChangeHandler {

    private final List<PChangeHandler> changeHandlers = new ArrayList<PChangeHandler>();

    private final List<String> items = new ArrayList<String>();

    private final Map<String, Object> hiddenValueByItems = new LinkedHashMap<String, Object>();

    private final Map<Object, String> itemsByHiddenValue = new LinkedHashMap<Object, String>();

    private boolean enabled = true;

    private final String caption;

    private final Set<String> selectValues = new HashSet<String>();

    private PListBox selected;
    private PListBox unselected;

    private PButton switchButton;

    private String debugID;

    private FormFieldComponent<PHorizontalPanel> formFieldComponent;

    public TwinListBoxFormFieldRenderer() {
        this(null);
    }

    public TwinListBoxFormFieldRenderer(final String caption) {
        this.caption = caption;
    }

    @Override
    public IsPWidget render(final FormField formField) {
        selected = new PListBox(true, true);
        unselected = new PListBox(true, true);
        final PHorizontalPanel panel = new PHorizontalPanel();
        formFieldComponent = new FormFieldComponent<PHorizontalPanel>(panel);
        panel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        selected.addChangeHandler(this);
        unselected.addChangeHandler(this);
        panel.setSpacing(5);

        for (final String item : items) {
            unselected.addItem(item);
        }
        panel.setTitle("caption");
        final PWidget unselectedWidget = unselected.asWidget();
        panel.add(unselectedWidget);
        switchButton = new PButton("<>");
        formFieldComponent.setCaption(caption);
        switchButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                selectValues.clear();
                for (int i = 1; i < unselected.getItemCount(); i++) {
                    if (unselected.isItemSelected(i)) {
                        selectValues.add(unselected.getItem(i));
                    }
                }

                for (int i = 1; i < selected.getItemCount(); i++) {
                    if (!selected.isItemSelected(i)) {
                        selectValues.add(selected.getItem(i));
                    }
                }
                refresh();
            }
        });
        final PWidget button = switchButton.asWidget();
        panel.add(button);
        final PWidget selectedWidget = selected.asWidget();
        panel.add(selectedWidget);
        panel.setCellVerticalAlignment(selectedWidget, PVerticalAlignment.ALIGN_TOP);
        panel.setCellHeight(selectedWidget, "100%");
        panel.setCellHeight(unselectedWidget, "100%");
        panel.setCellVerticalAlignment(button, PVerticalAlignment.ALIGN_TOP);
        panel.setCellVerticalAlignment(unselectedWidget, PVerticalAlignment.ALIGN_TOP);
        if (debugID != null) {
            onEnsureDebugID();
        }
        return formFieldComponent.asWidget();
    }

    @Override
    public void addErrorMessage(final String errorMessage) {
        formFieldComponent.addErrorMessage(errorMessage);
    }

    @Override
    public void clearErrorMessage() {
        formFieldComponent.clearErrors();
    }

    public void clear() {
        unselected.clear();
        selected.clear();
        selectValues.clear();
        items.clear();
        hiddenValueByItems.clear();
        itemsByHiddenValue.clear();
    }

    @Override
    public void reset() {
        selected.clear();
        unselected.clear();
        for (final String item : hiddenValueByItems.keySet()) {
            unselected.addItem(item);
        }
        selectValues.clear();
    }

    public void addItem(final String item) {
        addItem(item, item);
    }

    public void addItem(final String item, final Object hiddenValue) {
        unselected.addItem(item);
        items.add(item);
        hiddenValueByItems.put(item, hiddenValue);
        itemsByHiddenValue.put(hiddenValue, item);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        unselected.setEnabled(enabled);
        selected.setEnabled(enabled);
        switchButton.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    void refresh() {
        unselected.clear();
        selected.clear();
        unselected.setSelectedIndex(-1);
        selected.setSelectedIndex(-1);
        for (final String item : hiddenValueByItems.keySet()) {
            if (selectValues.contains(item)) {
                selected.addItem(item);
            } else {
                unselected.addItem(item);
            }
        }
    }

    public void setSelectedItem(final String text, final boolean selected) {

        final Object selectedValue = hiddenValueByItems.get(text);
        if (selectedValue != null) {
            if (selected) {
                if (selectValues.contains(text)) {
                    throw new IllegalArgumentException("Item '" + text + "' already selected for listbox '" + caption + "'");
                } else {
                    selectValues.add(text);
                    refresh();
                }
            } else {
                if (selectValues.contains(text)) {
                    selectValues.remove(text);
                    refresh();
                } else {
                    throw new IllegalArgumentException("Item '" + text + "' already unselected for listbox '" + caption + "'");
                }
            }
        } else throw new IllegalArgumentException("unknow Item '" + text + "' for listbox '" + caption + "'");
    }

    public void setSelectedItem(final String text) {
        setSelectedItem(text, true);
    }

    public void setSelectedValue(final Object value, final boolean selected) {
        final String item = itemsByHiddenValue.get(value);
        setSelectedItem(item, selected);
    }

    public void setSelectedValue(final Object value) {
        setSelectedValue(value, true);
    }

    @Override
    public void addChangeHandler(final PChangeHandler handler) {
        changeHandlers.add(handler);
    }

    @Override
    public Collection<PChangeHandler> getChangeHandlers() {
        return changeHandlers;
    }

    @Override
    public List<T> getValue() {
        final ArrayList<T> values = new ArrayList<T>();
        for (final String selectedItem : selectValues) {
            @SuppressWarnings("unchecked")
            final T t = (T) hiddenValueByItems.get(selectedItem);
            values.add(t);
        }
        return values;
    }

    @Override
    public void onChange(final PChangeEvent source) {
        for (final PChangeHandler changeHandler : changeHandlers) {
            changeHandler.onChange(source);
        }
    }

    @Override
    public void setValue(final Object value) {
        setSelectedValue(value);
    }

    @Override
    public void ensureDebugID(final String id) {
        this.debugID = id;
        if (selected == null) return;
        onEnsureDebugID();
    }

    private void onEnsureDebugID() {
        selected.ensureDebugId(debugID + "[selected]");
        unselected.ensureDebugId(debugID + "[unselected]");
        switchButton.ensureDebugId(debugID + "[switch_button]");
    }

    @Override
    public <H extends EventHandler> void addDomHandler(final H handler, final Type<H> type) {
        selected.addDomHandler(handler, type);
        unselected.addDomHandler(handler, type);
    }

    public PListBox getSelected() {
        return selected;
    }

    public PListBox getUnselected() {
        return unselected;
    }

}
