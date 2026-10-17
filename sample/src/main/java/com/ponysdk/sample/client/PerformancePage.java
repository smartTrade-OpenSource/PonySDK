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
import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;

/** Performance: live JVM and network metrics, 60-second charts. */
final class PerformancePage {

    private PerformancePage() {
    }

    // ── Tab: Perf ─────────────────────────────────────────────────────────────

    static PFlowPanel build(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Performance", "Live JVM and network metrics of this server and this session, sampled every "
            + "second. The charts are plain PonySDK widgets: 60 bars whose heights are set from Java.");
        final Runtime rt = Runtime.getRuntime();
        final long maxMem = rt.maxMemory();
        final long startTime = System.currentTimeMillis();

        p.add(sectionTitle("Last 60 seconds"));
        final PFlowPanel charts = Element.newPFlowPanel();
        charts.addStyleName("spark-grid");
        final Sparkline heapChart = new Sparkline("Live heap", "MB", "%.0f");
        heapChart.fill(java.util.Arrays.stream(LiveHeap.lastMinute()).mapToDouble(b -> b / 1024 / 1024).toArray());
        final Sparkline wireChart = new Sparkline("Sent on the wire", "KB/s", "%.1f");
        final Sparkline latChart = new Sparkline("Roundtrip latency", "ms", "%.0f");
        charts.add(heapChart.card, wireChart.card, latChart.card);
        p.add(charts);

        p.add(sectionTitle("JVM"));
        final PFlowPanel heapGrid = Element.newPFlowPanel();
        heapGrid.addStyleName("perf-grid");
        final PElement heapVal = el("perf-value", "—");
        final PElement heapUnit = el("perf-unit", "MB after the last GC");
        final PElement heapLbl = el("perf-label", "Live heap");
        final PElement heapBarWrap = Element.newDiv(); heapBarWrap.addStyleName("perf-bar-wrap");
        final PElement heapBar = Element.newDiv(); heapBar.addStyleName("perf-bar"); heapBar.setAttribute("style", "width:0%");
        heapBarWrap.add(heapBar);
        final PElement heapTrend = el("perf-trend", "");
        final PFlowPanel heapCard = perfCard(heapVal, heapUnit, heapLbl, heapBarWrap, heapTrend);
        final PElement maxVal = el("perf-value", String.valueOf(maxMem / 1024 / 1024));
        final PElement maxUnit = el("perf-unit", "MB max");
        final PElement maxLbl = el("perf-label", "Heap max");
        final PFlowPanel maxCard = perfCard(maxVal, maxUnit, maxLbl);
        final PElement committedVal = el("perf-value", "—");
        final PElement committedUnit = el("perf-unit", "MB reserved by the JVM");
        final PElement committedLbl = el("perf-label", "Heap committed");
        final PFlowPanel committedCard = perfCard(committedVal, committedUnit, committedLbl);
        final PElement cpuVal = el("perf-value", String.valueOf(rt.availableProcessors()));
        final PElement cpuUnit = el("perf-unit", "cores");
        final PElement cpuLbl = el("perf-label", "CPU");
        final PFlowPanel cpuCard = perfCard(cpuVal, cpuUnit, cpuLbl);
        final PElement uptimeVal = el("perf-value", "0");
        final PElement uptimeUnit = el("perf-unit", "seconds");
        final PElement uptimeLbl = el("perf-label", "Uptime");
        final PFlowPanel uptimeCard = perfCard(uptimeVal, uptimeUnit, uptimeLbl);

        heapGrid.add(heapCard, maxCard, committedCard, cpuCard, uptimeCard);
        p.add(heapGrid);

