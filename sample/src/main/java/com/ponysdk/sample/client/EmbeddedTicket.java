/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PHost;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;

import jakarta.json.Json;

/**
 * The ticket another page embeds, {@code <pony-app src=".../sample/" view="ticket" instrument="EURUSD">}
 * (docs/decisions/0008-embedded-application.md; the portal: sample/src/main/resources/portal). Its instrument and
 * quantity are the element's attributes, followed when the host changes them; the host's {@code reset} event puts the
 * quantity back; each trade goes to the host as a {@code trade} event. Prices are pushed by the server.
 */
final class EmbeddedTicket {

    static final String VIEW = "ticket";
    private static final Map<String, Double> MIDS = Map.of("EURUSD", 1.0842, "GBPUSD", 1.2710, "USDJPY", 151.20,
        "USDCHF", 0.8850);
    private static final String DEFAULT_INSTRUMENT = "EURUSD";
    private static final String DEFAULT_QUANTITY = "1000000";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PHost host;
    private final PLabel instrumentLabel = Element.newPLabel();
    private final PButton sell = Element.newPButton();
    private final PButton buy = Element.newPButton();
    private final PTextBox quantity = Element.newPTextBox();
    private final PLabel last = Element.newPLabel("No trade yet");
    private PDialogBox dialog;
    private final PLabel question = Element.newPLabel();
    /** The bid the open dialog asks to sell at. */
    private double sellPrice;
    private String instrument;
    private double mid;
    private double bid;
    private double ask;

    private EmbeddedTicket(final UIContext uiContext, final PHost host) {
        this.host = host;
        final PFlowPanel ticket = Element.newPFlowPanel();
        ticket.addStyleName("pony-ticket");

        final PFlowPanel head = Element.newPFlowPanel();
        head.addStyleName("pony-ticket-head");
        instrumentLabel.addStyleName("pony-ticket-instrument");
        final PLabel spot = Element.newPLabel("Spot · live");
        spot.addStyleName("pony-ticket-tag");
        head.add(instrumentLabel, spot);

        final PFlowPanel prices = Element.newPFlowPanel();
        prices.addStyleName("pony-ticket-prices");
        sell.addStyleName("pony-ticket-side");
        sell.addStyleName("pony-ticket-sell");
        buy.addStyleName("pony-ticket-side");
        buy.addStyleName("pony-ticket-buy");
        prices.add(sell, buy);

        final PFlowPanel amount = Element.newPFlowPanel();
        amount.addStyleName("pony-ticket-amount");
        final PLabel amountLabel = Element.newPLabel("Quantity");
        quantity.addStyleName("pony-ticket-quantity");
        quantity.setAttribute("inputmode", "numeric");
        quantity.setAttribute("aria-label", "Quantity");
        amount.add(amountLabel, quantity);

        last.addStyleName("pony-ticket-last");
        final PElement origin = Element.newDiv();
        origin.addStyleName("pony-ticket-origin");
        final String hostOrigin = uiContext.getHeader("Origin");
        origin.setInnerText("Embedded in " + (hostOrigin != null ? hostOrigin : "this page") + " · context #" + uiContext.getID());

        ticket.add(head, prices, amount, last, origin);
        PWindow.getMain().add(ticket);

        setInstrument(host.getAttribute("instrument"));
        setQuantity(host.getAttribute("quantity"));
        host.addAttributeListener("instrument", this::setInstrument);
        host.addAttributeListener("quantity", this::setQuantity);
        host.addEventListener("reset", detail -> setQuantity(host.getAttribute("quantity")));

        buy.addClickHandler(event -> trade("BUY", ask));
        sell.addClickHandler(event -> confirmSell());
        PScheduler.scheduleAtFixedRate(this::tick, Duration.ofMillis(500));
    }

    /** The ticket of {@code <pony-app view="ticket">}. */
    static void build(final UIContext uiContext, final PHost host) {
        new EmbeddedTicket(uiContext, host);
    }

    private void setInstrument(final String value) {
        instrument = value != null && MIDS.containsKey(value) ? value : DEFAULT_INSTRUMENT;
        mid = MIDS.get(instrument);
        instrumentLabel.setText(instrument.substring(0, 3) + " / " + instrument.substring(3));
        tick();
    }

    private void setQuantity(final String value) {
        quantity.setText(value != null ? value : DEFAULT_QUANTITY);
    }

    /** A new price: a random walk around the instrument's mid. */
    private void tick() {
        mid *= 1 + ThreadLocalRandom.current().nextGaussian() * 0.00004;
        final double spread = mid * 0.00001;
        bid = mid - spread;
        ask = mid + spread;
        sell.setHTML("<span>Sell</span><b>" + price(bid) + "</b>");
        buy.setHTML("<span>Buy</span><b>" + price(ask) + "</b>");
    }

    private String price(final double value) {
        return String.format(Locale.ROOT, instrument.endsWith("JPY") ? "%.3f" : "%.5f", value);
    }

    /** A sell asks first: a modal dialog inside the element, styled by the application's stylesheets. */
    private void confirmSell() {
        if (dialog == null) {
            dialog = Element.newPDialogBox();
            dialog.setCaption("Confirm the sale");
            dialog.addStyleName("pony-ticket-dialog");
            final PFlowPanel body = Element.newPFlowPanel();
            final PFlowPanel actions = Element.newPFlowPanel();
            actions.addStyleName("pony-ticket-actions");
            final PButton confirm = Element.newPButton("Confirm");
            final PButton cancel = Element.newPButton("Cancel");
            confirm.addClickHandler(event -> {
                dialog.hide();
                trade("SELL", sellPrice);
            });
            cancel.addClickHandler(event -> dialog.hide());
            actions.add(cancel, confirm);
            body.add(question, actions);
            dialog.setWidget(body);
            dialog.setModal(true);
            dialog.setGlassEnabled(true);
            PWindow.getMain().add(dialog); // created when attached to its window, then centred
        }
        sellPrice = bid;
        question.setText("Sell " + quantity.getText() + " " + instrument + " at " + price(sellPrice) + "?");
        dialog.center();
    }

    /** The trade goes to the host page, which keeps its own blotter. */
    private void trade(final String side, final double price) {
        final long amount;
        try {
            amount = Long.parseLong(quantity.getText().replace(",", "").replace("_", "").trim());
        } catch (final NumberFormatException e) {
            last.setText("Quantity: a whole number");
            return;
        }
        final String time = LocalTime.now().format(TIME);
        host.dispatchEvent("trade", Json.createObjectBuilder()
            .add("instrument", instrument)
            .add("side", side)
            .add("quantity", amount)
            .add("price", Double.parseDouble(price(price)))
            .add("time", time)
            .build());
        last.setText(time + " · " + side + " " + amount + " " + instrument + " at " + price(price));
    }
}
