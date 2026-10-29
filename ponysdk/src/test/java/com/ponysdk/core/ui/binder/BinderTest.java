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

package com.ponysdk.core.ui.binder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.HasPValue;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.binder.Binder.Binding;
import com.ponysdk.core.ui.binder.Ticket.Kind;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.listbox.ListBox;
import com.ponysdk.core.ui.listbox.ListBox.ListBoxItem;
import com.ponysdk.core.ui.listbox.ListBoxConfiguration;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;

/** The binder without Bean Validation: converters, validators, buffered and unbuffered, status, what the field shows. */
public class BinderTest extends PSuite {

    private static final String CONVERSION = "Not a whole number";

    private final Binder<Ticket> binder = new Binder<>();
    private final PFlowPanel panel = Element.newPFlowPanel();
    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    static void type(final PObject box, final String text) {
        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), text).build());
    }

    static void choose(final PObject box, final int index) {
        box.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), String.valueOf(index)).build());
    }

    private static Ticket ticket(final String instrument, final Integer quantity) {
        final Ticket ticket = new Ticket();
        ticket.setInstrument(instrument);
        ticket.setQuantity(quantity);
        return ticket;
    }

    private PTextBox quantityBox() {
        final PTextBox box = Element.newPTextBox();
        panel.add(box);
        return box;
    }

    private Binding<Ticket, Integer> bindQuantity(final PTextBox box) {
        return binder.forField(box).withConverter(Converters.toInteger(CONVERSION)).bind(Ticket::getQuantity, Ticket::setQuantity);
    }

    private static PElement message(final PComplexPanel parent, final int index) {
        return (PElement) parent.getWidget(index);
    }

    // ---- what a field shows -----------------------------------------------------------------------------------

    @Test
    public void aConversionErrorShowsOnTheFieldAndClearsWhenTheTextIsRight() {
        final PTextBox box = quantityBox();
        bindQuantity(box);
        assertEquals(1, panel.getWidgetCount()); // nothing is added until there is an error

        type(box, "abc");
        assertEquals("true", box.getAttribute("aria-invalid"));
        assertEquals(2, panel.getWidgetCount());
        final PElement message = message(panel, 1); // after the field
        assertEquals(CONVERSION, message.getInnerText());
        assertEquals("alert", message.getAttribute("role"));
        assertTrue(message.hasStyleName("pony-FieldError"));
        assertTrue(message.getAttribute("id").startsWith("pony-error-"));
        assertEquals(message.getAttribute("id"), box.getAttribute("aria-describedby"));
        assertTrue(message.isVisible());

        type(box, "12");
        assertNull(box.getAttribute("aria-invalid"));
        assertNull(box.getAttribute("aria-describedby"));
        assertFalse(message.isVisible());

        type(box, "x"); // the same element again, with the new text
        assertEquals(2, panel.getWidgetCount());
        assertTrue(message.isVisible());
        assertEquals(message.getAttribute("id"), box.getAttribute("aria-describedby"));
    }

    @Test
    public void theMessageIsPlacedRightAfterItsFieldWhateverFollows() {
        final PTextBox first = Element.newPTextBox();
        final PTextBox second = Element.newPTextBox();
        panel.add(first);
        panel.add(second);
        bindQuantity(first);

        type(first, "x");
        assertSame(first, panel.getWidget(0));
        assertEquals(CONVERSION, message(panel, 1).getInnerText());
        assertSame(second, panel.getWidget(2));
    }

    @Test
    public void aFieldThatCannotHoldAMessageStillSaysItIsInvalid() {
        final PSimplePanel holder = Element.newPSimplePanel(); // takes one widget: nowhere to put a message
        final PTextBox inside = Element.newPTextBox();
        holder.setWidget(inside);
        final PTextBox alone = Element.newPTextBox(); // no parent at all
        bindQuantity(inside);
        binder.forField(alone).withConverter(Converters.toInteger(CONVERSION)).bind(Ticket::getQuantity, Ticket::setQuantity);

        type(inside, "x");
        type(alone, "x");
        for (final PTextBox box : List.of(inside, alone)) {
            assertEquals("true", box.getAttribute("aria-invalid"));
            assertNull(box.getAttribute("aria-describedby"));
        }
        type(inside, "1");
        type(alone, "1");
        assertNull(inside.getAttribute("aria-invalid"));
        assertNull(alone.getAttribute("aria-invalid"));
    }

    @Test
    public void aRequiredCheckBoxIsMarkedOnItsInput() {
        final PCheckBox box = Element.newPCheckBox("I agree");
        panel.add(box);
        PWindow.getMain().add(panel);
        writer.clear();
        binder.forField(box).asRequired("Tick it").bind(Ticket::isConfirmed, Ticket::setConfirmed);
        assertEquals(List.of("true"), writer.valuesAfter(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY, "aria-required"));
        assertNull(box.getAttribute("aria-required")); // not on the label, the root of a check box
    }

    @Test
    public void aCheckBoxsErrorIsOnItsInputAndLeavesWithIt() {
        final PCheckBox box = Element.newPCheckBox("I agree");
        panel.add(box);
        PWindow.getMain().add(panel);
        binder.forField(box).withValidator(Boolean::booleanValue, "Tick it").bind(Ticket::isConfirmed, Ticket::setConfirmed);
        writer.clear();

        PEmulator.valueChange(box, true);
        PEmulator.valueChange(box, false);
        assertEquals(List.of("true"), writer.valuesAfter(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY, "aria-invalid"));
        assertEquals(1, writer.count(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY) - 1); // + the describedby
        assertNull(box.getAttribute("aria-invalid")); // not on the label, the root of a check box
        PEmulator.valueChange(box, true);
        assertEquals(List.of("aria-describedby", "aria-invalid"),
            writer.valuesOf(ServerToClientModel.REMOVE_INPUT_ATTRIBUTE_KEY).stream().sorted().map(Object::toString).toList());
        assertEquals("Tick it", message(panel, 1).getInnerText());
    }

    @Test
    public void aDateBoxIsToldAgainItsErrorSinceItsTerminalDropsTheAttributeWhenItParsesTheText() {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final PDateBox box = Element.newPDateBox(format);
        panel.add(box);
        PWindow.getMain().add(panel);
        binder.forField(box).withConverter(Converters.dateToLocalDate(ZoneOffset.UTC))
            .withValidator(d -> d != null && d.getYear() > 2000, "Too old").bind(Ticket::getDate, Ticket::setDate);
        writer.clear();

        type(box, "1999-01-01");
        type(box, "1998-01-01"); // still invalid, and the terminal removed the attribute between the two
        assertEquals("true", box.getAttribute("aria-invalid"));
        assertEquals(1, writer.count(ServerToClientModel.REMOVE_ATTRIBUTE_KEY)); // the second time only
        assertEquals(List.of("true", "true"), writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "aria-invalid"));
        type(box, "2026-10-29");
        assertNull(box.getAttribute("aria-invalid"));
    }

    // ---- converters and validators ----------------------------------------------------------------------------

    @Test
    public void validatorsRunOnTheConvertedValueInOrderAndTheFirstErrorWins() {
        final PTextBox box = quantityBox();
        final List<String> calls = new ArrayList<>();
        binder.forField(box).withConverter(Converters.toInteger(CONVERSION))
            .withValidator(v -> {
                calls.add("first");
                return v == null || v > 0 ? Result.ok(v) : Result.error("Positive");
            })
            .withValidator(v -> {
                calls.add("second");
                return v == null || v % 2 == 0 ? Result.ok(v) : Result.error("Even");
            })
            .bind(Ticket::getQuantity, Ticket::setQuantity);

        type(box, "abc");
        assertEquals(CONVERSION, message(panel, 1).getInnerText());
        assertEquals(List.of(), calls); // no validator on what did not convert
        type(box, "-3");
        assertEquals("Positive", message(panel, 1).getInnerText());
        assertEquals(List.of("first"), calls);
        type(box, "3");
        assertEquals("Even", message(panel, 1).getInnerText());
        type(box, "4");
        assertNull(box.getAttribute("aria-invalid"));
        type(box, ""); // empty: null reaches the validators
        assertNull(box.getAttribute("aria-invalid"));
    }

    @Test
    public void convertersChainAndTheWayBackGoesThroughAllOfThem() {
        final PTextBox box = quantityBox();
        binder.forField(box).withConverter(Converters.toInteger(CONVERSION))
            .withConverter(Converter.of(lots -> lots * 100, units -> units / 100, "Overflow")) // lots of 100
            .bind(Ticket::getQuantity, Ticket::setQuantity);
        final Ticket ticket = ticket("EUR", 700);
        binder.readBean(ticket);
        assertEquals("7", box.getText());
        type(box, "5");
        assertTrue(binder.writeBean(ticket));
        assertEquals(500, ticket.getQuantity());
    }

    @Test
    public void aRequiredFieldIsMarkedAndRefusesAnEmptyOrBlankText() {
        final PTextBox box = quantityBox();
        binder.forField(box).asRequired("Required").withConverter(Converters.toInteger(CONVERSION))
            .bind(Ticket::getQuantity, Ticket::setQuantity);
        assertEquals("true", box.getAttribute("aria-required"));

        type(box, "  ");
        assertEquals("Required", message(panel, 1).getInnerText());
        type(box, "3");
        assertNull(box.getAttribute("aria-invalid"));

        // after the converter, it is the missing value that is refused
        final PTextBox other = quantityBox();
        binder.forField(other).withConverter(Converters.toInteger(CONVERSION)).asRequired("Needed")
            .bind(Ticket::getQuantity, Ticket::setQuantity);
        type(other, "");
        assertEquals("Needed", message(panel, 3).getInnerText());
        type(other, "7"); // a value that is no text
        assertNull(other.getAttribute("aria-invalid"));

        // a field that is no widget has nothing to mark
        final EagerField bare = new EagerField();
        binder.forField(bare).asRequired("Required").bind(Ticket::getInstrument, Ticket::setInstrument);
        bare.setValueFromUser(" ");
        assertFalse(binder.getStatus().valid());
    }

    @Test
    public void aPredicateAndItsMessageIsAValidator() {
        final PTextBox box = quantityBox();
        binder.forField(box).withValidator(s -> s.length() <= 3, "Too long").bind(Ticket::getInstrument, Ticket::setInstrument);
        type(box, "abcd");
        assertEquals("Too long", message(panel, 1).getInnerText());
    }

    // ---- buffered ---------------------------------------------------------------------------------------------

    @Test
    public void readBeanShowsTheBeanWithoutAnyError() {
        final PTextBox instrument = Element.newPTextBox();
        final PTextBox quantity = quantityBox();
        binder.forField(instrument).asRequired("Required").bind(Ticket::getInstrument, Ticket::setInstrument);
        bindQuantity(quantity);

        binder.readBean(ticket("EURUSD", 25));
        assertEquals("EURUSD", instrument.getText());
        assertEquals("25", quantity.getText());
        assertNull(quantity.getAttribute("aria-invalid"));

        binder.readBean(null); // an empty form
        assertEquals("", instrument.getText());
        assertEquals("", quantity.getText());
        assertNull(binder.getBean());
    }

    @Test
    public void aFormThatHoldsInvalidValuesWhenReadIsInvalidButShowsNoErrorYet() {
        final PTextBox instrument = Element.newPTextBox();
        binder.forField(instrument).asRequired("Required").bind(Ticket::getInstrument, Ticket::setInstrument);

        binder.readBean(new Ticket());
        assertFalse(binder.getStatus().valid());
        assertNull(instrument.getAttribute("aria-invalid"));
        assertFalse(binder.validate()); // now it says so
        assertEquals("true", instrument.getAttribute("aria-invalid"));
    }

    @Test
    public void writeBeanWritesEverythingOrNothing() {
        final PTextBox instrument = Element.newPTextBox();
        final PTextBox quantity = quantityBox();
        panel.add(instrument);
        binder.forField(instrument).asRequired("Required").bind(Ticket::getInstrument, Ticket::setInstrument);
        bindQuantity(quantity);
        final Ticket ticket = ticket("EURUSD", 25);
        binder.readBean(ticket);

        type(instrument, "GBPUSD");
        type(quantity, "abc");
        assertFalse(binder.writeBean(ticket));
        assertEquals("EURUSD", ticket.getInstrument()); // the valid field was not written either
        assertEquals(25, ticket.getQuantity());

        type(instrument, "");
        assertFalse(binder.writeBean(ticket));
        assertEquals("true", instrument.getAttribute("aria-invalid")); // every invalid field says so
        assertEquals("true", quantity.getAttribute("aria-invalid"));

        type(instrument, "GBPUSD");
        type(quantity, "30");
        assertTrue(binder.writeBean(ticket));
        assertEquals("GBPUSD", ticket.getInstrument());
        assertEquals(30, ticket.getQuantity());
        assertNull(quantity.getAttribute("aria-invalid"));
    }

    @Test
    public void aReadOnlyBindingIsShownButNeverWritten() {
        final PTextBox box = quantityBox();
        binder.forField(box).bind(Ticket::getInstrument, null);
        final Ticket ticket = ticket("EURUSD", 1);
        binder.readBean(ticket);
        assertEquals("EURUSD", box.getText());
        type(box, "changed");
        assertTrue(binder.writeBean(ticket));
        assertEquals("EURUSD", ticket.getInstrument());

        binder.withValidator(t -> Result.error("Always wrong")); // the revert has nothing to put back
        assertFalse(binder.writeBean(ticket));
        assertEquals("EURUSD", ticket.getInstrument());
    }

    @Test
    public void aBeanValidatorFailingPutsTheBeanBackAndShowsItsMessage() {
        final PTextBox instrument = Element.newPTextBox();
        final PTextBox quantity = quantityBox();
        binder.forField(instrument).bind(Ticket::getInstrument, Ticket::setInstrument);
        bindQuantity(quantity);
        binder.withValidator(t -> t.getQuantity() != null && t.getQuantity() > 100 && "EUR".equals(t.getInstrument())
            ? Result.error("EUR is limited to 100") : Result.ok(t))
            .withValidator(t -> Result.ok(t));
        final Ticket ticket = ticket("USD", 10);
        binder.readBean(ticket);

        type(instrument, "EUR");
        type(quantity, "500");
        assertFalse(binder.writeBean(ticket));
        assertEquals("USD", ticket.getInstrument());
        assertEquals(10, ticket.getQuantity());
        assertEquals(List.of("EUR is limited to 100"), binder.getStatus().beanErrors());
        assertFalse(binder.getStatus().valid());
        assertNull(quantity.getAttribute("aria-invalid")); // the bean's error is not a field's

        type(quantity, "50"); // an edit makes the last write's errors stale
        assertEquals(List.of(), binder.getStatus().beanErrors());
        assertTrue(binder.writeBean(ticket));
        assertEquals(50, ticket.getQuantity());
    }

    @Test
    public void validateChecksTheFieldsOfABufferedBinderAndClearsTheBeansErrors() {
        final PTextBox box = quantityBox();
        bindQuantity(box);
        binder.readBean(ticket("A", 1));
        type(box, "x");
        type(box, "2");
        assertTrue(binder.validate());
        type(box, "x");
        assertFalse(binder.validate());
        assertEquals("true", box.getAttribute("aria-invalid"));
    }

    // ---- status -----------------------------------------------------------------------------------------------

    @Test
    public void theStatusTellsChangesAndValidityAndListenersHearIt() {
        final PTextBox box = quantityBox();
        bindQuantity(box);
        final List<Binder.Status> heard = new ArrayList<>();
        final HandlerRegistration registration = binder.addStatusChangeListener(heard::add);
        final Ticket ticket = ticket("A", 5);

        binder.readBean(ticket);
        assertEquals(new Binder.Status(false, true, List.of()), binder.getStatus());
        assertEquals(List.of(), heard); // nothing changed from the empty form

        type(box, "6");
        assertEquals(new Binder.Status(true, true, List.of()), binder.getStatus());
        type(box, "x");
        type(box, "7"); // still changed, valid again
        type(box, "5"); // back to what was read
        assertEquals(new Binder.Status(false, true, List.of()), binder.getStatus());
        assertEquals(List.of(new Binder.Status(true, true, List.of()), new Binder.Status(true, false, List.of()),
            new Binder.Status(true, true, List.of()), new Binder.Status(false, true, List.of())), heard);

        type(box, "8");
        assertTrue(binder.writeBean(ticket));
        assertFalse(binder.getStatus().hasChanges()); // the bean now holds it
        assertEquals(new Binder.Status(false, true, List.of()), heard.get(heard.size() - 1));

        registration.removeHandler();
        final int count = heard.size();
        type(box, "9");
        assertEquals(count, heard.size());
    }

    @Test
    public void aBindingValidatesItsFieldAndTellsTheStatus() {
        final PTextBox box = quantityBox();
        final Binding<Ticket, Integer> binding = bindQuantity(box);
        final List<Binder.Status> heard = new ArrayList<>();
        binder.addStatusChangeListener(heard::add);
        box.setText("x"); // set on the server: no change event
        assertTrue(binder.getStatus().hasChanges());

        assertFalse(binding.validate());
        assertEquals("true", box.getAttribute("aria-invalid"));
        assertEquals(CONVERSION, message(panel, 1).getInnerText());
        assertEquals(List.of(new Binder.Status(true, false, List.of())), heard);
        box.setText("3");
        assertTrue(binding.validate());
        assertNull(box.getAttribute("aria-invalid"));
    }

    // ---- unbuffered -------------------------------------------------------------------------------------------

    @Test
    public void setBeanWritesEachValidChangeToTheBeanAtOnce() {
        final PTextBox instrument = Element.newPTextBox();
        final PTextBox quantity = quantityBox();
        binder.forField(instrument).bind(Ticket::getInstrument, Ticket::setInstrument);
        bindQuantity(quantity);
        final Ticket ticket = ticket("EURUSD", 25);
        binder.setBean(ticket);
        assertSame(ticket, binder.getBean());
        assertEquals("25", quantity.getText());

        type(quantity, "30");
        assertEquals(30, ticket.getQuantity());
        assertFalse(binder.getStatus().hasChanges()); // the bean has it
        type(quantity, "abc"); // invalid: the bean keeps its value, the field says so
        assertEquals(30, ticket.getQuantity());
        assertEquals("true", quantity.getAttribute("aria-invalid"));
        assertTrue(binder.getStatus().hasChanges());
        type(instrument, "GBPUSD");
        assertEquals("GBPUSD", ticket.getInstrument());

        binder.setBean(null); // unbound, and empty
        assertNull(binder.getBean());
        assertEquals("", quantity.getText());
        type(instrument, "CHFUSD");
        assertEquals("GBPUSD", ticket.getInstrument());
    }

    @Test
    public void inAnUnbufferedBinderTheBeansValidatorsRunAfterEachWrite() {
        final PTextBox quantity = quantityBox();
        bindQuantity(quantity);
        binder.withValidator(t -> t.getQuantity() != null && t.getQuantity() > 100 ? Result.error("Too big") : Result.ok(t));
        final Ticket ticket = ticket("A", 5);
        binder.setBean(ticket);

        type(quantity, "500");
        assertEquals(500, ticket.getQuantity()); // written: the bean is not put back in this mode
        assertEquals(List.of("Too big"), binder.getStatus().beanErrors());
        assertFalse(binder.validate());
        type(quantity, "50");
        assertEquals(List.of(), binder.getStatus().beanErrors());
        assertTrue(binder.validate());

        type(quantity, "x"); // a field is wrong: the bean is not judged
        assertFalse(binder.validate());
        assertEquals(List.of(), binder.getStatus().beanErrors());
    }

    @Test
    public void readBeanLeavesUnbufferedMode() {
        final PTextBox quantity = quantityBox();
        bindQuantity(quantity);
        final Ticket bound = ticket("A", 1);
        binder.setBean(bound);
        binder.readBean(ticket("B", 2));
        assertNull(binder.getBean());
        type(quantity, "9");
        assertEquals(1, bound.getQuantity());
    }

    @Test
    public void whileTheBeanIsShownTheFieldsOwnEventsAreNotChanges() {
        final EagerField field = new EagerField();
        final int[] writes = { 0 };
        binder.forField(field).bind(Ticket::getInstrument, (t, v) -> {
            writes[0]++;
            t.setInstrument(v);
        });
        binder.setBean(ticket("EUR", 1)); // the field fires as it takes the value
        assertEquals(0, writes[0]);
        assertEquals(1, field.fired);

        field.setValueFromUser("USD"); // the user: a change
        assertEquals(1, writes[0]);
        assertEquals("USD", binder.getBean().getInstrument());
    }

    @Test
    public void aFieldThatIsNoWidgetStillBinds() {
        final EagerField field = new EagerField();
        binder.forField(field).withValidator(v -> !"bad".equals(v), "Bad").bind(Ticket::getInstrument, Ticket::setInstrument);
        binder.readBean(ticket("EUR", 1));
        field.setValueFromUser("bad");
        assertFalse(binder.getStatus().valid()); // nothing to show on, the status knows
        field.setValueFromUser("good");
        assertTrue(binder.getStatus().valid());
        final Ticket target = new Ticket();
        assertTrue(binder.writeBean(target));
        assertEquals("good", target.getInstrument());
    }

    // ---- list boxes -------------------------------------------------------------------------------------------

    @Test
    public void aListBoxBindsItsSelectedValue() {
        final PListBox box = Element.newPListBox(true);
        box.addItem("Market", Kind.MARKET);
        box.addItem("Limit", Kind.LIMIT);
        panel.add(box);
        final HasPValue<Kind> field = Fields.of(box, Kind.class);
        binder.forField(field).withValidator(k -> k != null, "Choose").bind(Ticket::getKind, Ticket::setKind);
        final Ticket ticket = new Ticket();
        ticket.setKind(Kind.LIMIT);
        binder.readBean(ticket);
        assertEquals(Kind.LIMIT, field.getValue());

        choose(box, 1);
        assertEquals(Kind.MARKET, field.getValue());
        choose(box, 0);
        assertEquals("true", box.getAttribute("aria-invalid"));
        assertEquals("Choose", message(panel, 1).getInnerText());
        choose(box, 2);
        assertNull(box.getAttribute("aria-invalid"));
        assertTrue(binder.writeBean(ticket));
        assertEquals(Kind.LIMIT, ticket.getKind());

        final List<Kind> heard = new ArrayList<>();
        final PValueChangeHandler<Kind> handler = e -> heard.add(e.getData());
        field.addValueChangeHandler(handler);
        choose(box, 1);
        assertEquals(List.of(Kind.MARKET), heard);
        assertEquals(2, field.getValueChangeHandlers().size()); // the binder's and this one
        assertTrue(field.removeValueChangeHandler(handler));
        assertFalse(field.removeValueChangeHandler(handler));
    }

    @Test
    public void aDropDownListBoxBindsItsSelectedData() {
        final ListBox<Kind> box = new ListBox<>(new ListBoxConfiguration(),
            List.of(ListBoxItem.of("Market", Kind.MARKET), ListBoxItem.of("Limit", Kind.LIMIT)));
        PWindow.getMain().add(box.asWidget());
        final HasPValue<Kind> field = Fields.of(box);
        binder.forField(field).bind(Ticket::getKind, Ticket::setKind);
        final Ticket ticket = new Ticket();
        ticket.setKind(Kind.LIMIT);
        binder.readBean(ticket);
        assertEquals(Kind.LIMIT, box.getSelectedData());

        PEmulator.click(((PComplexPanel) box.asWidget()).getWidget(2)); // its clear button
        assertNull(field.getValue());
        assertTrue(binder.getStatus().hasChanges());
        assertTrue(binder.writeBean(ticket));
        assertNull(ticket.getKind());
        box.asWidget().removeFromParent();
    }

    // ---- the fields' value types ------------------------------------------------------------------------------

    @Test
    public void numbersAndDatesBindThroughTheirConverters() {
        final PTextBox price = quantityBox();
        final PTextBox day = quantityBox();
        binder.forField(price).withConverter(Converters.toBigDecimal("Not a price")).bind(Ticket::getLimit, Ticket::setLimit);
        binder.forField(day).withConverter(Converters.toLocalDate("Not a date")).bind(Ticket::getDate, Ticket::setDate);
        final Ticket ticket = new Ticket();
        ticket.setLimit(new BigDecimal("1.0850"));
        ticket.setDate(LocalDate.of(2026, 10, 29));
        binder.readBean(ticket);
        assertEquals("1.0850", price.getText());
        assertEquals("2026-10-29", day.getText());

        type(price, "1,5");
        type(day, "tomorrow");
        assertFalse(binder.writeBean(ticket));
        type(price, "2.5");
        type(day, "2026-11-30");
        assertTrue(binder.writeBean(ticket));
        assertEquals(new BigDecimal("2.5"), ticket.getLimit());
        assertEquals(LocalDate.of(2026, 11, 30), ticket.getDate());
    }

    /** A value holder that is no widget and fires its handlers as it is set, as some custom fields do. */
    private static final class EagerField implements HasPValue<String> {

        private final List<PValueChangeHandler<String>> handlers = new ArrayList<>();
        private String value;
        private int fired;

        void setValueFromUser(final String value) {
            this.value = value;
            handlers.forEach(h -> h.onValueChange(new PValueChangeEvent<>(this, value)));
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(final String value) {
            this.value = value;
            fired++;
            handlers.forEach(h -> h.onValueChange(new PValueChangeEvent<>(this, value)));
        }

        @Override
        public void addValueChangeHandler(final PValueChangeHandler<String> handler) {
            handlers.add(handler);
        }

        @Override
        public boolean removeValueChangeHandler(final PValueChangeHandler<String> handler) {
            return handlers.remove(handler);
        }

        @Override
        public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
            return handlers;
        }
    }
}
