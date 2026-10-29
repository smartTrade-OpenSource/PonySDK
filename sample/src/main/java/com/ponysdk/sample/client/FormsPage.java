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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PTextArea;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.binder.Binder;
import com.ponysdk.core.ui.binder.Converters;
import com.ponysdk.core.ui.binder.Fields;
import com.ponysdk.sample.client.OrderTicket.Side;
import com.ponysdk.sample.client.OrderTicket.Type;

/**
 * Forms (docs/decisions/0021-forms-binder.md): an order ticket bound to a bean. The rules are the bean's annotations
 * (Bean Validation); the binder shows each error on its field, for the eyes and for a screen reader.
 */
final class FormsPage {

    private FormsPage() {
    }

    static PFlowPanel build() {
        final PFlowPanel p = section();
        pageTitle(p, "Forms", "An order ticket bound to a bean. The rules are annotations on the bean: a field says what "
                + "is wrong with it as you leave it, and Save writes the bean only when everything is valid.");

        final PFlowPanel form = Element.newPFlowPanel();
        form.addStyleName("forms-card");
        form.setAttribute("role", "group");
        form.setAttribute("aria-label", "Order ticket");

        // region fields
        final PTextBox instrument = Element.newPTextBox();
        final PListBox side = Element.newPListBox(true); // an empty item: nothing chosen yet
        side.addItem("Buy", Side.BUY);
        side.addItem("Sell", Side.SELL);
        final PListBox type = Element.newPListBox();
        type.addItem("Market", Type.MARKET);
        type.addItem("Limit", Type.LIMIT);
        final PTextBox quantity = Element.newPTextBox();
        final PTextBox limitPrice = Element.newPTextBox();
        final SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
        day.setTimeZone(TimeZone.getDefault());
        final PDateBox valueDate = Element.newPDateBox(day);
        final PTextArea comment = Element.newPTextArea();
        final PCheckBox confirmed = Element.newPCheckBox("I confirm the details");
        // endregion
        form.add(row("Instrument", instrument), row("Side", side), row("Type", type), row("Quantity", quantity),
            row("Limit price", limitPrice), row("Value date (yyyy-MM-dd)", valueDate), row("Comment", comment),
            checkRow(confirmed));

        final PElement problems = Element.newDiv(); // always there: what is put in an alert is announced
        problems.addStyleName("forms-problems");
        problems.setAttribute("role", "alert");
        final PElement status = Element.newDiv();
        status.addStyleName("forms-status");
        status.setAttribute("role", "status");
        final PButton save = Element.newPButton("Save");
        save.addStyleName("primary");
        final PButton reset = Element.newPButton("Reset");
        final PFlowPanel actions = Element.newPFlowPanel();
        actions.addStyleName("forms-actions");
        actions.add(save, reset, status);
        final PElement saved = Element.newDiv();
        saved.addStyleName("forms-saved");
        saved.setAttribute("aria-live", "polite");
        saved.setInnerText("Nothing saved yet.");

        // region binder
        final Binder<OrderTicket> binder = new Binder<>(OrderTicket.class); // its constraints apply to the fields
        binder.forField(instrument).bind(OrderTicket::getInstrument, OrderTicket::setInstrument);
        binder.forField(Fields.of(side, Side.class)).bind(OrderTicket::getSide, OrderTicket::setSide);
        binder.forField(Fields.of(type, Type.class)).bind(OrderTicket::getType, OrderTicket::setType);
        binder.forField(quantity).withConverter(Converters.toInteger("Not a whole number"))
            .bind(OrderTicket::getQuantity, OrderTicket::setQuantity);
        binder.forField(limitPrice).withConverter(Converters.toBigDecimal("Not a price"))
            .bind(OrderTicket::getLimitPrice, OrderTicket::setLimitPrice);
        binder.forField(valueDate).withConverter(Converters.dateToLocalDate(ZoneId.systemDefault()))
            .bind(OrderTicket::getValueDate, OrderTicket::setValueDate);
        binder.forField(comment).bind(OrderTicket::getComment, OrderTicket::setComment);
        binder.forField(confirmed).bind(OrderTicket::isConfirmed, OrderTicket::setConfirmed);
        // endregion

        // region save
        // The bean is read into the fields, and written back only when every field and the bean itself are valid
        final OrderTicket order = new OrderTicket();
        order.setInstrument("EURUSD");
        order.setQuantity(1_000);
        order.setValueDate(LocalDate.now());
        binder.readBean(order);

        binder.addStatusChangeListener(s -> {
            status.setInnerText(s.hasChanges() ? "Unsaved changes" : "No changes");
            problems.setInnerText(String.join(" ", s.beanErrors())); // the class-level rule: the bean's own error
        });
        save.addClickHandler(e -> {
            if (binder.writeBean(order)) {
                problems.setInnerText("");
                saved.setInnerText("Saved: " + order);
            } else if (binder.getStatus().beanErrors().isEmpty()) {
                problems.setInnerText("Some fields are not valid: fix the ones marked.");
            }
        });
        reset.addClickHandler(e -> {
            binder.readBean(order); // back to the last saved bean, no error shown
            problems.setInnerText("");
        });
        // endregion
        status.setInnerText("No changes");

        form.add(problems, actions);
        p.add(form, saved);
        p.add(codeToggle("the bean and its constraints", Source.code(OrderTicket.class, "constraints")
            + Source.code(OrderTicket.class, "rule")));
        p.add(codeToggle("the fields and their binding", Source.code(FormsPage.class, "fields")
            + Source.code(FormsPage.class, "binder")));
        p.add(codeToggle("reading, saving and the status", Source.code(FormsPage.class, "save")));
        return p;
    }

    /** A label and its field: a real {@code <label for>}, so a screen reader names the field and a click focuses it. */
    private static PFlowPanel row(final String caption, final PWidget field) {
        field.forceDomId();
        final PElement label = Element.newLabel();
        label.setInnerText(caption);
        label.setAttribute("for", String.valueOf(field.getID()));
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("forms-row");
        row.add(label, field); // the binder puts an error's message right after the field
        return row;
    }

    private static PFlowPanel checkRow(final PCheckBox box) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("forms-row");
        row.addStyleName("forms-row-check");
        row.add(box);
        return row;
    }
}
