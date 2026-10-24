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

import java.time.Duration;
import java.util.List;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAbsolutePanel;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PFocusPanel;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PTabLayoutPanel;
import com.ponysdk.core.ui.basic.PWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Layouts: containers built and filled from Java. */
final class LayoutsPage {

    private static final Logger log = LoggerFactory.getLogger(LayoutsPage.class);

    private LayoutsPage() {
    }

    // ── Tab: Layouts ─────────────────────────────────────────────────────────

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Layouts", "Containers built and filled from Java. Every box below is a server-side panel.");

        p.add(sectionTitle("Panels"));
        final PFlowPanel flow = Element.newPFlowPanel();
        flow.add(chip("A"), chip("B"), chip("C"));
        p.add(labeled("FlowPanel", flow));
        final PHorizontalPanel h = Element.newPHorizontalPanel();
        h.add(chip("Left"), chip("Middle"), chip("Right"));
        p.add(labeled("HorizontalPanel", h));
        final PVerticalPanel v = Element.newPVerticalPanel();
        v.add(chip("Top"), chip("Middle"), chip("Bottom"));
        p.add(labeled("VerticalPanel", v));
        final PAbsolutePanel abs = Element.newPAbsolutePanel();
        abs.setHeight("120px");
        abs.setWidth("min(560px,100%)");
        abs.addStyleName("lay-frame");
        final String[][] spots = { { "x 20, y 16", "20px", "16px" }, { "x 210, y 60", "210px", "60px" }, { "x 400, y 28", "400px", "28px" } };
        for (final String[] spot : spots) {
            final PLabel c = chip(spot[0]);
            c.setStyleProperty("position", "absolute");
            c.setStyleProperty("left", spot[1]);
            c.setStyleProperty("top", spot[2]);
            abs.add(c);
        }
        p.add(labeled("AbsolutePanel", abs));
        final PScrollPanel scroll = Element.newPScrollPanel();
        scroll.setHeight("110px");
        scroll.setWidth("320px");
        scroll.addStyleName("lay-frame");
        final PFlowPanel lines = Element.newPFlowPanel();
        for (int i = 1; i <= 20; i++) lines.add(Element.newPLabel("Line " + i + " — scroll me"));
        scroll.setWidget(lines);
        p.add(labeled("ScrollPanel", scroll));
        final PSimplePanel simple = Element.newPSimplePanel();
        simple.setWidget(chip("Exactly one child"));
        p.add(labeled("SimplePanel", simple));
        final PFocusPanel focus = Element.newPFocusPanel();
        final PLabel focusLbl = Element.newPLabel("Click me: the click is handled in Java");
        focusLbl.addStyleName("lay-chip");
        focus.setWidget(focusLbl);
        final int[] clicks = { 0 };
        focus.addClickHandler(e -> focusLbl.setText("Clicked " + ++clicks[0] + " time" + (clicks[0] > 1 ? "s" : "")));
        p.add(labeled("FocusPanel", focus));

        p.add(sectionTitle("Layout panels"));
        final PDockLayoutPanel dock = Element.newPDockLayoutPanel(PUnit.PX);
        dock.addNorth(chip("North"), 34);
        dock.addSouth(chip("South"), 34);
        dock.addWest(chip("West"), 90);
        dock.add(chip("Center"));
        p.add(labeled("DockLayoutPanel", sized(dock)));
        final PSplitLayoutPanel split = Element.newPSplitLayoutPanel();
        split.addWest(chip("Drag the splitter →"), 170);
        split.add(chip("Center"));
        p.add(labeled("SplitLayoutPanel", sized(split)));
        final PStackLayoutPanel stack = Element.newPStackLayoutPanel(PUnit.PX);
        stack.add(chip("Mail section"), "Mail", false, 32);
        stack.add(chip("Contacts section"), "Contacts", false, 32);
        stack.add(chip("Tasks section"), "Tasks", false, 32);
        p.add(labeled("StackLayoutPanel", sized(stack)));
        final PTabLayoutPanel tabs = Element.newPTabLayoutPanel();
        tabs.add(chip("First tab content"), "First");
        tabs.add(chip("Second tab content"), "Second");
        tabs.add(chip("Third tab content"), "Third");
        tabs.selectTab(0);
        p.add(labeled("TabLayoutPanel", sized(tabs)));

