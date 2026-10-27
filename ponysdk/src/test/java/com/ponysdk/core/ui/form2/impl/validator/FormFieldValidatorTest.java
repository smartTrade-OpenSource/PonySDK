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

package com.ponysdk.core.ui.form2.impl.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.form2.api.ValidationResult;
import com.ponysdk.core.ui.form2.impl.formfield.StringTextBoxFormField;
import com.ponysdk.test.PSuite;

public class FormFieldValidatorTest extends PSuite {

    @Test
    public void testEmailValidator() {
        final EmailValidator validator = new EmailValidator();
        assertTrue(validator.isValid("john.doe@pony-sdk.com").isValid());
        assertTrue(validator.isValid("a+b@c.io").isValid());
        assertEquals("Invalid email", validator.isValid("john@").getErrorMessage());
        assertFalse(validator.isValid("john.doe.com").isValid());
    }

    @Test
    public void testCompositeValidator() {
        final CompositeFormFieldValidator validator = new CompositeFormFieldValidator(new NotEmptyFormFieldValidator("empty"),
            new EmailValidator());
        assertEquals("empty", validator.isValid("").getErrorMessage());
        assertEquals("Invalid email", validator.isValid("x").getErrorMessage());
        assertEquals(ValidationResult.OK(), validator.isValid("x@y.com"));
        assertTrue(new CompositeFormFieldValidator().isValid(null).isValid());
    }

    @Test
    public void testNotEmptyBindsRequiredStyle() {
        final StringTextBoxFormField field = new StringTextBoxFormField("Name");
        PWindow.getMain().add(field);
        assertFalse(field.asWidget().hasStyleName("required"));
        field.setValidator(new NotEmptyFormFieldValidator());
        assertTrue(field.asWidget().hasStyleName("required"));
        assertEquals("Empty Field", field.validate().getErrorMessage());

        // Replacing the validator unbinds the previous one
        field.setValidator(new EmailValidator());
        assertFalse(field.asWidget().hasStyleName("required"));
        assertFalse(new NotEmptyFormFieldValidator().isValid(null).isValid());
    }
}
