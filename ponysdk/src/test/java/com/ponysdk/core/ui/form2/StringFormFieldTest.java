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
import com.ponysdk.test.PSuite;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;

public class StringFormFieldTest extends PSuite {

    @Test
    public void testDefaultValidation() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        PWindow.getMain().add(formField);
        Assert.assertEquals(ValidationResult.OK(), formField.validate());
    }

    @Test
    public void testRequired() {
        final StringTextBoxFormField formField = new StringTextBoxFormField(true);
        PWindow.getMain().add(formField);
        Assert.assertEquals(FormField.REQUIRED_RESULT, formField.validate());

        final StringTextBoxFormField formField2 = new StringTextBoxFormField("Test", true);
        PWindow.getMain().add(formField2);
        Assert.assertEquals(FormField.REQUIRED_RESULT, formField2.validate());
    }

    @Test
    public void testValidator() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        PWindow.getMain().add(formField);
        formField.setValidator(new NotEmptyFieldValidator("Custom Message"));
        Assert.assertEquals("Custom Message", formField.validate().getErrorMessage());

        final StringTextBoxFormField formField2 = new StringTextBoxFormField();
        PWindow.getMain().add(formField2);
        formField2.setValidator(new NotEmptyFieldValidator());
        Assert.assertEquals("Empty Field", formField2.validate().getErrorMessage());
    }

    @Test
    public void testValidatorAndRequired() {
        final StringTextBoxFormField formField = new StringTextBoxFormField(true);
        PWindow.getMain().add(formField);
        formField.setValidator(new NotEmptyFieldValidator("Empty Field"));
        Assert.assertEquals(FormField.REQUIRED_RESULT.getErrorMessage(), formField.validate().getErrorMessage());
    }

    @Test
    public void testCaption() {
        final StringTextBoxFormField formField = new StringTextBoxFormField("Caption");
        PWindow.getMain().add(formField);
        int id = formField.asWidget().getID();
        Assert.assertEquals("Caption", formField.getCaption());
        final Element e1 = Jsoup.parse(formField.asWidget().dumpDOM()).getElementsByAttributeValue("pid", id + "").first().child(0);
        Assert.assertFalse(e1.hasAttr("hidden"));

        formField.setCaption("Caption2");
        Assert.assertEquals("Caption2", formField.getCaption());

        formField.setCaption(null);
        Assert.assertNull(formField.getCaption());
        final Element e2 = Jsoup.parse(formField.asWidget().dumpDOM()).getElementsByAttributeValue("pid", id + "").first().child(0);
        Assert.assertTrue(e2.hasAttr("hidden"));
    }

    @Test
    public void testValidationWhenDisabled() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        PWindow.getMain().add(formField);
        formField.setValidator(new NotEmptyFieldValidator("Empty Field"));
        formField.setEnabled(false);
        Assert.assertEquals(ValidationResult.OK(), formField.validate());
        formField.setEnabled(true);
        Assert.assertEquals("Empty Field", formField.validate().getErrorMessage());
    }

    @Test
    public void testDirty() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        PWindow.getMain().add(formField);
        formField.setInitialValue("Initial");
        Assert.assertTrue(formField.hasDiff());
        formField.setValue("Initial");
        Assert.assertFalse(formField.hasDiff());
        formField.setValue("Diff");
        Assert.assertTrue(formField.hasDiff());
        formField.reset();
        Assert.assertFalse(formField.hasDiff());

        formField.setValue("Initial2", true);
        Assert.assertFalse(formField.hasDiff());
    }

    @Test
    public void testInnerWidget() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        PWindow.getMain().add(formField);
        Assert.assertNotNull(formField.getInnerWidget());
        Assert.assertTrue(formField.getInnerWidget().hasStyleName("inner-widget"));
    }

    @Test
    public void testEnabled() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        PWindow.getMain().add(formField);
        Assert.assertTrue(formField.isEnabled());
        formField.setEnabled(false);
        Assert.assertFalse(formField.isEnabled());
    }

    @Test
    public void testValue() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        PWindow.getMain().add(formField);
        formField.setValue("Test");
        Assert.assertEquals("Test", formField.getValue());
        formField.reset();
        Assert.assertEquals("", formField.getValue());
    }
}
