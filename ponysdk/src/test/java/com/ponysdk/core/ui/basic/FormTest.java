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

package com.ponysdk.core.ui.basic;

import com.ponysdk.framework.PSuite;
import org.junit.Assert;
import org.junit.Test;

import com.ponysdk.core.ui.form.Form;
import com.ponysdk.core.ui.form.formfield.StringListBoxFormField;
import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.form.validator.NotEmptyFieldValidator;

public class FormTest extends PSuite {

    @Test
    public void testForm1() {
        final Form form = new Form();

        final StringTextBoxFormField formField = new StringTextBoxFormField();
        form.addFormField(formField);
        Assert.assertTrue(form.isValid());

        formField.setValidator(new NotEmptyFieldValidator());
        Assert.assertFalse(form.isValid());

        formField.setValue("test");
        Assert.assertTrue(form.isValid());

        formField.reset();
        Assert.assertFalse(form.isValid());
    }

    @Test
    public void testForm2() {
        final Form form = new Form();

        final StringListBoxFormField formField = new StringListBoxFormField();
        formField.getWidget().addItem("test1");
        formField.getWidget().addItem("test2");
        formField.getWidget().addItem("test3");

        form.addFormField(formField);
        Assert.assertTrue(form.isValid());

        formField.setValidator(new NotEmptyFieldValidator());
        Assert.assertFalse(form.isValid());

        formField.setValue("test1");
        Assert.assertTrue(form.isValid());

        formField.reset();
        Assert.assertFalse(form.isValid());

        formField.setValue("test1");
        Assert.assertTrue(form.isValid());
    }

}
