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

import static com.ponysdk.core.ui.binder.BinderTest.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.binder.Ticket.Kind;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

import jakarta.validation.constraints.Size;

/** The binder with Bean Validation (Hibernate Validator, on the test classpath) for the bean's constraints. */
public class BinderBeanValidationTest extends PSuite {

    private final Binder<Ticket> binder = new Binder<>(Ticket.class);
    private final PFlowPanel panel = Element.newPFlowPanel();

    private PTextBox box() {
        final PTextBox box = Element.newPTextBox();
        panel.add(box);
        return box;
    }

    private String message(final int index) {
        return ((PElement) panel.getWidget(index)).getInnerText();
    }

    private PTextBox instrument() {
        final PTextBox box = box();
        binder.forField(box).bind(Ticket::getInstrument, Ticket::setInstrument);
        return box;
    }

    private PTextBox quantity() {
        final PTextBox box = box();
        binder.forField(box).withConverter(Converters.toInteger("Not a whole number")).bind(Ticket::getQuantity, Ticket::setQuantity);
        return box;
    }

    private PTextBox limit() {
        final PTextBox box = box();
        binder.forField(box).withConverter(Converters.toBigDecimal("Not a price")).bind(Ticket::getLimit, Ticket::setLimit);
        return box;
    }

    private PListBox kind() {
        final PListBox box = Element.newPListBox();
        box.addItem("Market", Kind.MARKET);
        box.addItem("Limit", Kind.LIMIT);
        panel.add(box);
        binder.forField(Fields.of(box, Kind.class)).bind(Ticket::getKind, Ticket::setKind);
        return box;
    }

    @Test
    public void theConstraintsOfAPropertyApplyToItsField() {
        final PTextBox box = instrument();

        type(box, "AB");
        assertEquals("true", box.getAttribute("aria-invalid"));
        assertEquals("Too short", message(1));
        type(box, "abC");
        assertEquals("Upper case only", message(1));
        type(box, "aa"); // two constraints: both messages, in a fixed order
        assertEquals("Too short; Upper case only", message(1));
        type(box, "ABC");
        assertNull(box.getAttribute("aria-invalid"));
    }

    @Test
    public void theConstraintsApplyToTheConvertedValueAndAMissingOneIsNull() {
        final PTextBox box = quantity();
        final PTextBox price = limit();

        type(box, "abc"); // the conversion first: a constraint judges a value
        assertEquals("Not a whole number", message(1));
        type(box, "");
        assertEquals("Quantity required", message(1));
        type(box, "0");
        assertEquals("At least 1", message(1));
        type(box, "5000");
        assertEquals("At most 1000", message(1));
        type(box, "10");
        assertNull(box.getAttribute("aria-invalid"));

        type(price, "-1.5");
        assertEquals("Positive only", message(3));
        type(price, "");
        assertNull(price.getAttribute("aria-invalid")); // @Positive accepts an absent price
    }

    @Test
    public void aCheckBoxIsJudgedByItsPrimitiveProperty() {
        final PCheckBox box = Element.newPCheckBox("I confirm");
        panel.add(box);
        binder.forField(box).bind(Ticket::isConfirmed, Ticket::setConfirmed); // "isConfirmed" is the property "confirmed"
        binder.readBean(new Ticket());
        assertFalse(binder.getStatus().valid()); // not confirmed: invalid, and no error shown yet
        assertEquals(1, panel.getWidgetCount());

        PEmulator.valueChange(box, true);
        assertTrue(binder.getStatus().valid());
        PEmulator.valueChange(box, false);
        assertEquals("Confirm", message(1));
    }

    @Test
    public void aLimitOrderWithoutPriceIsRefusedByTheClassAndTheBeanKeepsItsValues() {
        final PTextBox instrument = instrument();
        final PTextBox quantity = quantity();
        final PTextBox price = limit();
        final PListBox kind = kind();
        final Ticket ticket = new Ticket();
        ticket.setInstrument("EUR");
        ticket.setQuantity(10);
        binder.readBean(ticket);

        type(instrument, "USD");
        selectLimit(kind);
        type(quantity, "20");
        assertFalse(binder.writeBean(ticket));
        assertEquals(List.of("A limit order needs a limit price"), binder.getStatus().beanErrors());
        assertEquals("EUR", ticket.getInstrument());
        assertEquals(10, ticket.getQuantity());
        assertEquals(Kind.MARKET, ticket.getKind());
        assertNull(price.getAttribute("aria-invalid")); // the rule is the bean's, not the price's

        type(price, "1.0850");
        assertEquals(List.of(), binder.getStatus().beanErrors());
        assertTrue(binder.writeBean(ticket));
        assertEquals("USD", ticket.getInstrument());
        assertEquals(Kind.LIMIT, ticket.getKind());
        assertEquals(new BigDecimal("1.0850"), ticket.getLimit());
    }

    private static void selectLimit(final PListBox kind) {
        BinderTest.choose(kind, 1);
    }

