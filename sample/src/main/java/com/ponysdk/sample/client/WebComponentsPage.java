/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;
import static com.ponysdk.sample.client.Costs.*;

import java.util.Date;
import java.util.List;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWebComponent;

/** Web Components: standard custom elements driven from Java. */
final class WebComponentsPage {

    private WebComponentsPage() {
    }

    // ── Tab: WebComponents ───────────────────────────────────────────────────

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Web Components", "Standard custom elements driven from Java: attributes, properties, events, "
            + "slots and methods. Object properties travel as JSON merge-patches, only the changed keys.");
        buildWebComponentsInto(p);
        return p;
    }

    static void buildWebComponentsInto(final PFlowPanel container) {
        final PLabel statusLabel = Element.newPLabel("Status: waiting for events...");
        statusLabel.setStyleProperty("color", "var(--text-3)");
        statusLabel.setStyleProperty("font-style", "italic");

        // my-counter
        final PWebComponent counter = new PWebComponent("my-counter");
        counter.attr("label", "Clicks");
        counter.property("count").set(0);
        counter.onEvent("count-changed", event -> {
            final String detail = event.containsKey(ClientToServerModel.WC_EVENT_DETAIL.toStringValue())
                ? event.getString(ClientToServerModel.WC_EVENT_DETAIL.toStringValue()) : "{}";
            statusLabel.setText("count-changed: " + detail);
        });

        // my-dashboard
        final PWebComponent dashboard = new PWebComponent("my-dashboard");
        dashboard.attr("title", "PonySDK Dashboard");
        final PButton refreshBtn = Element.newPButton("Refresh");
        final PButton exportBtn = Element.newPButton("Export");
        dashboard.slot("toolbar").add(refreshBtn, exportBtn);
        final PFlowPanel contentPanel = Element.newPFlowPanel();
        final PTextBox searchBox = Element.newPTextBox();
        searchBox.setPlaceholder("Search (PTextBox inside WC)...");
        contentPanel.add(Element.newPLabel("PLabel inside 'content' slot"), searchBox);
        dashboard.slot("content").add(contentPanel);
        dashboard.slot("footer").add(statusLabel);
        refreshBtn.addClickHandler(e -> { statusLabel.setText("Refreshed at " + new java.util.Date()); dashboard.attr("theme", "light"); });
        exportBtn.addClickHandler(e -> { statusLabel.setText("Export triggered!"); dashboard.attr("theme", "dark"); });

        // my-card
        final PWebComponent card = new PWebComponent("my-card");
        card.attr("title", "PonySDK Card");
        card.property("content").set("\"Card body text set from server via property API.\"");
        final PButton likeBtn = Element.newPButton("Like");
        final PButton shareBtn = Element.newPButton("Share");
        card.slot("actions").add(likeBtn, shareBtn);
        likeBtn.addClickHandler(e -> statusLabel.setText("Liked!"));
        shareBtn.addClickHandler(e -> statusLabel.setText("Shared!"));

        // Counter controls
        final PFlowPanel counterControls = Element.newPFlowPanel();
        counterControls.setStyleProperty("display", "flex");
        counterControls.setStyleProperty("gap", "8px");
        final PButton resetBtn = Element.newPButton("Reset counter");
        resetBtn.addClickHandler(e -> { counter.call("reset"); statusLabel.setText("reset() called"); });
        final PButton setCountBtn = Element.newPButton("Set count = 42");
        setCountBtn.addClickHandler(e -> { counter.property("count").set(42); statusLabel.setText("count set to 42"); });
        counterControls.add(resetBtn, setCountBtn);

        // my-stats — object property updated through JSON merge-patch (only changed keys travel)
        final PWebComponent stats = new PWebComponent("my-stats");
        final int[] trades = { 0 };
        final String statsTemplate = "{\"desk\":\"FX Spot EUR/USD\",\"trader\":\"demo-trader\",\"trades\":%d%s}";
        stats.property("stats").set(String.format(statsTemplate, 0, ",\"note\":\"initial snapshot\""));
        final PFlowPanel statsControls = Element.newPFlowPanel();
        statsControls.setStyleProperty("display", "flex");
        statsControls.setStyleProperty("gap", "8px");
        final PButton tradeBtn = Element.newPButton("Patch: +1 trade");
        tradeBtn.addClickHandler(e -> {
            trades[0]++;
            stats.property("stats").set(String.format(statsTemplate, trades[0], ",\"note\":\"initial snapshot\""));
            statusLabel.setText("stats patched: trades=" + trades[0]);
        });
        final PButton dropNoteBtn = Element.newPButton("Patch: drop note");
        dropNoteBtn.addClickHandler(e -> {
            stats.property("stats").set(String.format(statsTemplate, trades[0], ""));
            statusLabel.setText("stats patched: note removed");
        });
        statsControls.add(tradeBtn, dropNoteBtn);

        // Slot children added after the component is live
        final int[] toolbarItems = { 0 };
        final PButton addToolbarItemBtn = Element.newPButton("Add toolbar item");
        addToolbarItemBtn.addClickHandler(e -> {
            final PLabel item = Element.newPLabel("Item " + ++toolbarItems[0]);
            item.addStyleName("dyn-toolbar-item");
            dashboard.slot("toolbar").add(item);
        });
        counterControls.add(addToolbarItemBtn);

        container.add(exampleCard("<my-counter>",
            "An attribute and a property set from Java, a method call, and the element's own event handled on the server.",
            stack(counter, counterControls),
            ln(ty("PWebComponent") + " counter = " + kw("new") + " " + ty("PWebComponent") + "(" + st("my-counter") + ");")
                + ln("counter." + mt("attr") + "(" + st("label") + ", " + st("Clicks") + ");")
                + ln("counter." + mt("property") + "(" + st("count") + ")." + mt("set") + "(0);")
                + ln("counter." + mt("onEvent") + "(" + st("count-changed") + ", e -> status." + mt("setText") + "(...));")
                + ln("reset." + mt("addClickHandler") + "(e -> counter." + mt("call") + "(" + st("reset") + "));"),
            costSpec("PWebComponent <my-counter>")));
        container.add(exampleCard("<my-stats>",
            "An object property: each change travels as a JSON merge-patch carrying only the keys that changed.",
            stack(stats, statsControls),
            ln(cm("only \"trades\" travels; a key set to null is removed on the element"))
                + ln("stats." + mt("property") + "(" + st("stats") + ")." + mt("set") + "(json);")));
        container.add(exampleCard("<my-dashboard>",
            "Named slots filled with PonySDK widgets - buttons, a text box, a label - and children added while it is live.",
            dashboard,
            ln("dashboard." + mt("slot") + "(" + st("toolbar") + ")." + mt("add") + "(refresh, export);")
                + ln("dashboard." + mt("slot") + "(" + st("content") + ")." + mt("add") + "(panel);")
                + ln("dashboard." + mt("slot") + "(" + st("footer") + ")." + mt("add") + "(status);")));
        container.add(exampleCard("<my-card>",
            "A property set from the server and an actions slot whose buttons are handled in Java.",
            card,
            ln("card." + mt("property") + "(" + st("content") + ")." + mt("set") + "(" + st("Card body text...") + ");")
                + ln("card." + mt("slot") + "(" + st("actions") + ")." + mt("add") + "(like, share);")));
    }
}
