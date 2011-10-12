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

import com.ponysdk.ui.server.form.FormField;

public class PriceFieldValidator implements FieldValidator {

    private static String FIELD_MSG_NOT_A_PRICE = "Not a price";
    private static String FIELD_MSG_PRICE_TOO_LONG = "Too many digits";
    private static String FIELD_MSG_TOO_MANY_DECIMALS = "Too many decimals";

    private static final int PRICE_MAX_LENGTH = 18;
    private static final int DECIMAL_PART_MAX_LENGTH = 9;

    private boolean emptyIsValid;

    // Default, accept decimal and street price
    private boolean canBeDecimalPrice = true;
    private boolean canBeStreetPrice = true;

    public PriceFieldValidator() {
        this(true);
    }

    public PriceFieldValidator(boolean emptyIsValid) {
        this.emptyIsValid = emptyIsValid;
    }

    public PriceFieldValidator(boolean emptyIsValid, boolean canBeDecimalPrice, boolean canBeStreetPrice) {
        this.emptyIsValid = emptyIsValid;
        this.canBeDecimalPrice = canBeDecimalPrice;
        this.canBeStreetPrice = canBeStreetPrice;
    }

    @Override
    public ValidationResult isValid(FormField field) {

        final String text = (String) field.getValue();
        if (text == null || text.isEmpty()) {
            if (emptyIsValid)
                return ValidationResult.newOKValidationResult();
            return ValidationResult.newFailedValidationResult("Empty field");
        }
        return isPrice(field);
    }

    // Valid prices :
    // 12345-156
    // 10-15+
    // 10-150
    // -1-169
    // -2-65
    // .23
    // 1.25
    // -1.36
    // -.25
    private static final String VALID_32ND_REGEX = "^-?[0-9]+-[0-9]{2}[0-9+]?$";
    private static final String VALID_DOUBLE_REGEX = "^-?[0-9]*(([.][0-9]+)|([0-9]*))$";

    private ValidationResult isPrice(FormField field) {
        final String pxValue = (String) field.getValue();

        if (pxValue.length() > PRICE_MAX_LENGTH) {
            return ValidationResult.newFailedValidationResult(FIELD_MSG_PRICE_TOO_LONG);
        }

        // We check the length of the decimal part of the price, if any
        final int indexOfDot = pxValue.indexOf('.');
        if (indexOfDot != -1) {
            if (pxValue.substring(indexOfDot + 1).length() > DECIMAL_PART_MAX_LENGTH) {
                return ValidationResult.newFailedValidationResult(FIELD_MSG_TOO_MANY_DECIMALS);
            }
        }

        final boolean isValidDecimalPrice = pxValue.matches(VALID_DOUBLE_REGEX);
        if (canBeDecimalPrice && isValidDecimalPrice) {
            return ValidationResult.newOKValidationResult();
        }

        final boolean isValidStreetPx = pxValue.matches(VALID_32ND_REGEX);
        if (canBeStreetPrice && isValidStreetPx) {
            return ValidationResult.newOKValidationResult();
        }

        return ValidationResult.newFailedValidationResult(FIELD_MSG_NOT_A_PRICE);
    }

    public void setEmptyIsValid(boolean emptyIsValid) {
        this.emptyIsValid = emptyIsValid;
    }

    public void setCanBeDecimalPrice(boolean canBeDecimalPrice) {
        this.canBeDecimalPrice = canBeDecimalPrice;
    }

    public void setCanBeStreetPrice(boolean canBeStreetPrice) {
        this.canBeStreetPrice = canBeStreetPrice;
    }
}
