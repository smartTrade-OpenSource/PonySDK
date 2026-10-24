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

        final PButton button = Element.newPButton("Click me");
        final PElement buttonEcho = echoIdle("server: not clicked yet");
        final AtomicInteger clicks = new AtomicInteger();
        button.addClickHandler(e -> {
            button.setText("Clicked!");
            echoSet(buttonEcho, "server: handled click #" + clicks.incrementAndGet() + " in a Java lambda");
        });
        p.add(exampleCard("A button and its handler",
            "The widget is a Java object on the server; the click handler is a Java lambda. Only the new text travels back.",
            previewWithEcho(button, buttonEcho), snippet_helloWorld()));

        final PLabel counter = Element.newPLabel("0 updates");
        counter.addStyleName("code-counter");
        final AtomicInteger count = new AtomicInteger();
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            counter.setText(count.incrementAndGet() + " updates");
        }, Duration.ofMillis(100)));
        p.add(exampleCard("Push from the server",
            "A server task updates the label ten times a second: no polling, no request from the browser, about 10 bytes per update.",
            counter, snippet_liveCounter()));

        final PWidget grid = smallDataGrid();
        grid.setWidth("100%");
        grid.setHeight("320px");
        p.add(exampleCard("A data grid whose data stays on the server",
            "Columns and rows are defined in Java; the browser only receives the rows it shows. Scroll, sort and resize are "
                + "handled on the server.",
            grid, snippet_datagrid()));

        p.add(highlightBox("", "What travels on the wire",
            "One label.setText(...) is 13 bytes on the wire protocol (26 as a JSON message); in a tick of many cells, update "
                + "runs, the string dictionary and deflate bring it to about 5 bytes per cell. The Binary protocol page shows "
                + "the bytes of an update, field by field."));
        return p;
    }

    static String snippet_helloWorld() {
        return ln(kw("public class") + " " + ty("MyApp") + " " + kw("implements") + " " + ty("EntryPoint") + " {") +
            ln("") +
            ln("  " + kw("@Override")) +
            ln("  " + kw("public void") + " " + mt("start") + "(" + ty("UIContext") + " ctx) {") +
            ln("    " + ty("PButton") + " button = " + ty("Element") + "." + mt("newPButton") + "(" + st("Click me") + ");") +
            ln("    " + cm("runs on the server, in a Java lambda")) +
            ln("    button." + mt("addClickHandler") + "(e -> button." + mt("setText") + "(" + st("Clicked!") + "));") +
            ln("    " + ty("PWindow") + "." + mt("getMain") + "()." + mt("add") + "(button);") +
            ln("  }") +
            ln("}");
    }

    static String snippet_liveCounter() {
        return ln(ty("PLabel") + " counter = " + ty("Element") + "." + mt("newPLabel") + "(" + st("0 updates") + ");") +
            ln(ty("AtomicInteger") + " count = " + kw("new") + " " + ty("AtomicInteger") + "();") +
            ln("") +
            ln(cm("a server task: about 10 bytes on the wire per update")) +
            ln(ty("PScheduler") + "." + mt("scheduleAtFixedRate") + "(() ->") +
            ln("    counter." + mt("setText") + "(count." + mt("incrementAndGet") + "() + " + st(" updates") + "),") +
            ln("    " + ty("Duration") + "." + mt("ofMillis") + "(" + nu("100") + "));");
    }

    static String snippet_datagrid() {
        return ln(ty("List") + "&lt;" + ty("ColumnDefinition") + "&lt;" + ty("Row") + "&gt;&gt; columns = " + kw("new") + " " + ty("ArrayList") + "&lt;&gt;();") +
            ln(kw("for") + " (" + kw("char") + " c = " + st("a").replace("\"", "'") + "; c &lt;= " + st("h").replace("\"", "'") + "; c++) {") +
            ln("  " + ty("String") + " key = " + ty("String") + "." + mt("valueOf") + "(c);") +
            ln("  columns." + mt("add") + "(" + kw("new") + " " + ty("DefaultColumnDefinition") + "&lt;&gt;(key, row -> row." + mt("getValue") + "(key),") +
            ln("      (row, value) -> row." + mt("putValue") + "(key, value)));") +
            ln("}") +
            ln(ty("DefaultDataGridView") + "&lt;" + ty("Integer") + ", " + ty("Row") + "&gt; grid = " + kw("new") + " " + ty("DefaultDataGridView") + "&lt;&gt;();") +
            ln("grid." + mt("setAdapter") + "(" + kw("new") + " " + ty("RowAdapter") + "(columns)); " + cm("row key, header...")) +
            ln("") +
            ln(cm("the data stays on the server; the browser receives the rows it shows")) +
            ln(kw("for") + " (" + kw("int") + " i = " + nu("0") + "; i &lt; " + nu("50") + "; i++) grid." + mt("getController") + "()." + mt("setData") + "(" + kw("new") + " " + ty("Row") + "(i));");
    }
}
