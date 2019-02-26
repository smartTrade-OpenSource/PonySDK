/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;
import com.ponysdk.core.ui.form.validator.FieldValidator;
import com.ponysdk.core.ui.form.validator.ValidationResult;

public class ContainerFormField<T> extends AbstractFormField<T, PPanel> implements FormFieldListener, PValueChangeHandler<T> {

    private final AbstractFormField<T, ? extends IsPWidget> formField;

    public ContainerFormField(final AbstractFormField<T, ? extends IsPWidget> formField, final PPanel widget) {
        super(widget);

        this.formField = formField;
        this.formField.setShowError(false);
        this.formField.addFormFieldListener(this);
        this.formField.addValueChangeHandler(this);
        this.formField.asWidget().removeStyleName(CLASS_FORM_FIELD_VALIDATOR);
    }

    @Override
    public T getValue() {
        return formField.getValue();
    }

    @Override
    public void setValue(final T value) {
        formField.setValue(value);
    }

    @Override
    protected String getStringValue() {
        return formField.getStringValue();
    }

    @Override
    public void afterValidation(final FormField formField, final ValidationResult result) {
        if (result.isValid()) {
            resetError();
        } else {
            widget.asWidget().addStyleName(CLASS_INVALID);
            widget.asWidget().setAttribute(DATA_TITLE, result.getErrorMessage());
        }
        fireAfterValidation(result);
    }

    @Override
    public void afterReset(final FormField formField) {
        resetError();
        fireAfterReset();
    }

    @Override
    public void onValueChange(final PValueChangeEvent<T> event) {
        fireValueChange(getValue());
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        formField.setEnabled(enabled);
    }

    @Override
    public HasPValue<T> asHasPValue() {
        return formField.asHasPValue();
    }

    @Override
    public boolean isDirty() {
        return formField.isDirty();
    }

    @Override
    public ValidationResult isValid() {
        return formField.isValid();
    }

    @Override
    public void reset() {
        formField.reset();
    }

    @Override
    public void setValidator(final FieldValidator validator) {
        formField.setValidator(validator);
    }

    @Override
    public void setDataProvider(final DataConverter<String, T> dataProvider) {
        formField.setDataProvider(dataProvider);
    }

    @Override
    protected void reset0() {
        // Nothing to do
    }

}
