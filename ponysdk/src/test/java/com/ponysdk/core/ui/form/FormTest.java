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

package com.ponysdk.core.ui.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.form.FormFieldComponent.CaptionOrientation;
import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.form.validator.NotEmptyFieldValidator;
import com.ponysdk.test.PSuite;

public class FormTest extends PSuite {

    private static StringTextBoxFormField requiredField() {
        final StringTextBoxFormField field = new StringTextBoxFormField();
        field.setValidator(new NotEmptyFieldValidator("required"));
        return field;
    }

    @Test
    public void testForm() {
        final Form form = new Form();
        assertTrue(form.isValid());

        final StringTextBoxFormField first = requiredField();
        final StringTextBoxFormField second = requiredField();
        form.addFormField(first);
        form.addFormField(second);
        assertEquals(2, form.getFormFields().size());
        assertFalse(form.isValid());

        first.setValue("a");
        assertFalse(form.isValid());
        second.setValue("b");
        assertTrue(form.isValid());

        form.reset();
        assertNull(first.getValue());
        assertNull(second.getValue());

        form.removeFormField(second);
        first.setValue("a");
        assertTrue(form.isValid());
        assertEquals(1, form.getFormFields().size());
    }

    @Test
    public void testComponentCaption() {
        final StringTextBoxFormField field = requiredField();
        final FormFieldComponent component = new FormFieldComponent("Name", field);
        assertSame(field, component.getFormField());
        assertTrue(component.hasStyleName("pony-Form-FormField-Component"));
        assertTrue(component.hasStyleName("TOP"));

        // Caption first, then the container with the field and the error label
        final PLabel caption = (PLabel) component.getWidget(0);
        assertEquals("Name", caption.getText());
        assertTrue(caption.isVisible());
        assertSame(field.asWidget(), ((com.ponysdk.core.ui.basic.PFlowPanel) component.getWidget(1)).getWidget(0));

        component.setCaptionOrientation(CaptionOrientation.BOTTOM);
        assertFalse(component.hasStyleName("TOP"));
        assertTrue(component.hasStyleName("BOTTOM"));
        assertSame(caption, component.getWidget(1));
        component.setCaptionOrientation(CaptionOrientation.RIGHT);
        assertSame(caption, component.getWidget(1));
        component.setCaptionOrientation(CaptionOrientation.LEFT);
        assertSame(caption, component.getWidget(0));
        component.setCaptionOrientation(CaptionOrientation.TOP);
        assertSame(caption, component.getWidget(0));

        component.setCaption(null);
        assertFalse(caption.isVisible());
        assertNull(caption.getText());

        final FormFieldComponent noCaption = new FormFieldComponent(new StringTextBoxFormField());
        assertFalse(((PWidget) noCaption.getWidget(0)).isVisible());
        final FormFieldComponent left = new FormFieldComponent("L", CaptionOrientation.LEFT, new StringTextBoxFormField());
        assertTrue(left.hasStyleName("LEFT"));
    }

    @Test
    public void testComponentShowsErrors() {
        final StringTextBoxFormField field = requiredField();
        final FormFieldComponent component = new FormFieldComponent("Name", field);
        final PLabel errorLabel = (PLabel) ((com.ponysdk.core.ui.basic.PFlowPanel) component.getWidget(1)).getWidget(1);
        assertTrue(errorLabel.hasStyleName("error-label"));

        assertFalse(component.isValid().isValid());
        assertTrue(component.hasStyleName("error"));
        assertEquals("required", errorLabel.getText());

        field.setValue("x");
        assertTrue(component.isValid().isValid());
        assertFalse(component.hasStyleName("error"));
        assertEquals("", errorLabel.getText());

        field.setValue("");
        component.isValid();
        component.reset();
        assertFalse(component.hasStyleName("error"));
        assertEquals("", errorLabel.getText());
        assertNull(field.getValue());
    }
}