        p.add(sectionTitle("Network and sessions"));
        final PFlowPanel netGrid = Element.newPFlowPanel();
        netGrid.addStyleName("perf-grid");
        final PElement sessVal = el("perf-value", "1");
        final PElement sessUnit = el("perf-unit", "active");
        final PElement sessLbl = el("perf-label", "Sessions");
        final PFlowPanel sessCard = perfCard(sessVal, sessUnit, sessLbl);
        final PElement latVal = el("perf-value", "—");
        final PElement latUnit = el("perf-unit", "ms roundtrip");
        final PElement latLbl = el("perf-label", "Latency");
        final PElement latBar2Wrap = Element.newDiv(); latBar2Wrap.addStyleName("perf-bar-wrap");
        final PElement latBar2 = Element.newDiv(); latBar2.addStyleName("perf-bar"); latBar2.setAttribute("style", "width:0%");
        latBar2Wrap.add(latBar2);
        final PFlowPanel latCard = perfCard(latVal, latUnit, latLbl, latBar2Wrap);
        final PElement sentVal = el("perf-value", "—");
        final PElement sentUnit = el("perf-unit", "KB/s · deflate");
        final PElement sentLbl = el("perf-label", "Sent (compressed)");
        final PFlowPanel sentCard = perfCard(sentVal, sentUnit, sentLbl);
        final PElement rcvVal = el("perf-value", "—");
        final PElement rcvUnit = el("perf-unit", "KB/s");
        final PElement rcvLbl = el("perf-label", "Received");
        final PFlowPanel rcvCard = perfCard(rcvVal, rcvUnit, rcvLbl);
        final PElement netVal = el("perf-value", "v" + uiContext.getProtocolVersion());
        final PElement netUnit = el("perf-unit", "binary protocol");
        final PElement netLbl = el("perf-label", "WebSocket");
        final PFlowPanel netCard = perfCard(netVal, netUnit, netLbl);

        netGrid.add(sessCard, latCard, sentCard, rcvCard, netCard);
        p.add(netGrid);

        // Dictionary section
        p.add(sectionTitle("String dictionary"));
        final PFlowPanel dictGrid = Element.newPFlowPanel();
        dictGrid.addStyleName("perf-grid");
        final PElement dictSizeVal = el("perf-value", "—");
        final PElement dictSizeUnit = el("perf-unit", "entries");
        final PElement dictSizeLbl = el("perf-label", "Entries");
        final PElement dictBarWrap = Element.newDiv(); dictBarWrap.addStyleName("perf-bar-wrap");
        final PElement dictBar = Element.newDiv(); dictBar.addStyleName("perf-bar"); dictBar.setAttribute("style", "width:0%");
        dictBarWrap.add(dictBar);
        final PFlowPanel dictSizeCard = perfCard(dictSizeVal, dictSizeUnit, dictSizeLbl, dictBarWrap);
        final PElement dictMaxVal = el("perf-value", "4096");
        final PElement dictMaxUnit = el("perf-unit", "max entries");
        final PElement dictMaxLbl = el("perf-label", "Capacity");
        final PFlowPanel dictMaxCard = perfCard(dictMaxVal, dictMaxUnit, dictMaxLbl);
        final PElement dictHitVal = el("perf-value", "—");
        final PElement dictHitUnit = el("perf-unit", "% hit rate");
        final PElement dictHitLbl = el("perf-label", "Hit rate");
        final PFlowPanel dictHitCard = perfCard(dictHitVal, dictHitUnit, dictHitLbl);

        dictGrid.add(dictSizeCard, dictMaxCard, dictHitCard);
        p.add(dictGrid);

        // Highlight box
        p.add(highlightBox("", "Zero polling overhead",
            "PonySDK uses a persistent WebSocket connection. No HTTP polling, no REST calls. " +
            "Each UI update is a binary delta of a few bytes: about 10 bytes on the wire for a lone update, about 5 " +
            "per cell when a grid updates many cells at once: see Overview for a live measurement."));

        // Live refresh every second
        final long[] prevHeap = {0};
        final long[] prevSent = {0};
        final long[] prevRcv = {0};
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            final long used = LiveHeap.bytes() / 1024 / 1024;
            final long max = maxMem / 1024 / 1024;
            final int pct = (int) (used * 100 / Math.max(max, 1));
            heapVal.setInnerText(String.valueOf(used));
            committedVal.setInnerText(String.valueOf(rt.totalMemory() / 1024 / 1024));
            heapBar.setAttribute("style", "width:" + pct + "%;height:4px;border-radius:4px;background:var(--accent);transition:width .8s cubic-bezier(.4,0,.2,1);");
            heapTrend.setInnerText(used > prevHeap[0] ? "+" + (used - prevHeap[0]) + " MB" : used < prevHeap[0] ? "−" + (prevHeap[0] - used) + " MB" : "stable");
            heapTrend.setAttribute("class", used > prevHeap[0] ? "perf-trend perf-down" : used < prevHeap[0] ? "perf-trend perf-up" : "perf-trend");
            prevHeap[0] = used;

            final long upSec = (System.currentTimeMillis() - startTime) / 1000;
            uptimeVal.setInnerText(upSec < 60 ? String.valueOf(upSec) : (upSec / 60) + "m " + (upSec % 60) + "s");
            uptimeUnit.setInnerText(upSec < 60 ? "seconds" : "minutes, seconds");

