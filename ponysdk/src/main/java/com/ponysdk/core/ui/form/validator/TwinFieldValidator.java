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

package com.ponysdk.core.ui.form.validator;

import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.form.formfield.FormField;

public class TwinFieldValidator implements FieldValidator {

    private final FormField<String, ? extends PWidget> twinFormField;

    private final String errorMessage;

    public TwinFieldValidator(final String errorMessage, final FormField<String, ? extends PWidget> twinFormField) {
        this.errorMessage = errorMessage;
        this.twinFormField = twinFormField;
    }

    @Override
    public ValidationResult isValid(final String value) {
        final String twinText = twinFormField.getValue();
        if (value == null && twinText == null) {
            return ValidationResult.newOKValidationResult();
        } else if (value == null || !value.equals(twinText)) {
            return ValidationResult.newFailedValidationResult(errorMessage);
        } else {
            return ValidationResult.newOKValidationResult();
        }
    }

}
