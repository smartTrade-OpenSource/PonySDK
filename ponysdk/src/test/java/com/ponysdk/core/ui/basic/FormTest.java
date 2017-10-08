package com.ponysdk.core.ui.basic;

import com.ponysdk.core.ui.form.Form;
import com.ponysdk.core.ui.form.formfield.StringListBoxFormField;
import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.form.validator.NotEmptyFieldValidator;
import org.junit.Assert;
import org.junit.Test;

public class FormTest extends PSuite {
    @Test
    public void testForm1() {
        Form form = new Form();

        StringTextBoxFormField formField = new StringTextBoxFormField();
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
        Form form = new Form();

        StringListBoxFormField formField = new StringListBoxFormField();
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
