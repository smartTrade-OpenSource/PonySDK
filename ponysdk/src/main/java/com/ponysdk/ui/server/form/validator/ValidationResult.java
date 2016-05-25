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

package com.ponysdk.ui.server.form.validator;

/**
 * Returned by a {@link FieldValidator}
 */
public class ValidationResult {

    public static ValidationResult newOKValidationResult() {
        final ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);
        return validationResult;
    }

    public static ValidationResult newFailedValidationResult(final String errorMessage) {
        final ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(false);
        validationResult.setErrorMessage(errorMessage);
        return validationResult;
    }

    private boolean valid;

    private String errorMessage;

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }
}
