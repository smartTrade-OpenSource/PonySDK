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

package com.ponysdk.core.ui.form2.impl.formfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form2.impl.validator.NotEmptyFormFieldValidator;
import com.ponysdk.test.PSuite;

import jakarta.json.Json;

public class InputFormFieldTest extends PSuite {

    private static <F extends AbstractInputFormField<?>> F attach(final F field) {
        PWindow.getMain().add(field);
        return field;
    }

    private static void type(final AbstractInputFormField<?> field, final String value) {
        field.input.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), value).build());
    }

    @Test
    public void testValueChangeValidatesAndNotifies() {
        final StringTextBoxFormField field = attach(new StringTextBoxFormField("Name"));
        field.setValidator(new NotEmptyFormFieldValidator("required"));
        final List<String> values = new ArrayList<>();
        final PValueChangeHandler<String> handler = event -> values.add(event.getData());
        field.addValueChangeHandler(handler);

        type(field, "pony");
        assertEquals(List.of("pony"), values);
        assertEquals("pony", field.getValue());
        assertEquals("pony", field.getStringToValidate());
        assertTrue(field.isDirty());
        assertEquals("true", field.asWidget().getAttribute("dirty"));

        assertTrue(field.removeValueChangeHandler(handler));
        assertFalse(field.removeValueChangeHandler(handler));
        type(field, "");
        assertEquals(1, values.size());
        // Blur validates as well
        field.input.onClientData(
            Json.createObjectBuilder().add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.BLUR.getValue()).build());
        assertFalse(field.validate().isValid());
    }

    @Test
    public void testEnablementAndPlaceholder() {
        final StringTextBoxFormField field = attach(new StringTextBoxFormField("Name"));
        assertTrue(field.isEnabled());
        field.disable();
        assertFalse(field.isEnabled());
        assertTrue(field.input.hasAttribute("disabled"));
        field.enable();
        assertTrue(field.isEnabled());

        field.setPlaceHolder("type here");
        assertEquals("type here", field.getPlaceHolder());
        field.focus();
        field.blur();
        assertTrue(field.input.getTabindex() >= 0);
    }

    @Test
    public void testNumberFields() {
        final IntegerInputFormField integer = attach(new IntegerInputFormField("i"));
        assertEquals("number", integer.input.getAttribute("type"));
        assertNull(integer.getValue());
        integer.setValue(12);
        assertEquals(12, integer.getValue());
        integer.setValue(null);
        assertNull(integer.getValue());

        final LongInputFormField longField = attach(new LongInputFormField("l"));
        assertNull(longField.getValue());
        longField.setValue(3L);
        assertEquals(3L, longField.getValue());

        final DoubleInputFormField doubleField = attach(new DoubleInputFormField("d"));
        assertEquals("any", doubleField.input.getAttribute("step"));
        assertNull(doubleField.getValue());
        doubleField.setValue(1.25);
        assertEquals(1.25, doubleField.getValue());

        final FloatInputFormField floatField = attach(new FloatInputFormField("f"));
        assertEquals("any", floatField.input.getAttribute("step"));
        assertNull(floatField.getValue());
        floatField.setValue(0.5f);
        assertEquals(0.5f, floatField.getValue());
    }

    @Test
    public void testDateField() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final DateInputFormField field = attach(new DateInputFormField("date", format));
        assertEquals("date", field.input.getAttribute("type"));
        field.setValue(new Date(0));
        assertEquals("1970-01-01", field.input.getText());
        assertEquals(new Date(0), field.getValue());
        type(field, "garbage");
        assertNull(field.getValue());

        final DateInputFormField defaultFormat = attach(new DateInputFormField("date"));
        type(defaultFormat, "2020-02-03");
        assertEquals(new SimpleDateFormat("yyyy-MM-dd").format(defaultFormat.getValue()), "2020-02-03");
    }

    @Test
    public void testEmailField() {
        final EmailInputFormField field = attach(new EmailInputFormField("mail"));
        assertEquals("email", field.input.getAttribute("type"));
        field.setValue("pony@sdk.com");
        assertEquals("pony@sdk.com", field.getValue());
        assertTrue(field.validate().isValid());
        field.setValue("not a mail");
        assertEquals("Invalid email", field.validate().getErrorMessage());
    }

    @Test
    public void testColorField() {
        final ColorInputFormField field = attach(new ColorInputFormField("color"));
        assertEquals("color", field.input.getAttribute("type"));
        assertNull(field.getValue());
        type(field, "#102030");
        final ColorInputFormField.Color color = field.getValue();
        assertEquals(0x10, color.getRed());
        assertEquals(0x20, color.getGreen());
        assertEquals(0x30, color.getBlue());
        assertEquals(255, color.getAlpha());
        assertTrue(color.toString().endsWith("[r=16,g=32,b=48,a=255]"));

        final ColorInputFormField.Color translucent = new ColorInputFormField.Color(1, 2, 3, 4);
        assertEquals("4010203", translucent.toHexString());
        field.setValue(translucent);
        assertEquals("#010203", field.input.getText());
    }

    @Test
    public void testColorRoundTrip() {
        final ColorInputFormField field = attach(new ColorInputFormField("color"));
        field.setValue(new ColorInputFormField.Color(0x10, 0x20, 0x30, 255));
        assertEquals(0x20, field.getValue().getGreen());
    }
}
