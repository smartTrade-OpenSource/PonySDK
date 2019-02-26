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

public class CompositeFieldValidator implements FieldValidator {

    private final FieldValidator[] fieldValidators;

    public CompositeFieldValidator(final FieldValidator... fieldValidators) {
        this.fieldValidators = fieldValidators;
    }

    @Override
    public ValidationResult isValid(final String value) {
        final ValidationResult validationResult = ValidationResult.newOKValidationResult();
        for (final FieldValidator fieldValidator : fieldValidators) {
            final ValidationResult result = fieldValidator.isValid(value);
            if (result.getData() != null) validationResult.setData(result.getData());
            if (!result.isValid()) {
                validationResult.setValid(false);
                validationResult.setErrorMessage(result.getErrorMessage());
                return validationResult;
            }
        }
        return validationResult;
    }
}