            final double lat = uiContext.getRoundtripLatency();
            latVal.setInnerText(String.format("%.0f", lat));
            final int latPct = (int) Math.min(lat / 2, 100);
            latBar2.setAttribute("style", "width:" + latPct + "%;background:" + (latPct < 30 ? "var(--accent)" : latPct < 70 ? "var(--warn)" : "var(--bad)") + " !important");

            final PonySDKMetrics metrics = uiContext.getMetrics();
            if (metrics != null) sessVal.setInnerText(String.valueOf(metrics.getActiveContexts()));
            // per-connection wire counters (framing + compressed payload), sampled every second
            final long curSent = uiContext.getWireBytesSent();
            final long curRcv = uiContext.getWireBytesReceived();
            if (curSent >= 0 && prevSent[0] > 0) sentVal.setInnerText(String.format(java.util.Locale.ROOT, "%.1f", (curSent - prevSent[0]) / 1024.0));
            if (curRcv >= 0 && prevRcv[0] > 0) rcvVal.setInnerText(String.format(java.util.Locale.ROOT, "%.1f", (curRcv - prevRcv[0]) / 1024.0));
            if (curSent >= 0 && prevSent[0] > 0) wireChart.push((curSent - prevSent[0]) / 1024.0);
            prevSent[0] = curSent;
            prevRcv[0] = curRcv;
            heapChart.push(used);
            latChart.push(lat);

            // Dictionary stats
            final com.ponysdk.core.server.application.StringDictionary dict = uiContext.getStringDictionary();
            if (dict != null) {
                final int dictSize = dict.size();
                final int dictMax = dict.getMaxSize();
                dictSizeVal.setInnerText(String.valueOf(dictSize));
                dictMaxVal.setInnerText(String.valueOf(dictMax));
                final int dictPct = (int) (dictSize * 100L / Math.max(dictMax, 1));
                dictBar.setAttribute("style", "width:" + dictPct + "%;height:4px;border-radius:4px;background:var(--accent);transition:width .8s;");
                dictHitVal.setInnerText(String.valueOf(dict.getSessionHitRate()));
            }
        }, Duration.ofSeconds(1)));

        return p;
    }

    /** A 60-sample bar chart: one div per sample, heights set from Java (coalesced style updates). */
    static final class Sparkline {
        private static final int N = 60;
        final PFlowPanel card = Element.newPFlowPanel();
        private final PElement value = el("spark-value", "—");
        private final PElement range = el("spark-range", "");
        private final PElement[] bars = new PElement[N];
        private final double[] samples = new double[N];
        private final String unit;
        private final String format;
        private int count;

        Sparkline(final String label, final String unit, final String format) {
            this.unit = unit;
            this.format = format;
            card.addStyleName("spark-card");
            final PFlowPanel head = Element.newPFlowPanel();
            head.addStyleName("spark-head");
            head.add(el("spark-label", label), value);
            final PFlowPanel plot = Element.newPFlowPanel();
            plot.addStyleName("spark-plot");
            for (int i = 0; i < N; i++) {
                bars[i] = el("spark-bar", "");
                bars[i].setStyleProperty("height", "0%");
                plot.add(bars[i]);
            }
            card.add(head, plot, range);
        }

        void push(final double v) {
            System.arraycopy(samples, 1, samples, 0, N - 1);
            samples[N - 1] = v;
            count = Math.min(N, count + 1);
            render(v);
        }

        /** Starts with values already known, oldest first: drawn once. */
        void fill(final double[] values) {
            final int n = Math.min(N, values.length);
            if (n == 0) return;
            System.arraycopy(values, values.length - n, samples, N - n, n);
            count = n;
            render(values[values.length - 1]);
        }

        private void render(final double v) {
            double max = 0, min = Double.MAX_VALUE, sum = 0;
            for (int i = N - count; i < N; i++) {
                max = Math.max(max, samples[i]);
                min = Math.min(min, samples[i]);
                sum += samples[i];
            }
            final double top = max > 0 ? max * 1.1 : 1;
            for (int i = 0; i < N; i++) {
                // whole percents: unchanged heights are not sent again
                final int h = i < N - count ? 0 : (int) Math.max(2, Math.round(samples[i] * 100 / top));
                bars[i].setStyleProperty("height", h + "%");
            }
            value.setInnerText(String.format(java.util.Locale.ROOT, format, v) + " " + unit);
            range.setInnerText(String.format(java.util.Locale.ROOT, "min " + format + " · avg " + format + " · max " + format + " "
                + unit, min, sum / count, max));
        }
    }
}
