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
import java.util.Collections;
import java.util.List;

import com.ponysdk.core.event.EventHandler;
import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.ui.server.basic.HasPValueChangeHandlers;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PTextBoxBase;
import com.ponysdk.ui.server.basic.event.HasPKeyPressHandlers;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PDomEvent.Type;
import com.ponysdk.ui.server.basic.event.PHasText;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form.FormField;

public class TextBoxBaseFormFieldRenderer implements FormFieldRenderer, PValueChangeHandler<String>, PHasText, HasPValueChangeHandlers<String>, HasPKeyPressHandlers {

    private String caption;

    protected final List<FormFieldComponent<? extends PTextBoxBase>> fields = new ArrayList<FormFieldComponent<? extends PTextBoxBase>>();

    private final List<PValueChangeHandler<String>> valueChangeHandlers = new ArrayList<PValueChangeHandler<String>>();

    private final List<PKeyPressHandler> keypPressHandlers = new ArrayList<PKeyPressHandler>();

    private boolean enabled = true;

    private String value;

    protected String debugID;

    // private final List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>();

    public TextBoxBaseFormFieldRenderer() {
        this(null);
    }

    public TextBoxBaseFormFieldRenderer(final String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(final String caption) {
        this.caption = caption;
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.setCaption(caption);
        }
    }

    @Override
    public void setValue(final Object value) {
        if (value != null) {
            this.value = value.toString();
        } else {
            this.value = null;
        }

        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().setText(this.value);
        }
    }

    @Override
    public String getValue() {// temp must be removed
        if (value == null) return null;
        if (value.toString().isEmpty()) return null;
        return value.toString();
    }

    @Override
    public IsPWidget render(final FormField formField) {
        final PTextBoxBase textBox = new PTextBoxBase();
        if (debugID != null) {
            textBox.ensureDebugId(debugID);
        }
        final FormFieldComponent<PTextBoxBase> textField = buildTextField(textBox);
        fields.add(textField);
        return textField;
    }

    protected <T extends PTextBoxBase> FormFieldComponent<T> buildTextField(final T t) {
        final FormFieldComponent<T> formFieldComponent = new FormFieldComponent<T>(t);
        formFieldComponent.getInput().addValueChangeHandler(this);
        formFieldComponent.getInput().setText(value);
        formFieldComponent.getInput().setEnabled(enabled);
        formFieldComponent.setCaption(caption);
        addListener(formFieldComponent.getInput());
        return formFieldComponent;
    }

    private void addListener(final PTextBoxBase textBox) {
        for (final PValueChangeHandler<String> handler : valueChangeHandlers) {
            textBox.addValueChangeHandler(handler);
        }
        for (final PKeyPressHandler handler : keypPressHandlers) {
            textBox.addKeyPressHandler(handler);
        }
    }

    @Override
    public void addErrorMessage(final String errorMessage) {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.addErrorMessage(errorMessage);
        }
    }

    @Override
    public void clearErrorMessage() {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.clearErrors();
        }
    }

    @Override
    public void reset() {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().setText(null);

        }
        value = null;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().setEnabled(enabled);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().addValueChangeHandler(handler);
        }
    }

    public void addClickHandler(final PClickHandler handler) {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().addClickHandler(handler);
        }
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<String> handler) {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().removeValueChangeHandler(handler);
        }
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(valueChangeHandlers);
    }

    @Override
    public HandlerRegistration addKeyPressHandler(final PKeyPressHandler handler) {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().addKeyPressHandler(handler);
        }
        keypPressHandlers.add(handler);

        return new HandlerRegistration() {

            @Override
            public void removeHandler() {
                keypPressHandlers.remove(handler);
            }
        };
    }

    @Override
    public List<PKeyPressHandler> getKeyPressHandlers() {
        return keypPressHandlers;
    }

    @Override
    public String getText() {
        return value;
    }

    @Override
    public void setText(final String text) {
        this.value = text;
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().setText(text);
        }
    }

    @Override
    public void onValueChange(final PValueChangeEvent<String> event) {
        setText(event.getValue());
    }

    @Override
    public void ensureDebugID(final String debugID) {
        this.debugID = debugID;
        if (fields.isEmpty()) return;

        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().ensureDebugId(debugID);
        }
    }

    public List<FormFieldComponent<? extends PTextBoxBase>> getFields() {
        return fields;
    }

    @Override
    public <H extends EventHandler> void addDomHandler(final H handler, final Type<H> type) {
        for (final FormFieldComponent<? extends PTextBoxBase> field : fields) {
            field.getInput().addDomHandler(handler, type);
        }
    }

}
