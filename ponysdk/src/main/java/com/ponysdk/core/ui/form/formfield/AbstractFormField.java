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

package com.ponysdk.core.ui.form.formfield;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;
import com.ponysdk.core.ui.form.validator.FieldValidator;
import com.ponysdk.core.ui.form.validator.ValidationResult;
import com.ponysdk.core.util.SetUtils;

/**
 * A field of a {@link com.ponysdk.core.ui.form.Form} that can be validated or reset
 */
public abstract class AbstractFormField<T, W extends IsPWidget> implements FormField, HasPValue<T> {

    public static final String CLASS_FORM_FIELD_VALIDATOR = "form-field-validator";
    public static final String DATA_TITLE = "data-title";
    public static final String CLASS_INVALID = "invalid";

    protected final W widget;
    private Set<FormFieldListener> listeners;
    protected DataConverter<String, T> dataProvider;
    private FieldValidator validator;
    protected Set<PValueChangeHandler<T>> handlers;

    private boolean dirty = false;
    private PValueChangeHandler<T> dirtyModeHandler;

    private boolean enabled = true;
    private boolean showError = true;

    public AbstractFormField() {
        this(null);
        // If no widget, no error will be showed
        this.showError = false;
    }

    public AbstractFormField(final W widget) {
        this(widget, null);
    }

    public AbstractFormField(final W widget, final DataConverter<String, T> dataProvider) {
        this(widget, dataProvider, false);
    }

    public AbstractFormField(final W widget, final DataConverter<String, T> dataProvider, final boolean dirtyMode) {
        this.widget = widget;
        this.dataProvider = dataProvider;

        if (dirtyMode) {
            this.dirtyModeHandler = event -> dirty = true;
            this.addValueChangeHandler(dirtyModeHandler);
        }

        if (this.widget != null) {
            this.widget.asWidget().addStyleName(CLASS_FORM_FIELD_VALIDATOR);
            this.widget.asWidget().addDestroyListener(event -> onDestroy());
        }
    }

    private void onDestroy() {
        if (dirtyModeHandler != null) removeValueChangeHandler(dirtyModeHandler);
    }

    @Override
    public ValidationResult isValid() {
        ValidationResult result;
        if (enabled && validator != null) result = validator.isValid(getStringValue());
        else result = ValidationResult.newOKValidationResult();

        if (showError) {
            if (result.isValid()) {
                resetError();
            } else {
                widget.asWidget().addStyleName(CLASS_INVALID);
                widget.asWidget().setAttribute(DATA_TITLE, result.getErrorMessage());
            }
        }

        fireAfterValidation(result);

        return result;
    }

    @Override
    public void setValidator(final FieldValidator validator) {
        this.validator = validator;
    }

    @Override
    public void addFormFieldListener(final FormFieldListener listener) {
        if (listeners == null) listeners = SetUtils.newArraySet(4);
        listeners.add(listener);
    }

    @Override
    public void reset() {
        if (showError) resetError();
        reset0();
        dirty = false;
        fireAfterReset();
    }

    public void resetError() {
        widget.asWidget().removeStyleName(CLASS_INVALID);
        widget.asWidget().removeAttribute(DATA_TITLE);
    }

    protected void fireAfterReset() {
        if (listeners != null) listeners.forEach(listener -> listener.afterReset(this));
    }

    protected void fireAfterValidation(final ValidationResult result) {
        if (listeners != null) listeners.forEach(listener -> listener.afterValidation(this, result));
    }

    @Override
    public PWidget asWidget() {
        return widget.asWidget();
    }

    public W getWidget() {
        return widget;
    }

    public void setDataProvider(final DataConverter<String, T> dataProvider) {
        this.dataProvider = dataProvider;
    }

    protected abstract String getStringValue();

    protected abstract void reset0();

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<T> handler) {
        if (handlers == null) handlers = SetUtils.newArraySet(4);
        handlers.add(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<T> handler) {
        return handlers != null && handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<T>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    protected void fireValueChange(final T value) {
        if (handlers != null) handlers.forEach(handler -> handler.onValueChange(new PValueChangeEvent<>(this, value)));
    }

    @Override
    public HasPValue<T> asHasPValue() {
        return this;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Handle form field dirty state. The field is considered dirty as soon as the user edited its
     * value (even if the value is the same as the original one).
     *
     * @return true if dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    public void setShowError(final boolean showError) {
        this.showError = showError;
    }

}
