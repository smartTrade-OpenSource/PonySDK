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

package com.ponysdk.ui.server.form.formfield;

import java.util.HashSet;
import java.util.Set;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.form.Form;
import com.ponysdk.ui.server.form.dataconverter.DataConverter;
import com.ponysdk.ui.server.form.validator.FieldValidator;
import com.ponysdk.ui.server.form.validator.ValidationResult;
import com.ponysdk.ui.server.list.Resetable;
import com.ponysdk.ui.server.list.Validable;

/**
 * A field of a {@link Form} that can be validated or reset
 */
public abstract class FormField<T> implements IsPWidget, Validable, Resetable {

    private final Set<FormFieldListener> listeners = new HashSet<>();

    private FieldValidator validator;

    protected DataConverter<String, T> dataProvider;

    public FormField(final DataConverter<String, T> dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public ValidationResult isValid() {
        ValidationResult result;

        if (validator == null) result = ValidationResult.newOKValidationResult();
        else result = validator.isValid(getStringValue());

        fireAfterValidation(result);

        return result;
    }

    public void setValidator(final FieldValidator validator) {
        this.validator = validator;
    }

    public void addFormFieldListener(final FormFieldListener listener) {
        listeners.add(listener);
    }

    @Override
    public void reset() {
        reset0();
        fireAfterReset();
    }

    private void fireAfterReset() {
        for (final FormFieldListener listener : listeners) {
            listener.afterReset(this);
        }
    }

    private void fireAfterValidation(final ValidationResult result) {
        for (final FormFieldListener listener : listeners) {
            listener.afterValidation(this, result);
        }
    }

    public void setDataProvider(final DataConverter<String, T> dataProvider) {
        this.dataProvider = dataProvider;
    }

    protected abstract String getStringValue();

    protected abstract void reset0();

    public abstract T getValue();

    public abstract void setValue(T value);

}
