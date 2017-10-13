package com.ponysdk.core.ui.basic;

import com.ponysdk.core.ui.form.formfield.StringListBoxFormField;
import com.ponysdk.core.ui.form.validator.NotEmptyFieldValidator;
import org.junit.Assert;
import org.junit.Test;

public class Validator extends PSuite {
    @Test
    public void NotEmptyFieldValidator() {
        StringListBoxFormField formField = new StringListBoxFormField();
        formField.setValidator(new NotEmptyFieldValidator());
        Assert.assertFalse(formField.isValid().isValid());

        formField.getWidget().addItem("Test");
        Assert.assertFalse(formField.isValid().isValid());

        formField.getWidget().setSelectedIndex(0);
        Assert.assertTrue(formField.isValid().isValid());
    }

}
