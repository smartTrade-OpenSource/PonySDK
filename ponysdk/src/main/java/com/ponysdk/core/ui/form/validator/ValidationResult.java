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

/**
 * Returned by a {@link com.ponysdk.core.ui.form.validator.FieldValidator}
 */
public class ValidationResult {

    private boolean valid;
    private String errorMessage;
    private Object data;

    public static ValidationResult newOKValidationResult() {
        return newOKValidationResult(null);
    }

    public static ValidationResult newOKValidationResult(final Object data) {
        return newValidationResult(true, data);
    }

    public static ValidationResult newFailedValidationResult(final String errorMessage) {
        return newFailedValidationResult(errorMessage, null);
    }

    public static ValidationResult newFailedValidationResult(final String errorMessage, final Object data) {
        final ValidationResult validationResult = newValidationResult(false, data);
        validationResult.setErrorMessage(errorMessage);
        return validationResult;
    }

    public static ValidationResult newValidationResult(final boolean valid, final Object data) {
        final ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(valid);
        validationResult.setData(data);
        return validationResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "valid : " + valid + (data != null ? " ; data : " + data : "") + (valid ? " ; " + errorMessage : "");
    }

}
