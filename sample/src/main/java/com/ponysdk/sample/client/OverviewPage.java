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
import java.util.ArrayList;
import java.util.List;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.sample.quickstart.LivePrice;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

/** Overview: hero, measured comparison with 2.10, live ticker and its encoding pipeline. */
final class OverviewPage {

    private OverviewPage() {
    }

    // ── Tab: Overview ────────────────────────────────────────────────────────

    /** One measured row of the 2.10 vs 3.0 comparison. */
    record Measure(String metric, String gain, String factor, String before, String after, int afterPct, String hint) {}

    /** @param navigate opens a page of the showcase, by its name in the sidebar */
    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers,
                                      final java.util.function.Consumer<String> navigate) {
        final PFlowPanel p = section();

        // Hero
        final PFlowPanel hero = Element.newPFlowPanel();
        hero.addStyleName("ov-hero");
        final PFlowPanel heroText = Element.newPFlowPanel();
        heroText.addStyleName("ov-hero-text");
        heroText.add(el("ov-eyebrow", "PonySDK 3 · open source · Apache 2.0"));
        heroText.add(el("ov-h1", "Real-time web UIs, written only in Java."));
        heroText.add(el("ov-lead", "Every widget lives on the server. The browser is a thin renderer that receives "
            + "compact binary instructions over one WebSocket: no REST layer, no JSON, no JavaScript to write."));
        final PFlowPanel ctas = Element.newPFlowPanel();
        ctas.addStyleName("ov-ctas");
        final PButton tryIt = Element.newPButton("Try the widgets");
        tryIt.addStyleName("primary");
        tryIt.addClickHandler(e -> navigate.accept("Inputs"));
        final PButton protocol = Element.newPButton("See the protocol");
        protocol.addClickHandler(e -> navigate.accept("Binary protocol"));
        final PButton costs = Element.newPButton("What a widget costs");
        costs.addClickHandler(e -> navigate.accept("Component costs"));
        ctas.add(tryIt, protocol, costs);
        heroText.add(ctas);
        hero.add(heroText);

        // The whole application, next to the pitch: a live price, server-side
        final PFlowPanel heroCode = Element.newPFlowPanel();
        heroCode.addStyleName("code-snippet");
        heroCode.addStyleName("ov-hero-code");
        final PFlowPanel heroCodeHead = Element.newPFlowPanel();
        heroCodeHead.addStyleName("code-header");
        heroCodeHead.add(el("code-title", "LivePrice.java — the whole application"), el("code-lang", "JAVA"));
        final PElement heroCodeBody = Element.newDiv();
        heroCodeBody.addStyleName("code-body");
        heroCodeBody.setTabindex(TabindexMode.TABULABLE); // a keyboard user scrolls what overflows
        heroCodeBody.setInnerHTML(Source.code(LivePrice.class, "app")); // sample/.../quickstart/LivePrice.java, as it runs
        heroCode.add(heroCodeHead, heroCodeBody);
        hero.add(heroCode);
        final PFlowPanel chips = Element.newPFlowPanel();
        chips.addStyleName("ov-chips");
        for (final String c : new String[] { "Binary WebSocket protocol", "Server-side state", "String dictionary",
                                             "Update coalescing", "Transparent reconnection", "Web Components" }) {
            chips.add(el("ov-chip", c));
        }
        heroText.add(chips);
        p.add(hero);

        // Measured comparison
        p.add(sectionTitle("Measured — PonySDK 3 against 2.10"));
        final PFlowPanel legend = Element.newPFlowPanel();
        legend.addStyleName("ov-legend");
        legend.add(el("ov-legend-old", "2.10"), el("ov-legend-new", "3.0"), el("ov-legend-text",
            "Same benchmark, same machine and JDK 25 for both versions. Lower is better on every metric."));
        p.add(legend);
        final PFlowPanel kpis = Element.newPFlowPanel();
        kpis.addStyleName("ov-kpis");
        for (final Measure m : new Measure[] {
            new Measure("CPU per UI update", "−37 %", "1.6× less", "2 280 ns", "1 440 ns", 63, "Server time to apply and encode one update"),
            new Measure("Bandwidth on the wire", "−50 %", "2.0× less", "1.02 MB/s", "0.51 MB/s", 50, "Same updates, bytes on the TCP stream"),
            new Measure("Allocation per update", "−83 %", "6.0× less", "42 B", "7 B", 17, "Garbage produced per update"),
            new Measure("Process memory (RSS)", "−54 %", "2.2× less", "410 MB", "190 MB", 46, "Whole JVM, 200 live sessions"),
            new Measure("Retained heap", "−18 %", "1.2× less", "26.3 MB", "21.7 MB", 82, "Live objects after a full GC, same benchmark") }) {
            final PFlowPanel k = Element.newPFlowPanel();
            k.addStyleName("ov-kpi");
            k.add(el("ov-kpi-metric", m.metric()));
            final PFlowPanel head = Element.newPFlowPanel();
            head.addStyleName("ov-kpi-head");
            head.add(el("ov-kpi-gain", m.gain()), el("ov-kpi-factor", m.factor()));
            k.add(head);
            k.add(compareRow("2.10", m.before(), 100, false));
            k.add(compareRow("3.0", m.after(), m.afterPct(), true));
            k.add(el("ov-kpi-hint", m.hint()));
            kpis.add(k);
        }
        p.add(kpis);
        p.add(el("ov-note", "Benchmark: 200 sessions, each a 200-cell price grid receiving 130 updates every 100 ms "
            + "(260 000 updates/s). Mean of three runs per version, values rounded."));

        // Live metrics of this very session
        p.add(sectionTitle("Live — this session, right now"));
        final PFlowPanel live = Element.newPFlowPanel();
        live.addStyleName("perf-grid");
        live.addStyleName("ov-live");
        final PElement wireVal = el("perf-value", "—");
        final PElement rawVal = el("perf-value", "—");
        final PElement ratioVal = el("perf-value", "—");
        final PElement perUpdVal = el("perf-value", "—");
        final PElement updVal = el("perf-value", "0");
        final PElement latVal = el("perf-value", "—");
        live.add(perfCard(wireVal, el("perf-unit", "KB/s on the wire"), el("perf-label", "Downstream")));
        live.add(perfCard(rawVal, el("perf-unit", "KB/s before deflate"), el("perf-label", "Binary protocol")));
        live.add(perfCard(ratioVal, el("perf-unit", "× smaller"), el("perf-label", "Compression")));
        live.add(perfCard(updVal, el("perf-unit", "updates / s"), el("perf-label", "Ticker load")));
        live.add(perfCard(perUpdVal, el("perf-unit", "bytes / update"), el("perf-label", "Wire cost")));
        live.add(perfCard(latVal, el("perf-unit", "ms roundtrip"), el("perf-label",
            "Latency · protocol v" + uiContext.getProtocolVersion())));
        p.add(live);

        // Live pipeline: how a setter call becomes a few bytes on the wire
        p.add(sectionTitle("How it gets so small — the update pipeline, live"));
        final PFlowPanel flow = Element.newPFlowPanel();
        flow.addStyleName("pl-flow");
        for (final String[] step : new String[][] {
            { "Java setter", "label.setText(…)" }, { "Coalescing", "last value wins" },
            { "Update runs", "ids chained" }, { "Dictionary", "3-byte refs" },
            { "Typed binary", "no JSON" }, { "Deflate", "zlib stream" }, { "WebSocket", "one frame / tick" },
            { "Terminal", "TypeScript → DOM" } }) {
            final PFlowPanel box = Element.newPFlowPanel();
            box.addStyleName("pl-step");
            box.add(el("pl-step-name", step[0]), el("pl-step-sub", step[1]));
            flow.add(box);
        }
        p.add(flow);

        final PFlowPanel funnel = Element.newPFlowPanel();
        funnel.addStyleName("pl-funnel");
        final String[][] stages = {
            { "Setter calls", "calls / s", "The application calls setters freely, several times per cell and per tick." },
            { "After coalescing", "updates / s", "Within one transaction only the last value of each property is kept." },
            { "As JSON", "KB / s", "What the same updates would weigh as JSON messages ({\"id\":…,\"text\":…})." },
            { "PonySDK binary", "KB / s", "Typed binary, chained update runs, dictionary references: exact protocol bytes." },
            { "On the wire", "KB / s", "After permessage-deflate with context takeover: bytes on the socket." } };
        final PElement[] stageVal = new PElement[stages.length];
        final PElement[] stageFill = new PElement[stages.length];
        final PElement[] stageGain = new PElement[stages.length];
        for (int k = 0; k < stages.length; k++) {
            final PFlowPanel row = Element.newPFlowPanel();
            row.addStyleName("pl-row");
            if (k >= 2) row.addStyleName("pl-row-bytes");
            final PFlowPanel head = Element.newPFlowPanel();
            head.addStyleName("pl-row-head");
            stageVal[k] = el("pl-val", "—");
            stageGain[k] = el("pl-gain", "");
            head.add(el("pl-name", stages[k][0]), stageVal[k], el("pl-unit", stages[k][1]), stageGain[k]);
            final PElement bar = Element.newDiv();
            bar.addStyleName("pl-bar");
            stageFill[k] = Element.newDiv();
            stageFill[k].addStyleName("pl-bar-fill");
            stageFill[k].setAttribute("style", "width:0%");
            bar.add(stageFill[k]);
            row.add(head, bar, el("pl-desc", stages[k][2]));
            funnel.add(row);
        }
        p.add(funnel);

        // Stress ticker: the server updates random cells; the cards above show what it costs
        p.add(sectionTitle("Stress it — a live price ticker driven from Java"));
        final PFlowPanel controls = Element.newPFlowPanel();
        controls.addStyleName("ov-controls");
        final PFlowPanel grid = Element.newPFlowPanel();
        grid.addStyleName("ov-ticker");
        grid.setTabindex(TabindexMode.TABULABLE); // a keyboard user scrolls what overflows

        final java.util.Random rnd = new java.util.Random();
        final List<PLabel> cells = new ArrayList<>();
        final double[] prices = new double[1000];
        final int[] cellCount = { 240 };
        final int[] rate = { 20 };          // ticks per second
        final boolean[] running = { true };
        final long[] updates = { 0 };
        final long[] calls = { 0 };      // setter calls made by the application
        final long[] jsonBytes = { 0 };  // what the same distinct updates would weigh as JSON messages
        final PScheduler.UIRunnable[] ticker = { null };

        final Runnable rebuildCells = () -> {
            grid.clear();
            cells.clear();
            for (int i = 0; i < cellCount[0]; i++) {
                prices[i] = 1.05 + rnd.nextDouble() * 0.2;
                final PLabel c = Element.newPLabel(String.format(java.util.Locale.ROOT, "%.5f", prices[i]));
                c.addStyleName("ov-cell");
                cells.add(c);
                grid.add(c);
            }
        };
        final boolean[] touched = new boolean[1000];
        final Runnable tick = () -> {
            if (idle()) return;
            final int n = Math.max(1, cells.size() / 4);   // a quarter of the cells move each tick
            java.util.Arrays.fill(touched, false);
            for (int k = 0; k < n; k++) {
                final int i = rnd.nextInt(cells.size());
                final double delta = (rnd.nextDouble() - 0.5) * 0.002;
                prices[i] = Math.max(0.5, prices[i] + delta);
                final PLabel c = cells.get(i);
                final String text = String.format(java.util.Locale.ROOT, "%.5f", prices[i]);
                final String color = delta >= 0 ? "var(--good)" : "var(--bad)";
                c.setText(text);
                c.setStyleProperty("color", color);
                calls[0] += 2;
                if (!touched[i]) {
                    updates[0] += 2;                        // text + colour; a cell hit twice in a tick is sent once
                    // {"id":1234,"text":"1.08512"} and {"id":1234,"style":{"color":"#43e8b0"}}
                    final int idLen = String.valueOf(c.getID()).length();
                    jsonBytes[0] += 17 + idLen + text.length() + 28 + idLen + color.length();
                }
                touched[i] = true;
            }
        };
        final Runnable restartTicker = () -> {
            if (ticker[0] != null) ticker[0].cancel();
            ticker[0] = null;
            if (!running[0]) return;
            ticker[0] = PScheduler.scheduleAtFixedRate(tick, Duration.ofMillis(1000 / rate[0]));
            schedulers.add(ticker[0]);
        };

        final PButton runBtn = Element.newPButton("Pause");
        runBtn.addClickHandler(e -> {
            running[0] = !running[0];
            runBtn.setText(running[0] ? "Pause" : "Run");
            styleBinaryCtrl(runBtn, running[0]);
            restartTicker.run();
        });
        styleBinaryCtrl(runBtn, true);
        final PFlowPanel runRow = Element.newPFlowPanel();
        runRow.add(runBtn);
        controls.add(binaryCtrlGroup("Stream", runRow));

        final int[] sizes = { 120, 240, 600, 1000 };
        final PButton[] sizeBtns = new PButton[sizes.length];
        final PFlowPanel sizeRow = Element.newPFlowPanel();
        for (int i = 0; i < sizes.length; i++) {
            final int v = sizes[i];
            sizeBtns[i] = Element.newPButton(String.valueOf(v));
            styleBinaryCtrl(sizeBtns[i], v == cellCount[0]);
            sizeBtns[i].addClickHandler(e -> {
                cellCount[0] = v;
                for (int j = 0; j < sizes.length; j++) styleBinaryCtrl(sizeBtns[j], sizes[j] == v);
                rebuildCells.run();
            });
            sizeRow.add(sizeBtns[i]);
        }
        controls.add(binaryCtrlGroup("Cells", sizeRow));

        final int[] rates = { 5, 20, 50 };
        final PButton[] rateBtns = new PButton[rates.length];
        final PFlowPanel rateRow = Element.newPFlowPanel();
        for (int i = 0; i < rates.length; i++) {
            final int v = rates[i];
            rateBtns[i] = Element.newPButton(v + " ticks/s");
            styleBinaryCtrl(rateBtns[i], v == rate[0]);
            rateBtns[i].addClickHandler(e -> {
                rate[0] = v;
                for (int j = 0; j < rates.length; j++) styleBinaryCtrl(rateBtns[j], rates[j] == v);
                restartTicker.run();
            });
            rateRow.add(rateBtns[i]);
        }
        controls.add(binaryCtrlGroup("Rate", rateRow));

        p.add(controls);
        p.add(grid);
        p.add(el("ov-note", "Each tick changes the text and colour of a quarter of the cells. Within one tick, "
            + "repeated changes of a cell are coalesced, the updates are chained into runs, and repeated prices "
            + "travel as 3-byte dictionary references before permessage-deflate."));

        rebuildCells.run();
        restartTicker.run();

        // 1 s refresh of the live cards (deltas of the connection counters)
        final long[] prev = { uiContext.getWireBytesSent(), uiContext.getProtocolBytesSent(), 0, System.nanoTime(), 0, 0 };
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            final long wire = uiContext.getWireBytesSent();
            final long raw = uiContext.getProtocolBytesSent();
            final long now = System.nanoTime();
            final double secs = Math.max(0.001, (now - prev[3]) / 1e9);
            final long dWire = wire - prev[0], dRaw = raw - prev[1], dUpd = updates[0] - prev[2];
            prev[0] = wire;
            prev[1] = raw;
            prev[2] = updates[0];
            prev[3] = now;
            final long dCalls = calls[0] - prev[4], dJson = jsonBytes[0] - prev[5];
            prev[4] = calls[0];
            prev[5] = jsonBytes[0];
            final double[] perSec = { dCalls / secs, dUpd / secs, dJson / 1024.0 / secs, dRaw / 1024.0 / secs,
                                      wire >= 0 ? dWire / 1024.0 / secs : 0 };
            for (int k = 0; k < perSec.length; k++) {
                stageVal[k].setInnerText(k < 2 ? String.format(java.util.Locale.ROOT, "%,d", Math.round(perSec[k]))
                    : String.format(java.util.Locale.ROOT, "%.1f", perSec[k]));
                // counts are scaled on the calls, bytes on the JSON size
                final double base = k < 2 ? perSec[0] : perSec[2];
                final int pct = base > 0 ? (int) Math.max(1, Math.min(100, Math.round(perSec[k] * 100 / base))) : 0;
                stageFill[k].setAttribute("style", "width:" + pct + "%");
                if (k == 1 && perSec[0] > 0) stageGain[k].setInnerText("−" + Math.round(100 - perSec[1] * 100 / perSec[0]) + " %");
                if (k >= 3 && perSec[2] > 0) stageGain[k].setInnerText(String.format(java.util.Locale.ROOT, "%.0f", perSec[2] / Math.max(0.001, perSec[k])) + "× smaller than JSON");
            }
            if (wire >= 0) {
                wireVal.setInnerText(String.format(java.util.Locale.ROOT, "%.1f", dWire / 1024.0 / secs));
                ratioVal.setInnerText(dWire > 0 ? String.format(java.util.Locale.ROOT, "%.1f", (double) dRaw / dWire) : "—");
                perUpdVal.setInnerText(dUpd > 0 ? String.format(java.util.Locale.ROOT, "%.1f", (double) dWire / dUpd) : "—");
            }
            rawVal.setInnerText(String.format(java.util.Locale.ROOT, "%.1f", dRaw / 1024.0 / secs));
            updVal.setInnerText(String.format(java.util.Locale.ROOT, "%,d", Math.round(dUpd / secs)));
            latVal.setInnerText(String.format(java.util.Locale.ROOT, "%.0f", uiContext.getRoundtripLatency()));
        }, Duration.ofSeconds(1)));

        return p;
    }

    static PFlowPanel compareRow(final String version, final String value, final int pct, final boolean current) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("ov-cmp");
        if (current) row.addStyleName("ov-cmp-now");
        row.add(el("ov-cmp-ver", version));
        final PElement bar = Element.newDiv();
        bar.addStyleName("ov-bar");
        final PElement fill = Element.newDiv();
        fill.addStyleName("ov-bar-fill");
        fill.setAttribute("style", "width:" + pct + "%");
        bar.add(fill);
        row.add(bar);
        row.add(el("ov-cmp-val", value));
        return row;
    }
}
