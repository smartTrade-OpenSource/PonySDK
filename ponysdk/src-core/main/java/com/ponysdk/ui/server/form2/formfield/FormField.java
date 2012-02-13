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

package com.ponysdk.ui.server.form2.formfield;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ponysdk.ui.server.basic.HasPValue;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form2.dataconverter.DataConverter;
import com.ponysdk.ui.server.form2.validator.FieldValidator;
import com.ponysdk.ui.server.form2.validator.ValidationResult;

public abstract class FormField<T> implements HasPValue<T>, IsPWidget {

    private final List<PValueChangeHandler<T>> handlers = new ArrayList<PValueChangeHandler<T>>();

    private FieldValidator validator;

    protected DataConverter<String, T> dataProvider;

    public FormField(final DataConverter<String, T> dataProvider) {
        this.dataProvider = dataProvider;
    }

    public ValidationResult isValid() {

        if (validator == null) return ValidationResult.newOKValidationResult();

        return validator.isValid(getStringValue());
    }

    public void setValidator(final FieldValidator validator) {
        this.validator = validator;
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<T> handler) {
        handlers.add(handler);
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<T> handler) {
        handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<T>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    protected abstract String getStringValue();

    public abstract void reset();
}
