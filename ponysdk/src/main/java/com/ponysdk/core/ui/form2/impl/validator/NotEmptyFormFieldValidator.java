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

package com.ponysdk.core.ui.form2.impl.validator;


import com.ponysdk.core.ui.form2.api.FormField;
import com.ponysdk.core.ui.form2.api.FormFieldValidator;
import com.ponysdk.core.ui.form2.api.ValidationResult;

import java.util.Objects;

public class NotEmptyFormFieldValidator implements FormFieldValidator {

    private static final String ATTR_REQUIRED = "required";

    private final String errorMessage;

    public NotEmptyFormFieldValidator() {
        this("Empty Field");
    }

    public NotEmptyFormFieldValidator(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public ValidationResult isValid(final String value) {
        if (!Objects.requireNonNullElse(value, "").isEmpty()) return ValidationResult.OK();
        return ValidationResult.KO(errorMessage);
    }

    @Override
    public void bindFormField(FormField<?> formField) {
        formField.asWidget().addStyleName(ATTR_REQUIRED);
    }

    @Override
    public void unbindFormField(FormField<?> formField) {
        formField.asWidget().removeStyleName(ATTR_REQUIRED);
    }
}