        p.add(sectionTitle("Disclosure / popups"));
        final PDisclosurePanel disclosure = Element.newPDisclosurePanel("Click to expand");
        disclosure.setContent(chip("Hidden content, sent once and toggled client-side"));
        p.add(labeled("DisclosurePanel", disclosure));
        final PButton openDialog = Element.newPButton("Open dialog");
        openDialog.addClickHandler(e -> {
            final PDialogBox dialog = Element.newPDialogBox(true);
            dialog.setCaption("A PDialogBox");
            final PFlowPanel body = Element.newPFlowPanel();
            body.addStyleName("lay-dialog");
            body.add(Element.newPLabel("Created on the server when you clicked."));
            final PButton close = Element.newPButton("Close");
            close.addClickHandler(ev -> dialog.hide());
            body.add(close);
            dialog.setWidget(body);
            dialog.setGlassEnabled(true);
            PWindow.getMain().add(dialog); // created when attached to its window, then centred
            dialog.center();
        });
        p.add(labeled("DialogBox", openDialog));
        final PButton openPopup = Element.newPButton("Show popup");
        openPopup.addClickHandler(e -> {
            final com.ponysdk.core.ui.basic.PDecoratedPopupPanel popup = Element.newPDecoratedPopupPanel(true);
            popup.setWidget(chip("Auto-hide popup: click anywhere to close"));
            PWindow.getMain().add(popup);
            popup.center();
        });
        p.add(labeled("DecoratedPopupPanel", openPopup));

        p.add(sectionTitle("Visibility handler"));
        testVisibilityHandler(p, schedulers);

        return p;
    }

    static void testVisibilityHandler(final PComplexPanel container, final List<PScheduler.UIRunnable> schedulers) {
        final int[] counter = { 0 };
        final PFlowPanel box = Element.newPFlowPanel();
        box.addStyleName("lay-vis");
        container.add(box);
        final PLabel hint = Element.newPLabel("Scroll the frame: the server is told when the card leaves or "
            + "enters the viewport, and only refreshes it while it is visible.");
        hint.addStyleName("lay-hint");
        box.add(hint);
        final PLabel liveVisibility = Element.newPLabel("Live Visibility : Unknown");
        liveVisibility.addStyleName("pony-ex-echo");
        box.add(liveVisibility);

        final PButton button = Element.newPButton("Check visibility");
        box.add(button);

        final PLabel visibilityLabel = Element.newPLabel("Visibility : Unknown");
        visibilityLabel.addStyleName("lay-hint");
        box.add(visibilityLabel);

        final PScrollPanel frame = Element.newPScrollPanel();
        frame.setHeight("200px");
        frame.setWidth("320px");
        frame.addStyleName("lay-frame");
        box.add(frame);

        final PFlowPanel panel = Element.newPFlowPanel();
        panel.setHeight("2000px");
        frame.add(panel);

        final PFlowPanel subPanel = Element.newPFlowPanel();
        subPanel.addStyleName("lay-vis-card");
        subPanel.setHeight("125px");
        subPanel.setWidth("260px");
        panel.add(subPanel);

        final PLabel label = Element.newPLabel("Server counter: " + counter[0]++);
        subPanel.add(label);

        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            counter[0]++;
            if (subPanel.isShown() && subPanel.getWindow() != null && subPanel.getWindow().isShown()) updateLabel(label, String.valueOf(counter[0]));
        }, Duration.ofSeconds(1)));

        subPanel.addVisibilityHandler(event -> {
            if (event.getData() && subPanel.getWindow() != null) {
                subPanel.getWindow().addVisibilityHandler(windowEvent -> {
                    if (windowEvent.getData()) {
                        log.debug("Force refresh, because window became visible");
                        updateLabel(label, String.valueOf(counter[0]));
                    } else {
                        log.debug("Window became not visible");
                    }
                });
            }
        });

        liveVisibility.setText("Live Visibility : " + subPanel.isShown());
        visibilityLabel.setText("Visibility : " + subPanel.isShown());
        subPanel.addVisibilityHandler(event -> {
            liveVisibility.setText("Live Visibility : " + event.getData());
            if (event.getData()) {
                log.debug("Force refresh, because panel became visible");
                updateLabel(label, String.valueOf(counter[0]));
            }
        });
        button.addClickHandler(event -> {
            visibilityLabel.setText("Visibility : " + subPanel.isShown());
        });
    }
}
