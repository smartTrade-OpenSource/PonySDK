/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;

import java.util.List;
import java.util.Locale;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.impl.java.ShowcaseMain;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Embedding (docs/decisions/0008-embedded-application.md): a PonySDK application inside a page it does not own. This
 * page embeds the ticket itself, a second application on a context of its own, and drives it as any host does; the
 * portal (sample/src/main/resources/portal) does the same from a plain page of another origin.
 */
final class EmbeddingPage {


    private EmbeddingPage() {
    }

    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel page = section();
        pageTitle(page, "Embedding", "A PonySDK application inside a page it does not own: the <pony-app> element. Each "
            + "element runs its own terminal on its own context, in a shadow root; its attributes and events carry what "
            + "the host page and the application say to each other.");

        page.add(sectionTitle("Live: a ticket embedded in this page"));
        // Absolute: this page may itself be embedded in another (the portal embeds the showcase)
        final String authority = uiContext.getHeader("Host"); // with its port, unlike getHost()
        final String self = (uiContext.isSecure() ? "https://" : "http://") + authority + "/"
            + uiContext.getConfiguration().getApplicationContextName() + "/";
        final PWebComponent ticket = new PWebComponent("pony-app");
        ticket.addStyleName("pony-embedding-ticket");
        ticket.setAttribute("src", self);
        ticket.setAttribute("view", EmbeddedTicket.VIEW);
        ticket.setAttribute("instrument", "EURUSD");
        ticket.add(Element.newPLabel("Loading the ticket…"));

        final PListBox instrument = Element.newPListBox();
        instrument.setAttribute("aria-label", "Instrument");
        for (final String name : List.of("EURUSD", "GBPUSD", "USDJPY", "USDCHF")) instrument.addItem(name);
        instrument.addChangeHandler(event -> ticket.setAttribute("instrument", instrument.getSelectedItem()));
        final PTextBox quantity = Element.newPTextBox();
        quantity.setAttribute("aria-label", "Quantity");
        quantity.setText("1000000");
        quantity.addValueChangeHandler(event -> ticket.setAttribute("quantity", quantity.getText()));

        final PFlowPanel trades = Element.newPFlowPanel();
        trades.addStyleName("pony-embedding-trades");
        final PElement none = Element.newDiv();
        none.setInnerText("No trade yet: the ticket's trade events show here.");
        trades.add(none);
        ticket.addEventListener("trade", trade -> {
            if (none.getParent() != null) none.removeFromParent();
            final PElement row = Element.newDiv();
            row.addStyleName("pony-embedding-trade");
            row.setInnerText(describe(trade));
            trades.insert(row, 0);
        });

        final PFlowPanel host = Element.newPFlowPanel();
        host.addStyleName("pony-embedding-host");
        host.add(labeled("instrument", instrument), labeled("quantity", quantity), sectionTitle("Trade events"), trades);
        final PFlowPanel live = Element.newPFlowPanel();
        live.addStyleName("pony-embedding-live");
        live.add(ticket, host);
        page.add(live);

        page.add(sectionTitle("On another origin"));
        final String portal = "http://" + authority.replaceFirst(":\\d+$", "") + ":" + ShowcaseMain.PORTAL_PORT + "/";
        final PAnchor link = Element.newPAnchor("Open the portal: " + portal, portal);
        link.setAttribute("target", "_blank");
        link.setAttribute("rel", "noopener");
        page.add(link);
        page.add(codeSnippet("The host page", "HTML", escape("""
            <script type="module" src="https://fx.example.com/sample/ponyterminal/terminal.js"></script>
            <pony-app src="https://fx.example.com/sample/" view="ticket" instrument="EURUSD">Loading…</pony-app>
            <script>
              const ticket = document.querySelector('pony-app');
              ticket.addEventListener('trade', e => blotter.add(e.detail));     // events out
              ticket.setAttribute('instrument', 'USDJPY');                      // attributes in
              ticket.dispatchEvent(new CustomEvent('reset'));                   // events in
            </script>""")));
        page.add(codeSnippet("The application: EmbeddedTicket.java, the element's attributes and events in", "Java",
            Source.code(EmbeddedTicket.class, "host")));
        page.add(codeSnippet("The application: a trade, an event out", "Java", Source.code(EmbeddedTicket.class, "trade")));
        return page;
    }

    private static String describe(final JsonValue detail) {
        if (!(detail instanceof JsonObject trade)) return detail.toString();
        try {
            return String.format(Locale.ROOT, "%s  %s %,d %s at %s", trade.getString("time", ""), trade.getString("side", ""),
                trade.getJsonNumber("quantity").longValue(), trade.getString("instrument", ""), trade.getJsonNumber("price"));
        } catch (final RuntimeException e) {
            return trade.toString();
        }
    }

    /** One numbered line per line of code, as text. */
    private static String escape(final String code) {
        final StringBuilder html = new StringBuilder();
        for (final String line : code.split("\n")) html.append(ln(line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")));
        return html.toString();
    }
}
