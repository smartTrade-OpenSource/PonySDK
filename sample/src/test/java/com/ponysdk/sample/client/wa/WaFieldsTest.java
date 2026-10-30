/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.sample.client.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ProtocolVersion;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.binder.Binder;
import com.ponysdk.core.ui.binder.Converters;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;
import com.ponysdk.test.PSuite;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

/**
 * {@link WaFields} (docs/decisions/0028-web-awesome.md): a control reports its value on {@code change}, the field gives it
 * to the Binder, and what the server writes is sent every time.
 */
class WaFieldsTest extends PSuite {

    /** The (model, value) pairs written, in order. */
    private static final class Recorder extends ModelWriterForTest {

        final List<Object[]> writes = new ArrayList<>();

        @Override
        public void write(final ServerToClientModel model, final Object value) {
            writes.add(new Object[] { model, value });
            super.write(model, value);
        }

        /** The values of the property {@code name} written after WC_SET_PROPERTY, in order. */
        List<Object> propertyValues(final String name) {
            final List<Object> values = new ArrayList<>();
            for (int i = 0; i < writes.size() - 1; i++) {
                if (writes.get(i)[0] == ServerToClientModel.WC_SET_PROPERTY && name.equals(writes.get(i)[1])
                        && writes.get(i + 1)[0] == ServerToClientModel.WC_PROPERTY_VALUE) values.add(writes.get(i + 1)[1]);
            }
            return values;
        }
    }

    private Recorder recorder;
    private ModelWriter previous;

    @BeforeEach
    void record() {
        recorder = new Recorder();
        previous = UIContext.get().getWriter();
        Mockito.doReturn(recorder).when(UIContext.get()).getWriter();
        Mockito.doReturn(ProtocolVersion.LISTENERS).when(UIContext.get()).getProtocolVersion();
    }

    @AfterEach
    void restore() {
        Mockito.doReturn(previous).when(UIContext.get()).getWriter();
        Mockito.doCallRealMethod().when(UIContext.get()).getProtocolVersion();
    }

    private static <W extends PWebComponent> W attached(final W element) {
        PWindow.getMain().add(element);
        return element;
    }

