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


import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PCommands;
import com.ponysdk.core.ui.basic.PEventOptions;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * Client commands (docs/decisions/0027-client-commands.md): a panel shown and hidden by a round trip to the server,
 * and the same panel by commands the terminal runs on the click. A checkbox delays the server's answers, as a slow
 * network would: the commands do not wait for it.
 */
final class ClientCommandsPage {

    /** What a slow network adds to each answer of the server. */
    private static final Duration DELAY = Duration.ofMillis(800);

    private ClientCommandsPage() {
    }

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Client commands", "A click can show a panel, toggle a class, take the focus, with no round trip: the "
                + "terminal runs named commands the server declared, then tells the server the click happened if it is asked "
                + "to. Slow the server down and compare.");

        final PCheckBox slow = Element.newPCheckBox("Slow network: the server answers after " + DELAY.toMillis() + " ms");
        slow.addStyleName("cmd-slow");
        slow.setValue(true);
        // What the server does in answer to an event, late when the network is slow
        final Consumer<Runnable> answer = reply -> {
            if (slow.getValue()) schedulers.add(PScheduler.schedule(reply, DELAY));
            else reply.run();
        };

        // region server
        // The click goes to the server, which hides the panel: its answer is what the user waits for
        final PFlowPanel serverPanel = panel("Shown and hidden by the server");
        final PButton serverToggle = Element.newPButton("Toggle the panel");
        serverToggle.addClickHandler(e -> answer.accept(() -> serverPanel.setVisible(!serverPanel.isVisible())));
        // endregion

        // region client
        // The terminal toggles the panel and a class of the button, and focuses the box, on the click itself:
        // before the click is sent, and whatever the network
        final PFlowPanel clientPanel = panel("Shown and hidden by the terminal");
        final PTextBox note = Element.newPTextBox();
        note.setAttribute("aria-label", "A note, focused by the click");
        final PButton clientToggle = Element.newPButton("Toggle the panel");
        final PLabel seen = Element.newPLabel("The server has seen no click");
        seen.addStyleName("cmd-seen");
        final int[] clicks = { 0 };
        clientToggle.addEventListener("click",
            PEventOptions.NONE.commands(PCommands.NONE.toggle(clientPanel).toggleClass(clientToggle, "cmd-open").focus(note)),
            e -> answer.accept(() -> seen.setText("The server has seen " + (++clicks[0]) + " click(s)")));
        // endregion

        // region only
        // Without a server listener the event is not sent at all: nothing reaches the server
        final PLabel pill = Element.newPLabel("Hover the button: no message goes out");
        pill.addStyleName("cmd-pill");
        final PButton hint = Element.newPButton("A hint, purely client side");
        hint.addEventListener("mouseenter", PEventOptions.NONE.commands(PCommands.NONE.addClass(pill, "cmd-on")));
        hint.addEventListener("mouseleave", PEventOptions.NONE.commands(PCommands.NONE.removeClass(pill, "cmd-on")));
        // endregion

        // Nothing but this array holds the panel once the page is built: removed, it is collected, and its id goes
        final PFlowPanel[] held = { clientPanel };
        final PLabel server = Element.newPLabel("Ask the server what it knows of the panel");
        server.addStyleName("cmd-knows");
        final PButton ask = Element.newPButton("What does the server see?");
        ask.addClickHandler(e -> server.setText(held[0] == null ? "The panel is gone"
                : "The server thinks the panel is " + (held[0].isVisible() ? "visible" : "hidden")
                        + ": the browser's state is not the server's"));

        // A target the server collected is skipped by the terminal: the other commands run, nothing is reported
        final PLabel collected = Element.newPLabel();
        collected.addStyleName("cmd-collected");
        final WeakReference<PFlowPanel> weak = new WeakReference<>(clientPanel);
        final PButton remove = Element.newPButton("Remove the panel");
        remove.addClickHandler(e -> {
            if (held[0] != null) held[0].removeFromParent();
            held[0] = null;
            System.gc();
            collected.setText(weak.get() == null ? "The panel is collected" : "The panel is removed, not collected yet");
        });

        final PFlowPanel left = card("Through the server", serverToggle, serverPanel);
        left.addStyleName("cmd-server");
        final PFlowPanel right = card("A client command", clientToggle, clientPanel, field(note), seen, ask, server, remove, collected);
        right.addStyleName("cmd-client");
        final PFlowPanel columns = Element.newPFlowPanel();
        columns.addStyleName("cmd-columns");
        columns.add(left, right);

        p.add(slow, columns,
            codeToggle("through the server", Source.code(ClientCommandsPage.class, "server")),
            codeToggle("the client command", Source.code(ClientCommandsPage.class, "client")),
            sectionTitle("Commands without the server"), hint, pill,
            codeToggle("commands alone", Source.code(ClientCommandsPage.class, "only")));
        return p;
    }

    private static PFlowPanel panel(final String text) {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.addStyleName("cmd-panel");
        panel.add(Element.newPLabel(text));
        return panel;
    }

    private static PFlowPanel field(final PTextBox box) {
        final PFlowPanel field = Element.newPFlowPanel();
        field.addStyleName("cmd-field");
        field.add(box);
        return field;
    }

    private static PFlowPanel card(final String title, final PWidget... widgets) {
        final PFlowPanel card = Element.newPFlowPanel();
        card.addStyleName("cmd-card");
        card.add(sectionTitle(title));
        for (final PWidget widget : widgets) card.add(widget);
        return card;
    }
}
