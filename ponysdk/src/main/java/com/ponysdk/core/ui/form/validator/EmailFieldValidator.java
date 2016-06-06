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

import com.ponysdk.core.internalization.PString;

public class EmailFieldValidator implements FieldValidator {

    private static final String EMAILS_SEPARATOR = ";";
    private static final String VALID_MAIL_REGEX = "^[a-z0-9._-]+@[a-z0-9.-]{1,}[.][a-z]{2,3}";

    @Override
    public ValidationResult isValid(final String value) {
        if (value == null || value.isEmpty()) return ValidationResult.newOKValidationResult();

        final String[] emails = value.split(EMAILS_SEPARATOR);
        for (int i = 0; i < emails.length; i++) {
            if (!emails[i].matches(VALID_MAIL_REGEX)) { return ValidationResult.newFailedValidationResult(PString.get("validator.error.email")); }
        }

        return ValidationResult.newOKValidationResult();
    }

}
