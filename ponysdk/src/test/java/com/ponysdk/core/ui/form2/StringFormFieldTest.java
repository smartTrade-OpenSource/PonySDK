/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.form2;

import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.form2.api.ValidationResult;
import com.ponysdk.core.ui.form2.impl.validator.NotEmptyFormFieldValidator;
import com.ponysdk.core.ui.form2.impl.formfield.StringTextBoxFormField;
import com.ponysdk.test.PSuite;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import static org.junit.Assert.*;
import org.junit.Test;

public class StringFormFieldTest extends PSuite {

    @Test
    public void testDefaultValidation() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        assertEquals(ValidationResult.OK(), formField.validate());
    }

    @Test
    public void testValidator() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        formField.setValidator(new NotEmptyFormFieldValidator("Custom Message"));
        assertEquals("Custom Message", formField.validate().getErrorMessage());

        final StringTextBoxFormField formField2 = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField2);
        formField2.setValidator(new NotEmptyFormFieldValidator());
        assertEquals("Empty Field", formField2.validate().getErrorMessage());
    }

    @Test
    public void testValidatorAndRequired() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        formField.setValidator(new NotEmptyFormFieldValidator("Empty Field"));
        assertEquals("Empty Field", formField.validate().getErrorMessage());
    }

    @Test
    public void testCaption() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        int id = formField.asWidget().getID();
        assertEquals("Caption", formField.getCaption());
        final Element e1 = Jsoup.parse(formField.asWidget().dumpDOM()).getElementsByAttributeValue("pid", id + "").first().child(0);
        assertFalse(e1.hasAttr("hidden"));

        formField.setCaption("Caption2");
        assertEquals("Caption2", formField.getCaption());

        formField.setCaption(null);
        assertNull(formField.getCaption());
        final Element e2 = Jsoup.parse(formField.asWidget().dumpDOM()).getElementsByAttributeValue("pid", id + "").first().child(0);
        assertTrue(e2.hasAttr("hidden"));
    }

    @Test
    public void testDescription() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("");
        formField.setDescription("Description");
        PWindow.getMain().add(formField);
        int id = formField.asWidget().getID();
        assertEquals("Description", formField.getDescription());
        final Element e1 = Jsoup.parse(formField.asWidget().dumpDOM()).getElementsByAttributeValue("pid", id + "").first().child(0);
        assertFalse(e1.hasAttr("hidden"));

        formField.setDescription("Description2");
        assertEquals("Description2", formField.getDescription());

        formField.setDescription(null);
        assertNull(formField.getDescription());
    }

    @Test
    public void testValidationWhenDisabled() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        formField.setValidator(new NotEmptyFormFieldValidator("Empty Field"));
        formField.disable();
        assertEquals(ValidationResult.OK(), formField.validate());
        formField.enable();
        assertEquals("Empty Field", formField.validate().getErrorMessage());
    }

    @Test
    public void testDirty() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        formField.commit();
        formField.setValue("Initial");
        assertTrue(formField.isDirty());
        formField.commit();
        formField.setValue("Initial");
        assertFalse(formField.isDirty());
        formField.setValue("Diff");
        assertTrue(formField.isDirty());
        formField.validate();
        assertTrue(formField.asWidget().dumpDOM().contains("dirty"));
        formField.reset();
        assertFalse(formField.isDirty());
        formField.setValue("Initial2");
        assertTrue(formField.isDirty());
    }

    @Test
    public void testEnabled() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        assertTrue(formField.isEnabled());
        formField.disable();
        assertFalse(formField.isEnabled());
    }

    @Test
    public void testValue() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        formField.setValue("Test");
        assertEquals("Test", formField.getValue());
        formField.reset();
        assertEquals("", formField.getValue());
    }
}
