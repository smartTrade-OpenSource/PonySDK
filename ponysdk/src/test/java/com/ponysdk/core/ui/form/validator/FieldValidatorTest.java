/*
 * Copyright (c) 2026 PonySDK
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.test.PSuite;

public class FieldValidatorTest extends PSuite {

    private static void assertValid(final FieldValidator validator, final String value) {
        assertTrue(validator.isValid(value).isValid(), value);
    }

    private static void assertInvalid(final FieldValidator validator, final String value, final String message) {
        final ValidationResult result = validator.isValid(value);
        assertFalse(result.isValid(), value);
        assertEquals(message, result.getErrorMessage());
    }

    @Test
    public void testNumberValidators() {
        for (final FieldValidator validator : new FieldValidator[] { new IntegerFieldValidator(), new LongFieldValidator(),
                                                                     new DoubleFieldValidator(), new FloatFieldValidator() }) {
            assertValid(validator, null);
            assertValid(validator, "");
            assertValid(validator, "12");
        }
        assertInvalid(new IntegerFieldValidator(), "1.5", "Not an integer");
        assertInvalid(new LongFieldValidator(), "x", "Not a long");
        assertInvalid(new DoubleFieldValidator(), "x", "Not a double");
        assertInvalid(new FloatFieldValidator(), "x", "Not a float");
        assertValid(new DoubleFieldValidator(), "1.5");
        assertTrue(new IntegerFieldValidator().isAInteger("3").isValid());
    }

    @Test
    public void testEmailValidator() {
        final EmailFieldValidator validator = new EmailFieldValidator();
        assertValid(validator, null);
        assertValid(validator, "");
        assertValid(validator, "john.doe@pony.com");
        assertValid(validator, "a@b.fr;c@d.org");
        assertInvalid(validator, "a@b.fr;nope", "Invalid e-mail");
        assertInvalid(validator, "nope", "Invalid e-mail");
    }

    @Test
    public void testEmptyAndUncheckedValidators() {
        assertInvalid(new NotEmptyFieldValidator(), null, "Empty field");
        assertInvalid(new NotEmptyFieldValidator("custom"), "", "custom");
        assertValid(new NotEmptyFieldValidator(), "x");

        final UncheckedFieldValidator unchecked = new UncheckedFieldValidator();
        assertValid(unchecked, "true");
        assertInvalid(unchecked, "false", "Unchecked field");
        assertInvalid(unchecked, null, "Unchecked field");
        assertInvalid(unchecked, "", "Unchecked field");
    }

    @Test
    public void testStringLengthValidator() {
        final StringLengthValidator validator = new StringLengthValidator(2, 4);
        assertValid(validator, null);
        assertValid(validator, "");
        assertValid(validator, "abc");
        assertInvalid(validator, "a", "2 chars minimum");
        assertInvalid(validator, "abcde", "4 chars maximum");
    }

    @Test
    public void testTwinFieldValidator() {
        final StringTextBoxFormField twin = new StringTextBoxFormField();
        final TwinFieldValidator validator = new TwinFieldValidator("mismatch", twin);
        assertValid(validator, null);
        assertInvalid(validator, "a", "mismatch");
        twin.setValue("a");
        assertValid(validator, "a");
        assertInvalid(validator, null, "mismatch");
        assertInvalid(validator, "b", "mismatch");
    }

    @Test
    public void testCompositeValidator() {
        final CompositeFieldValidator validator = new CompositeFieldValidator(new NotEmptyFieldValidator("empty"),
            value -> ValidationResult.newOKValidationResult("data"), new StringLengthValidator(0, 3));
        assertInvalid(validator, "", "empty");
        final ValidationResult ok = validator.isValid("ab");
        assertTrue(ok.isValid());
        assertEquals("data", ok.getData());
        final ValidationResult tooLong = validator.isValid("abcd");
        assertFalse(tooLong.isValid());
        assertEquals("3 chars maximum", tooLong.getErrorMessage());
        assertEquals("data", tooLong.getData());
    }

    @Test
    public void testValidationResult() {
        final ValidationResult ok = ValidationResult.newOKValidationResult();
        assertTrue(ok.isValid());
        assertNull(ok.getData());
        assertNull(ok.getErrorMessage());

        final ValidationResult failed = ValidationResult.newFailedValidationResult("ko", 3);
        assertFalse(failed.isValid());
        assertEquals("ko", failed.getErrorMessage());
        assertEquals(3, failed.getData());
        assertEquals("valid : false ; data : 3", failed.toString().substring(0, "valid : false ; data : 3".length()));

        final ValidationResult custom = ValidationResult.newValidationResult(true, "d");
        custom.setValid(false);
        custom.setErrorMessage("m");
        custom.setData("e");
        assertFalse(custom.isValid());
        assertEquals("e", custom.getData());
        assertEquals("valid : true", ValidationResult.newOKValidationResult().toString().substring(0, 12));
    }

    @Test
    public void testValidationResultToString() {
        assertEquals("valid : true", ValidationResult.newOKValidationResult().toString());
        assertEquals("valid : false ; ko", ValidationResult.newFailedValidationResult("ko").toString());
    }
}
