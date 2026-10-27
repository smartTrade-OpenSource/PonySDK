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

package com.ponysdk.core.ui.form.formfield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.form.dataconverter.DataConverter;
import com.ponysdk.core.ui.form.dataconverter.IdentityConverter;
import com.ponysdk.core.ui.form.validator.IntegerFieldValidator;
import com.ponysdk.core.ui.form.validator.NotEmptyFieldValidator;
import com.ponysdk.core.ui.form.validator.ValidationResult;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

import jakarta.json.Json;

public class FormFieldTest extends PSuite {

    private static void typeText(final PObject textBox, final String value) {
        textBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), value).build());
    }

    private static class RecordingListener implements FormFieldListener {

        private int resets;
        private final List<ValidationResult> validations = new ArrayList<>();

        @Override
        public void afterReset(final FormField formField) {
            resets++;
        }

        @Override
        public void afterValidation(final FormField formField, final ValidationResult validationResult) {
            validations.add(validationResult);
        }
    }

    @Test
    public void testTextBoxValidationMarksTheWidget() {
        final StringTextBoxFormField field = new StringTextBoxFormField();
        assertTrue(field.asWidget().hasStyleName(AbstractFormField.CLASS_FORM_FIELD_VALIDATOR));
        assertSame(field.getWidget(), field.asWidget());
        assertSame(field, field.asHasPValue());
        final RecordingListener listener = new RecordingListener();
        field.addFormFieldListener(listener);

        assertTrue(field.isValid().isValid());
        field.setValidator(new NotEmptyFieldValidator("required"));
        assertFalse(field.isValid().isValid());
        assertTrue(field.asWidget().hasStyleName(AbstractFormField.CLASS_INVALID));
        assertEquals("required", field.asWidget().getAttribute(AbstractFormField.DATA_TITLE));
        assertEquals(2, listener.validations.size());

        field.setValue("value");
        assertEquals("value", field.getValue());
        assertTrue(field.isValid().isValid());
        assertFalse(field.asWidget().hasStyleName(AbstractFormField.CLASS_INVALID));
        assertNull(field.asWidget().getAttribute(AbstractFormField.DATA_TITLE));

        // A disabled field is always valid
        field.setValue("");
        assertNull(field.getValue());
        field.setEnabled(false);
        assertFalse(field.getWidget().isEnabled());
        assertTrue(field.isValid().isValid());
        field.setEnabled(true);

        // Without error display the widget is left alone
        field.setShowError(false);
        assertFalse(field.isValid().isValid());
        assertFalse(field.asWidget().hasStyleName(AbstractFormField.CLASS_INVALID));
        field.setShowError(true);

        field.isValid();
        field.reset();
        assertFalse(field.asWidget().hasStyleName(AbstractFormField.CLASS_INVALID));
        assertEquals(1, listener.resets);
    }

    @Test
    public void testDirtyModeAndValueChangeHandlers() {
        final StringTextBoxFormField field = new StringTextBoxFormField(true);
        assertFalse(field.isDirty());
        final List<String> values = new ArrayList<>();
        final com.ponysdk.core.ui.basic.event.PValueChangeHandler<String> handler = event -> values.add(event.getData());
        field.addValueChangeHandler(handler);
        assertEquals(2, field.getValueChangeHandlers().size());

        typeText(field.getWidget(), "typed");
        assertTrue(field.isDirty());
        assertEquals(List.of("typed"), values);

        field.reset();
        assertFalse(field.isDirty());
        assertTrue(field.removeValueChangeHandler(handler));
        typeText(field.getWidget(), "again");
        assertEquals(1, values.size());

        // Destroying the widget drops the dirty mode handler
        field.asWidget().onDestroy();
        assertTrue(field.getValueChangeHandlers().isEmpty());

        final StringTextBoxFormField plain = new StringTextBoxFormField(Element.newPTextBox("init"));
        assertEquals("init", plain.getValue());
        assertFalse(plain.removeValueChangeHandler(handler));
        assertTrue(plain.getValueChangeHandlers().isEmpty());
    }

    @Test
    public void testNumberTextBoxes() {
        final IntegerTextBoxFormField integer = new IntegerTextBoxFormField();
        integer.setValue(42);
        assertEquals(42, integer.getValue());
        integer.setValidator(new IntegerFieldValidator());
        typeText(integer.getWidget(), "x");
        assertFalse(integer.isValid().isValid());
        integer.reset();
        assertNull(integer.getValue());

        final LongTextBoxFormField longField = new LongTextBoxFormField();
        longField.setValue(5L);
        assertEquals(5L, longField.getValue());
        final DoubleTextBoxFormField doubleField = new DoubleTextBoxFormField();
        doubleField.setValue(1.5);
        assertEquals(1.5, doubleField.getValue());
        final FloatTextBoxFormField floatField = new FloatTextBoxFormField();
        floatField.setValue(2.5f);
        assertEquals(2.5f, floatField.getValue());

        final TextBoxFormField<Integer> custom = new TextBoxFormField<>(new com.ponysdk.core.ui.form.dataconverter.IntegerConverter(), true);
        typeText(custom.getWidget(), "7");
        assertTrue(custom.isDirty());
        assertEquals(7, custom.getValue());
        final TextBoxFormField<String> simple = new TextBoxFormField<>(IdentityConverter.STRING);
        simple.setDataProvider(new DataConverter<>() {

            @Override
            public String from(final String t) {
                return t == null ? null : t.toLowerCase();
            }

            @Override
            public String to(final String t) {
                return t == null ? null : t.toUpperCase();
            }
        });
        simple.setValue("AbC");
        assertEquals("abc", simple.getWidget().getText());
        assertEquals("ABC", simple.getValue());
    }

    @Test
    public void testPasswordTextBoxes() {
        final StringPasswordTextBoxFormField password = new StringPasswordTextBoxFormField();
        assertNull(password.getValue());
        password.setValue("secret");
        assertEquals("secret", password.getValue());

        final PasswordTextBoxFormField<Integer> pin = new PasswordTextBoxFormField<>(
            new com.ponysdk.core.ui.form.dataconverter.IntegerConverter());
        pin.setValue(1234);
        assertEquals(1234, pin.getValue());
        final PasswordTextBoxFormField<String> raw = new PasswordTextBoxFormField<>();
        raw.setDataProvider(IdentityConverter.STRING);
        raw.setValue("x");
        assertEquals("x", raw.getValue());
    }

    @Test
    public void testCheckBox() {
        final CheckBoxFormField field = new CheckBoxFormField(true);
        assertFalse(field.getValue());
        final List<Boolean> values = new ArrayList<>();
        field.addValueChangeHandler(event -> values.add(event.getData()));
        PEmulator.valueChange(field.getWidget(), true);
        assertTrue(field.getValue());
        assertTrue(field.isDirty());
        assertEquals(List.of(true), values);
        assertEquals("true", field.getStringValue());

        field.reset();
        assertFalse(field.getValue());
        field.setValue(true);
        assertTrue(field.getWidget().getValue());
        field.setEnabled(false);
        assertFalse(field.getWidget().isEnabled());

        assertFalse(new CheckBoxFormField().isDirty());
        assertFalse(new CheckBoxFormField(Element.newPCheckBox()).getValue());
    }

    @Test
    public void testDateBox() {
        final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final DateBoxFormField field = new DateBoxFormField(Element.newPDateBox(format), true);
        final Date date = new Date(0);
        field.setValue(date);
        assertEquals(date, field.getValue());
        assertEquals("01/01/1970", field.getStringValue());

        final List<Date> values = new ArrayList<>();
        field.addValueChangeHandler(event -> values.add(event.getData()));
        field.getWidget().onClientData(
            Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), "02/01/1970").build());
        assertEquals(List.of(new Date(86_400_000L)), values);
        assertTrue(field.isDirty());

        field.reset();
        assertNull(field.getValue());
        field.setEnabled(false);
        assertFalse(field.getWidget().isEnabled());

        assertEquals("yyyy", new DateBoxFormField("yyyy").getWidget().getDateFormat().toPattern());
        assertEquals("MM", new DateBoxFormField(new SimpleDateFormat("MM")).getWidget().getDateFormat().toPattern());
        assertTrue(new DateBoxFormField("yyyy", true).getValueChangeHandlers().size() == 1);
        assertFalse(new DateBoxFormField().isDirty());
        final PDateBox dateBox = Element.newPDateBox();
        assertSame(dateBox, new DateBoxFormField(dateBox).getWidget());
    }

    @Test
    public void testDateBoxDirtyModeWithFormat() {
        final DateBoxFormField field = new DateBoxFormField(new SimpleDateFormat("yyyy"), true);
        assertEquals(1, field.getValueChangeHandlers().size());
    }

    @Test
    public void testListBox() {
        final Map<String, Integer> data = new LinkedHashMap<>();
        data.put("one", 1);
        data.put("two", 2);
        final ListBoxFormField<Integer> field = new ListBoxFormField<>(data);
        assertEquals(2, field.getWidget().getItemCount());
        assertNull(field.getValue());
        field.setValue(2);
        assertEquals(2, field.getValue());
        assertEquals("two", field.getStringValue());

        final List<Integer> values = new ArrayList<>();
        field.addValueChangeHandler(event -> values.add(event.getData()));
        field.getWidget().onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "0").build());
        assertEquals(List.of(1), values);

        field.reset();
        assertEquals(-1, field.getWidget().getSelectedIndex());
        field.setEnabled(false);
        assertFalse(field.getWidget().isEnabled());

        final StringListBoxFormField strings = new StringListBoxFormField();
        strings.getWidget().addItem("a");
        strings.getWidget().addItem("b");
        strings.setValue("b");
        assertEquals("b", strings.getValue());
        final PListBox listBox = Element.newPListBox();
        assertSame(listBox, new StringListBoxFormField(listBox).getWidget());

        final ListBoxFormField<String> dirty = new ListBoxFormField<>(true);
        dirty.getWidget().addItem("x");
        dirty.getWidget().onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "0").build());
        assertTrue(dirty.isDirty());
        assertFalse(new ListBoxFormField<String>().isDirty());
        assertFalse(new ListBoxFormField<>(IdentityConverter.STRING).isDirty());
        assertFalse(new ListBoxFormField<>(Element.newPListBox()).isDirty());
    }

    @Test
    public void testContainerFormField() {
        final StringTextBoxFormField inner = new StringTextBoxFormField();
        final PFlowPanel panel = Element.newPFlowPanel();
        final ContainerFormField<String> container = new ContainerFormField<>(inner, panel);
        assertFalse(inner.asWidget().hasStyleName(AbstractFormField.CLASS_FORM_FIELD_VALIDATOR));
        assertTrue(panel.hasStyleName(AbstractFormField.CLASS_FORM_FIELD_VALIDATOR));
        assertSame(inner, container.asHasPValue());

        final RecordingListener listener = new RecordingListener();
        container.addFormFieldListener(listener);
        final List<String> values = new ArrayList<>();
        container.addValueChangeHandler(event -> values.add(event.getData()));

        container.setValue("v");
        assertEquals("v", container.getValue());
        assertEquals("v", container.getStringValue());
        typeText(inner.getWidget(), "typed");
        assertEquals(List.of("typed"), values);

        container.setValidator(new NotEmptyFieldValidator("empty"));
        inner.setValue("");
        assertFalse(container.isValid().isValid());
        // The error is shown on the container, not on the inner field
        assertTrue(panel.hasStyleName(AbstractFormField.CLASS_INVALID));
        assertEquals("empty", panel.getAttribute(AbstractFormField.DATA_TITLE));
        assertFalse(inner.asWidget().hasStyleName(AbstractFormField.CLASS_INVALID));
        assertEquals(1, listener.validations.size());

        inner.setValue("ok");
        assertTrue(container.isValid().isValid());
        assertFalse(panel.hasStyleName(AbstractFormField.CLASS_INVALID));

        inner.setValue("");
        container.isValid();
        container.reset();
        assertFalse(panel.hasStyleName(AbstractFormField.CLASS_INVALID));
        assertEquals(1, listener.resets);
        assertFalse(container.isDirty());

        container.setEnabled(false);
        assertFalse(inner.getWidget().isEnabled());
        container.setDataProvider(IdentityConverter.STRING);
    }

    @Test
    public void testFieldWithoutWidgetShowsNoError() {
        final AbstractFormField<String, PTextBox> field = new AbstractFormField<>() {

            private String value;

            @Override
            protected String getStringValue() {
                return value;
            }

            @Override
            protected void reset0() {
                value = null;
            }

            @Override
            public String getValue() {
                return value;
            }

            @Override
            public void setValue(final String value) {
                this.value = value;
                fireValueChange(value);
            }
        };
        assertNull(field.getWidget());
        // no handler yet: firing is a no-op
        field.setValue("a");
        field.setValidator(new NotEmptyFieldValidator("empty"));
        field.setValue("");
        final RecordingListener first = new RecordingListener();
        final RecordingListener second = new RecordingListener();
        field.addFormFieldListener(first);
        field.addFormFieldListener(second);
        assertFalse(field.isValid().isValid());
        field.reset();
        assertEquals(1, first.validations.size());
        assertEquals(1, second.resets);
        assertNull(field.getValue());
    }

    @Test
    public void testRemoveUnknownValueChangeHandler() {
        final StringTextBoxFormField field = new StringTextBoxFormField();
        field.addValueChangeHandler(event -> {});
        assertFalse(field.removeValueChangeHandler(event -> {}));
        assertEquals(1, field.getValueChangeHandlers().size());
    }

    @Test
    public void testEmptyStringFieldsHaveNoValue() {
        final StringTextBoxFormField text = new StringTextBoxFormField();
        assertNull(text.getValue());
        typeText(text.getWidget(), "");
        assertNull(text.getValue());

        final StringPasswordTextBoxFormField password = new StringPasswordTextBoxFormField();
        password.setValue("");
        assertNull(password.getValue());
        password.setValue(null);
        assertNull(password.getValue());
        text.setValue(null);
        assertNull(text.getValue());
    }

    @Test
    public void testListBoxListensToTheWidgetOnce() {
        final ListBoxFormField<String> field = new ListBoxFormField<>(true);
        field.getWidget().addItem("x");
        final List<String> first = new ArrayList<>();
        final List<String> second = new ArrayList<>();
        field.addValueChangeHandler(event -> first.add(event.getData()));
        field.addValueChangeHandler(event -> second.add(event.getData()));
        field.getWidget().onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "0").build());
        assertEquals(List.of("x"), first);
        assertEquals(List.of("x"), second);
    }

    @Test
    public void testDateBoxWithConverter() {
        final PDateBox dateBox = Element.newPDateBox();
        final DateBoxFormField field = new DateBoxFormField(dateBox, new com.ponysdk.core.ui.form.dataconverter.DateConverter());
        assertSame(dateBox, field.getWidget());
        assertFalse(field.isDirty());
        assertTrue(field.getValueChangeHandlers().isEmpty());
    }

    @Test
    public void testDestroyWithoutDirtyModeKeepsTheHandlers() {
        final StringTextBoxFormField plainField = new StringTextBoxFormField();
        plainField.addValueChangeHandler(event -> {});
        plainField.asWidget().onDestroy();
        assertEquals(1, plainField.getValueChangeHandlers().size());
    }
}