    /** What the terminal sends when the control fires {@code change}: the fields the listener asked for. */
    private static void change(final PWebComponent element, final String path, final Object value) {
        final JsonObjectBuilder fields = Json.createObjectBuilder();
        switch (value) {
            case final String s -> fields.add(path, s);
            case final Boolean b -> fields.add(path, b);
            case final Number n -> fields.add(path, n.doubleValue());
            default -> throw new IllegalArgumentException();
        }
        element.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), element.getID())
            .add(ClientToServerModel.EVENT.toStringValue(), "change")
            .add(ClientToServerModel.EVENT_FIELDS.toStringValue(), fields)
            .build());
    }

    @Test
    void aTextControlGivesWhatTheUserTypedOnChange() {
        final PWaInput input = attached(new PWaInput());
        final HasPValue<String> field = WaFields.of(input);
        final List<String> seen = new ArrayList<>();
        field.addValueChangeHandler(e -> seen.add(e.getData()));
        assertNull(field.getValue(), "nothing was typed or written");

        change(input, "target.value", "EURUSD");

        assertEquals("EURUSD", field.getValue());
        assertEquals(List.of("EURUSD"), seen);
    }

    @Test
    void aHandlerRemovedHearsNoMore() {
        final PWaTextarea textarea = attached(new PWaTextarea());
        final HasPValue<String> field = WaFields.of(textarea);
        final List<String> seen = new ArrayList<>();
        final com.ponysdk.core.ui.basic.event.PValueChangeHandler<String> handler = e -> seen.add(e.getData());
        field.addValueChangeHandler(handler);
        assertEquals(List.of(handler), List.copyOf(field.getValueChangeHandlers()));

        assertTrue(field.removeValueChangeHandler(handler));
        change(textarea, "target.value", "a comment");

        assertEquals(List.of(), seen);
        assertEquals("a comment", field.getValue(), "the value is still read");
    }

    @Test
    void whatTheServerWritesIsSentEveryTimeEvenWhenItIsTheLastWrittenValue() {
        final PWaInput input = attached(new PWaInput());
        final HasPValue<String> field = WaFields.of(input);
        recorder.writes.clear();

        field.setValue("EURUSD");
        change(input, "target.value", "GBPUSD"); // the user changed it in the browser: the server never wrote that
        field.setValue("EURUSD"); // a reset to the same text must reach the browser

        assertEquals(List.of("\"EURUSD\"", "\"EURUSD\""), recorder.propertyValues("value"));
        assertEquals("EURUSD", field.getValue());
        assertNull(input.property("value").get(), "stateless: the element keeps no copy to compare with");
    }

    @Test
    void aNullTextIsAnEmptyText() {
        final PWaSelect select = attached(new PWaSelect());
        final HasPValue<String> field = WaFields.of(select);
        recorder.writes.clear();

        field.setValue(null);

        assertEquals(List.of("\"\""), recorder.propertyValues("value"));
    }

    @Test
    void aTextWithQuotesIsEscaped() {
        final PWaColorPicker picker = attached(new PWaColorPicker());
        recorder.writes.clear();

        WaFields.of(picker).setValue("a\"b\\c");

        assertEquals(List.of("\"a\\\"b\\\\c\""), recorder.propertyValues("value"));
    }

    @Test
    void aCheckboxAndASwitchGiveTheirCheckedState() {
        final PWaCheckbox checkbox = attached(new PWaCheckbox());
        final PWaSwitch toggle = attached(new PWaSwitch());
        final HasPValue<Boolean> box = WaFields.of(checkbox);
        final HasPValue<Boolean> flip = WaFields.of(toggle);
        recorder.writes.clear();

        change(checkbox, "target.checked", true);
        change(toggle, "target.checked", false);
        assertEquals(Boolean.TRUE, box.getValue());
        assertEquals(Boolean.FALSE, flip.getValue());

        box.setValue(null);
        flip.setValue(true);
        assertNull(box.getValue());
        assertEquals(List.of("false", "true"), recorder.propertyValues("checked"));
    }

    @Test
    void aSliderAndARatingGiveANumber() {
        final PWaSlider slider = attached(new PWaSlider());
        final PWaRating rating = attached(new PWaRating());
        final HasPValue<Double> level = WaFields.of(slider);
        final HasPValue<Double> stars = WaFields.of(rating);
        recorder.writes.clear();

        change(slider, "target.value", 62.5);
        change(rating, "target.value", 4);
        assertEquals(62.5, level.getValue());
        assertEquals(4.0, stars.getValue());

        level.setValue(40.0);
        level.setValue(0.5);
        stars.setValue(null);
        assertEquals(List.of("40", "0.5"), recorder.propertyValues("value").subList(0, 2));
        assertEquals("0", recorder.propertyValues("value").get(2), "no number: zero");
    }

    @Test
    void theRadioGroupGivesItsValue() {
        final PWaRadioGroup group = attached(new PWaRadioGroup());
        final HasPValue<String> field = WaFields.of(group);

        change(group, "target.value", "LIMIT");

        assertEquals("LIMIT", field.getValue());
    }

    @Test
    void theFieldIsTheElementForTheBindersDisplay() {
        final PWaInput input = attached(new PWaInput());

        assertTrue(WaFields.of(input) instanceof com.ponysdk.core.ui.basic.IsPWidget widget && widget.asWidget() == input);
    }

    // ---- With the Binder ----

    /** A bean with a number, a text and a flag. */
    public static final class Ticket {

        private String instrument;
        private Integer quantity;
        private boolean confirmed;

        public String getInstrument() { return instrument; }
        public void setInstrument(final String instrument) { this.instrument = instrument; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(final Integer quantity) { this.quantity = quantity; }
        public boolean isConfirmed() { return confirmed; }
        public void setConfirmed(final boolean confirmed) { this.confirmed = confirmed; }
    }

    @Test
    void theBinderConvertsValidatesAndWritesTheBeanFromWebAwesomeControls() {
        final PFlowPanel panel = com.ponysdk.core.ui.basic.Element.newPFlowPanel();
        PWindow.getMain().add(panel);
        final PWaInput instrument = new PWaInput();
        final PWaInput quantity = new PWaInput();
        final PWaCheckbox confirmed = new PWaCheckbox();
        panel.add(instrument, quantity, confirmed);

        final Binder<Ticket> binder = new Binder<>();
        binder.forField(WaFields.of(instrument)).withValidator(v -> v != null && v.matches("[A-Z]{6}"), "Six capital letters")
            .bind(Ticket::getInstrument, Ticket::setInstrument);
        final HasPValue<String> quantityField = WaFields.of(quantity);
        binder.forField(quantityField).withConverter(Converters.toInteger("Not a whole number"))
            .bind(Ticket::getQuantity, Ticket::setQuantity);
        binder.forField(WaFields.of(confirmed)).bind(Ticket::isConfirmed, Ticket::setConfirmed);
        final Ticket ticket = new Ticket();
        ticket.setInstrument("EURUSD");
        ticket.setQuantity(1000);
        binder.readBean(ticket);
        assertEquals("1000", quantityField.getValue(), "read into the field");

        change(instrument, "target.value", "abc");
        change(quantity, "target.value", "12x");
        change(confirmed, "target.checked", true);

        // each invalid field says so on its element, as on a native widget (FieldDisplay): the library's styling reads it
        assertEquals("true", instrument.getAttribute("aria-invalid"));
        assertEquals("true", quantity.getAttribute("aria-invalid"));
        assertFalse(binder.writeBean(ticket), "nothing is written while a field is not valid");
        assertEquals("EURUSD", ticket.getInstrument());

        change(instrument, "target.value", "GBPUSD");
        change(quantity, "target.value", "250");
        assertNull(instrument.getAttribute("aria-invalid"), "a valid field loses the mark");
        assertNull(quantity.getAttribute("aria-invalid"));
        assertTrue(binder.writeBean(ticket));
        assertEquals("GBPUSD", ticket.getInstrument());
        assertEquals(250, ticket.getQuantity());
        assertTrue(ticket.isConfirmed());
    }
}
