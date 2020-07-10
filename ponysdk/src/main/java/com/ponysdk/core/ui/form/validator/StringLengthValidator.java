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

import com.ponysdk.core.ui.i18n.PString;

public class StringLengthValidator implements FieldValidator {

    private int minLength = 0;
    private int maxLength = 0;

    public StringLengthValidator(final int minLength, final int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public ValidationResult isValid(final String value) {
        if (value == null || value.isEmpty()) return ValidationResult.newOKValidationResult();
        if (value.length() < minLength)
            return ValidationResult.newFailedValidationResult(PString.get("validator.error.minlength", minLength));
        if (value.length() > maxLength)
            return ValidationResult.newFailedValidationResult(PString.get("validator.error.maxlength", maxLength));
        return ValidationResult.newOKValidationResult();
    }
}
