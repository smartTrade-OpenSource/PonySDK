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
import java.util.concurrent.atomic.AtomicInteger;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PLabel;

/** Code: the Java behind a PonySDK screen. */
final class CodePage {

    private CodePage() {
    }

    // ── Tab: Code ─────────────────────────────────────────────────────────────

    /** Each card runs the code it shows: Preview is that code, live; Code is its source. */
    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Code", "A PonySDK screen is plain Java on the server: widgets, handlers, pushes and data. Each card "
            + "below runs the code it shows.");

        // region button
        final PButton button = Element.newPButton("Click me");
        final PElement buttonEcho = echoIdle("server: not clicked yet");
        final AtomicInteger clicks = new AtomicInteger();
        // runs on the server, in a Java lambda: only the new texts travel back
        button.addClickHandler(e -> {
            button.setText("Clicked!");
            echoSet(buttonEcho, "server: handled click #" + clicks.incrementAndGet() + " in a Java lambda");
        });
        // endregion
        p.add(exampleCard("A button and its handler",
            "The widget is a Java object on the server; the click handler is a Java lambda. Only the new text travels back. "
                + "A whole application around it: README.md, Your first application.",
            previewWithEcho(button, buttonEcho), Source.code(CodePage.class, "button")));

        // region counter
        final PLabel counter = Element.newPLabel("0 updates");
        counter.addStyleName("code-counter");
        final AtomicInteger count = new AtomicInteger();
        // a server task: about 10 bytes on the wire per update
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return; // nobody is looking: no update
            counter.setText(count.incrementAndGet() + " updates");
        }, Duration.ofMillis(100)));
        // endregion
        p.add(exampleCard("Push from the server",
            "A server task updates the label ten times a second: no polling, no request from the browser, about 10 bytes per update.",
            counter, Source.code(CodePage.class, "counter")));

        final PWidget grid = smallDataGrid();
        grid.setWidth("100%");
        grid.setHeight("320px");
        p.add(exampleCard("A data grid whose data stays on the server",
            "Columns and rows are defined in Java; the browser only receives the rows it shows. Scroll, sort and resize are "
                + "handled on the server.",
            grid, Source.code(Costs.class, "datagrid")));

        p.add(highlightBox("", "What travels on the wire",
            "One label.setText(...) is 13 bytes on the wire protocol; in a tick of many cells, update runs, the string "
                + "dictionary and deflate bring it to about 5 bytes per cell. The Binary protocol page measures the bytes of "
                + "an update, field by field, and the same update as JSON."));
        return p;
    }
}