    @Test
    public void aClassConstraintThatNamesABoundPropertyShowsOnThatField() {
        final PTextBox quantity = quantity();
        final PTextBox price = limit();
        final Ticket ticket = new Ticket();
        ticket.setInstrument("EUR");
        ticket.setQuantity(10);
        binder.readBean(ticket);

        type(quantity, "950"); // a large order needs a price, said on the price
        assertFalse(binder.writeBean(ticket));
        assertEquals("true", price.getAttribute("aria-invalid"));
        assertEquals("A large order needs a limit price", message(2));
        assertEquals(List.of(), binder.getStatus().beanErrors());
        assertFalse(binder.getStatus().valid());
        assertEquals(10, ticket.getQuantity());

        type(price, "1.1"); // its own edit validates it again
        assertNull(price.getAttribute("aria-invalid"));
        assertTrue(binder.writeBean(ticket));
        assertEquals(950, ticket.getQuantity());

        type(price, ""); // not an edit of the quantity: the message goes at the next write
        assertFalse(binder.writeBean(ticket));
        assertEquals("true", price.getAttribute("aria-invalid"));
        type(quantity, "20"); // an edit elsewhere makes the bean's last errors stale
        assertNull(price.getAttribute("aria-invalid"));
        assertTrue(binder.writeBean(ticket));
    }

    @Test
    public void aConstraintOnAPropertyTheFormDoesNotBindIsNotTheFormsToShow() {
        final PTextBox instrument = instrument();
        final Ticket ticket = new Ticket();
        ticket.setInstrument("EUR");
        ticket.setNote("a note that is far too long"); // against the class's note rule, on a property no field edits
        binder.readBean(ticket);
        type(instrument, "USD");
        assertTrue(binder.writeBean(ticket));
        assertEquals("USD", ticket.getInstrument());
    }

    @Test
    public void unbufferedTheClassConstraintsRunAfterEachWrite() {
        final PTextBox price = limit();
        final PListBox kind = kind();
        final Ticket ticket = new Ticket();
        binder.setBean(ticket);

        selectLimit(kind);
        assertEquals(Kind.LIMIT, ticket.getKind()); // written: this mode does not put the bean back
        assertEquals(List.of("A limit order needs a limit price"), binder.getStatus().beanErrors());
        assertFalse(binder.validate());
        type(price, "2");
        assertEquals(List.of(), binder.getStatus().beanErrors());
        assertTrue(binder.getStatus().valid());
    }

    @Test
    public void aBinderWithoutABeanTypeAppliesNoConstraint() {
        final Binder<Ticket> plain = new Binder<>();
        final PTextBox box = box();
        plain.forField(box).bind(Ticket::getInstrument, Ticket::setInstrument);
        type(box, "a");
        assertNull(box.getAttribute("aria-invalid"));
    }

    // ---- the property of a getter -----------------------------------------------------------------------------

    public record Line(@Size(max = 3, message = "Too long") String code, @Size(max = 1, message = "Too long") String is,
                       @Size(max = 1, message = "Too long") String issue) {
    }

    @SuppressWarnings("unused")
    public static class Odd {

        @Size(max = 1, message = "Too long")
        private String URL;
        @Size(max = 1, message = "Too long")
        private String x;

        public String getURL() {
            return URL;
        }

        public String getX() {
            return x;
        }
    }

    @Test
    public void thePropertyIsTheNameOfTheMethodReference() {
        final Binder<Line> lines = new Binder<>(Line.class);
        final PTextBox code = box();
        final PTextBox is = box();
        final PTextBox issue = box();
        lines.forField(code).bind(Line::code, null); // a record's accessor is the property
        lines.forField(is).bind(Line::is, null); // "is" alone, and "issue": not a getter's prefix
        lines.forField(issue).bind(Line::issue, null);
        for (final PTextBox box : List.of(code, is, issue)) {
            type(box, "abcd");
            assertEquals("true", box.getAttribute("aria-invalid"));
        }
        type(code, "abc");
        assertNull(code.getAttribute("aria-invalid"));

        final Binder<Odd> odd = new Binder<>(Odd.class);
        final PTextBox url = box();
        final PTextBox x = box();
        odd.forField(url).bind(Odd::getURL, null); // an acronym keeps its capitals
        odd.forField(x).bind(Odd::getX, null);
        type(url, "ab");
        type(x, "ab");
        assertEquals("true", url.getAttribute("aria-invalid"));
        assertEquals("true", x.getAttribute("aria-invalid"));
    }

    @Test
    public void aLambdaNeedsItsPropertyNameSaid() {
        final PTextBox box = box();
        final Binder.BindingBuilder<Ticket, String> builder = binder.forField(box);
        assertThrows(IllegalArgumentException.class,
            () -> builder.bind(t -> t.getInstrument(), Ticket::setInstrument));
        // a getter that is not a lambda at all
        final Binder.Getter<Ticket, String> notALambda = new Binder.Getter<>() {
            @Override
            public String apply(final Ticket ticket) {
                return ticket.getInstrument();
            }
        };
        assertThrows(IllegalArgumentException.class, () -> builder.bind(notALambda, Ticket::setInstrument));

        builder.bind(t -> t.getInstrument(), Ticket::setInstrument, "instrument");
        type(box, "a");
        assertEquals("Too short; Upper case only", message(1));
    }

    @Test
    public void aNameThatIsNoPropertyOfTheBeanFailsAtBind() {
        final Binder.BindingBuilder<Ticket, String> builder = binder.forField(box());
        assertThrows(IllegalArgumentException.class, () -> builder.bind(Ticket::getInstrument, Ticket::setInstrument, "instrumnt"));
        assertThrows(NullPointerException.class, () -> builder.bind(Ticket::getInstrument, Ticket::setInstrument, null));
    }
}
