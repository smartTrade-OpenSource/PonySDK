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
import java.util.concurrent.atomic.AtomicInteger;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;
import com.ponysdk.core.ui.main.EntryPoint;

/** Code: the Java behind a PonySDK screen. */
final class CodePage {

    private CodePage() {
    }

    // ── Tab: Code ─────────────────────────────────────────────────────────────

    static PFlowPanel build() {
        final PFlowPanel p = section();
        pageTitle(p, "Code", "PonySDK lets you build rich UIs in pure Java — no JavaScript, no REST, no serialization.");

        p.add(highlightBox("💡", "Server-side rendering, client-side performance",
            "Your entire UI logic runs on the JVM. PonySDK sends only binary deltas over WebSocket — " +
            "typically 5–20 bytes per update. The browser renders natively, you code in Java."));

        p.add(sectionTitle("Hello World — server-side button"));
        p.add(codeSnippet("EntryPoint.java", "JAVA", snippet_helloWorld()));

        p.add(sectionTitle("Real-time push — server → browser"));
        p.add(codeSnippet("LiveCounter.java", "JAVA", snippet_liveCounter()));

        p.add(sectionTitle("DataGrid — 10 000 rows, virtual rendering"));
        p.add(codeSnippet("DataGridExample.java", "JAVA", snippet_datagrid()));

        p.add(sectionTitle("What travels on the wire"));
        p.add(highlightBox("📊", "Binary delta protocol with dictionary compression",
            "First send of 'Hello': ~7 bytes + dict entry. " +
            "Subsequent sends: 2 bytes (dict index). " +
            "Result: 80–95% bandwidth reduction vs REST/JSON APIs. " +
            "A full DataGrid refresh of 10 000 rows costs less than a single REST response."));
        p.add(codeSnippet("protocol.txt", "BINARY", snippet_protocol()));

        return p;
    }

    static String snippet_helloWorld() {
        return ln(kw("public class") + " " + ty("MyApp") + " " + kw("implements") + " " + ty("EntryPoint") + " {") +
            ln("") +
            ln("  " + kw("@Override")) +
            ln("  " + kw("public void") + " " + mt("start") + "(" + ty("UIContext") + " ctx) {") +
            ln("    " + cm("Create a button — runs on the server")) +
            ln("    " + ty("PButton") + " btn = " + ty("Element") + "." + mt("newPButton") + "(" + st("Click me") + ");") +
            ln("") +
            ln("    " + cm("Click handler — pure Java lambda")) +
            ln("    btn." + mt("addClickHandler") + "(e -> btn." + mt("setText") + "(" + st("Clicked!") + "));") +
            ln("") +
            ln("    " + cm("Add to main window — delta sent over WebSocket")) +
            ln("    " + ty("PWindow") + "." + mt("getMain") + "()." + mt("add") + "(btn);") +
            ln("    " + cm("No JS. No REST. No serialization. Just Java.")) +
            ln("  }") +
            ln("}");
    }

    static String snippet_liveCounter() {
        return ln(cm("Server pushes updates every 100ms — about 10 bytes per update")) +
            ln(ty("PLabel") + " label = " + ty("Element") + "." + mt("newPLabel") + "(" + st("0") + ");") +
            ln(ty("PWindow") + "." + mt("getMain") + "()." + mt("add") + "(label);") +
            ln("") +
            ln(ty("AtomicInteger") + " count = " + kw("new") + " " + ty("AtomicInteger") + "();") +
            ln(ty("PScheduler") + "." + mt("scheduleAtFixedRate") + "(() -> {") +
            ln("  " + cm("Only the changed text travels over WebSocket")) +
            ln("  label." + mt("setText") + "(count." + mt("incrementAndGet") + "() + " + st(" updates") + ");") +
            ln("}, " + ty("Duration") + "." + mt("ofMillis") + "(100));") +
            ln("") +
            ln(cm("10 updates/sec × 5 bytes = 50 bytes/sec per session")) +
            ln(cm("1000 concurrent users = 50 KB/s total — less than a single image"));
    }

    static String snippet_datagrid() {
        return ln(cm("10 000 rows × 52 columns — all server-side")) +
            ln(ty("DefaultDataGridView") + "&lt;" + ty("Integer") + ", " + ty("Row") + "&gt; grid") +
            ln("    = " + kw("new") + " " + ty("DefaultDataGridView") + "&lt;&gt;();") +
            ln(ty("DataGridController") + "&lt;" + ty("Integer") + ", " + ty("Row") + "&gt; ctrl") +
            ln("    = grid." + mt("getController") + "();") +
            ln("") +
            ln(cm("Load 10k rows — only visible rows are rendered in browser")) +
            ln(kw("for") + " (" + kw("int") + " i = 0; i &lt; 10_000; i++)") +
            ln("  ctrl." + mt("setData") + "(" + mt("createRow") + "(i));") +
            ln("") +
            ln(cm("Server-side filter — zero data leaves the server")) +
            ln("ctrl." + mt("setFilter") + "(MyFilter.class,") +
            ln("  row -> row." + mt("getName") + "()." + mt("startsWith") + "(" + st("A") + "));") +
            ln("") +
            ln(cm("Server-side sort — no client memory used")) +
            ln("ctrl." + mt("sort") + "(col, " + ty("SortOrder") + ".ASCENDING, " + kw("false") + ");");
    }

    static String snippet_protocol() {
        return ln(cm("Traditional REST/JSON approach:")) +
            ln(st("POST /api/widget/42  {\"text\": \"Hello\"}")) +
            ln(st("→ ~200 bytes (HTTP headers + JSON body)")) +
            ln("") +
            ln(cm("PonySDK binary delta protocol:")) +
            ln(st("[objectId=42][MODEL_TEXT=\"Hello\"]  → ~10 bytes, WebSocket frame")) +
            ln("") +
            ln(cm("String dictionary compression (learned across sessions):")) +
            ln(st("1st send:  \"Hello\" → dict[42]   (stores mapping, 7 bytes)")) +
            ln(st("Next:      dict[42]              → 2 bytes instead of 7")) +
            ln("") +
            ln(cm("Typical savings: 80–95% vs REST APIs")) +
            ln(cm("10 000-row DataGrid refresh: ~2 KB vs ~800 KB JSON"));
    }
}
