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

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.fdc3.Fdc3;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * FDC3 (docs/decisions/0014-fdc3.md): this page talks to the other applications of the desktop through the page's
 * desktop agent; here the sample's demonstration agent, which links the pages of this browser.
 */
final class Fdc3Page {

    private static final String[][] INSTRUMENTS = { { "AAPL", "Apple" }, { "MSFT", "Microsoft" }, { "NVDA", "Nvidia" },
            { "TSLA", "Tesla" }, { "AMZN", "Amazon" } };

    private Fdc3Page() {
    }

    static PFlowPanel build() {
        final PFlowPanel p = section();
        pageTitle(p, "FDC3", "The applications of a trader's desktop share the instrument in view and hand each other "
                + "work, through the desktop agent of the page (FINOS FDC3). Open the monitor, or this page in a second "
                + "window: what one broadcasts on a channel, the others follow.");

        final PLabel agent = Element.newPLabel("Looking for the desktop agent…");
        agent.addStyleName("fdc3-agent");
        final PFlowPanel channels = Element.newPFlowPanel();
        channels.addStyleName("fdc3-channels");
        final PFlowPanel instruments = Element.newPFlowPanel();
        instruments.addStyleName("fdc3-instruments");
        final PLabel received = Element.newPLabel("Nothing received on this channel yet");
        received.addStyleName("fdc3-received");
        final PLabel intent = Element.newPLabel("No intent handled yet");
        intent.addStyleName("fdc3-received");
        final PLabel raised = Element.newPLabel("");
        raised.addStyleName("fdc3-raised");
        final List<HandlerRegistration> registrations = new ArrayList<>();

        // region fdc3
        final Fdc3 fdc3 = Fdc3.get();
        registrations.add(fdc3.addStateListener(state -> {
            if (state == Fdc3.State.UNAVAILABLE) agent.setText("No desktop agent in this page");
            if (state != Fdc3.State.READY) return;
            agent.setText(fdc3.getInfo().getString("provider") + " " + fdc3.getInfo().getString("providerVersion")
                    + ", FDC3 " + fdc3.getInfo().getString("fdc3Version"));
            for (final JsonObject channel : fdc3.getUserChannels()) {
                final String id = channel.getString("id");
                final PButton join = Element.newPButton(channel.getJsonObject("displayMetadata").getString("name"));
                join.addStyleName("fdc3-channel");
                join.setAttribute("data-channel", id);
                join.addClickHandler(e -> fdc3.joinUserChannel(id));
                channels.add(join);
            }
            if (fdc3.getCurrentChannel() == null) fdc3.joinUserChannel("fdc3.channel.1");
        }));
        registrations.add(fdc3.addChannelListener(id -> channels.forEach(w -> {
            if (id != null && id.equals(w.getAttribute("data-channel"))) w.addStyleName("fdc3-channel-on");
            else w.removeStyleName("fdc3-channel-on");
        })));

        // Broadcast the instrument in view; follow the one the channel brings
        for (final String[] instrument : INSTRUMENTS) {
            final PButton button = Element.newPButton(instrument[0]);
            button.addClickHandler(e -> fdc3.broadcast(Fdc3.instrument(instrument[0], instrument[1])));
            instruments.add(button);
        }
        registrations.add(fdc3.addContextListener("fdc3.instrument",
            context -> received.setText("Following " + ticker(context) + " from the channel")));

        // Handle ViewInstrument for the others; raise it for them
        registrations.add(fdc3.addIntentListener("ViewInstrument", context -> {
            intent.setText("ViewInstrument " + ticker(context) + ", raised by another application");
            return Json.createObjectBuilder().add("type", "ponysdk.shown").add("ticker", ticker(context)).build();
        }));
        final PButton raise = Element.newPButton("Raise ViewInstrument for MSFT");
        raise.addClickHandler(e -> fdc3.raiseIntent("ViewInstrument", Fdc3.instrument("MSFT", "Microsoft"),
            result -> raised.setText(result.failed() ? "Not handled: " + result.error()
                    : "Handled by " + result.app() + ", result " + result.result())));
        // endregion

        final PAnchor monitor = Element.newPAnchor("Open the FDC3 monitor", "fdc3-monitor.html");
        monitor.setAttribute("target", "_blank");
        final PAnchor again = Element.newPAnchor("Open this page in another window", "#fdc3");
        again.setAttribute("target", "_blank");
        final PFlowPanel links = Element.newPFlowPanel();
        links.addStyleName("fdc3-links");
        links.add(monitor, again);

        p.add(agent, links);
        p.add(sectionTitle("Channel"));
        p.add(channels);
        p.add(sectionTitle("Broadcast"));
        p.add(instruments, received);
        p.add(sectionTitle("Intents"));
        p.add(stack(raise, raised, intent));
        p.add(codeToggle("the FDC3 calls", Source.code(Fdc3Page.class, "fdc3")));
        // The page left: its listeners go, the agent stops sending here what no one shows
        p.addDestroyListener(event -> registrations.forEach(HandlerRegistration::removeHandler));
        return p;
    }

    private static String ticker(final JsonObject context) {
        return context.getJsonObject("id").getString("ticker");
    }
}
