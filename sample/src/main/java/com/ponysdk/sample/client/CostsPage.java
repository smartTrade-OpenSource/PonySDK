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
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PFlexTable;

/** Component costs: the measured cost of every component. */
final class CostsPage {

    private CostsPage() {
    }

    static PFlowPanel build() {
        final PFlowPanel p = section();
        pageTitle(p, "Component costs",
            "What each component really costs, measured live on this server: memory kept on the server per "
                + "instance, and bytes per instance to create it and to update it.");

        final PFlowPanel controls = Element.newPFlowPanel();
        controls.addStyleName("ov-controls");
        final PButton run = Element.newPButton("Measure every component");
        styleBinaryCtrl(run, true);
        final PLabel status = Element.newPLabel("Each measurement creates " + CostMeter.SAMPLE
            + " instances in a hidden container, updates them once, then removes them.");
        status.addStyleName("lay-hint");
        controls.add(run, status);
        p.add(controls);

        final com.ponysdk.core.ui.basic.PFlexTable table = Element.newPFlexTable();
        table.addStyleName("data-table");
        table.addStyleName("cost-table");
        final String[] heads = { "Component", "Server memory", "", "Create · protocol", "Create · wire",
                                 "Update", "Update · protocol", "Update · wire" };
        for (int c = 0; c < heads.length; c++) table.setWidget(0, c, Element.newPLabel(heads[c]));
        final List<CostSpec> specs = CostSpecs.ALL;
        for (int r = 0; r < specs.size(); r++) {
            table.setWidget(r + 1, 0, Element.newPLabel(specs.get(r).name()));
            for (int c = 1; c < heads.length; c++) table.setWidget(r + 1, c, Element.newPLabel(c == 5 ? specs.get(r).updateLabel() : "—"));
        }
        // 8 columns: the table scrolls on its own instead of widening the page
        final PFlowPanel tableScroll = Element.newPFlowPanel();
        tableScroll.addStyleName("table-scroll");
        tableScroll.add(table);
        p.add(tableScroll);
        p.add(el("ov-note", "Heavy components (DataGrid2, FlexLayout) are measured on 5 instances laid out off-screen, "
            + "bytes counted until they finish rendering. Memory: object graph of one instance, shared objects (session, window, style pools, "
            + "interned strings) excluded. Protocol: exact bytes produced by the binary protocol, per instance. Wire: "
            + "bytes on the socket after permessage-deflate, per instance; " + CostMeter.SAMPLE + " similar instances "
            + "compress very well, so this is the cost when a screen repeats a component (grids, lists)."));

        final java.util.function.BiConsumer<Integer, CostMeter.Cost> show = (row, cost) -> {
            table.setWidget(row, 1, Element.newPLabel(CostMeter.memory(cost.memory())));
            final PElement bar = Element.newDiv();
            bar.addStyleName("ov-bar");
            final PElement fill = Element.newDiv();
            fill.addStyleName("ov-bar-fill");
            fill.addStyleName("cost-fill");
            fill.setAttribute("style", "width:" + Math.max(3, cost.memory() * 100 / 2048) + "%");
            bar.add(fill);
            table.setWidget(row, 2, bar);
            table.setWidget(row, 3, Element.newPLabel(bytes(cost.createProtocol())));
            table.setWidget(row, 4, Element.newPLabel(bytes(cost.createWire())));
            table.setWidget(row, 6, Element.newPLabel(bytes(cost.updateProtocol())));
            table.setWidget(row, 7, Element.newPLabel(bytes(cost.updateWire())));
        };
        final boolean[] all = { false }; // "Measure again": every component, not only the ones never measured
        final int[] measured = { 0 };
        final java.util.function.IntConsumer[] next = new java.util.function.IntConsumer[1];
        next[0] = idx -> {
            // page left (replaced by another) or client gone (suspended): stop, nobody would see the results
            if (p.getParent() == null || UIContext.get().isSuspended()) {
                run.setEnabled(true);
                return;
            }
            if (idx >= specs.size()) {
                status.setText(measured[0] == specs.size() ? "Done: " + specs.size() + " components measured."
                    : measured[0] > 0 ? "Done: " + measured[0] + " measured now, the other " + (specs.size() - measured[0])
                        + " earlier on this server. Measure again re-measures all " + specs.size() + "."
                    : "Last measured on this server. Measure again re-measures them in this session.");
                run.setEnabled(true);
                return;
            }
            final CostSpec spec = specs.get(idx);
            if (!all[0] && MEASURED.containsKey(spec.name())) {
                next[0].accept(idx + 1);
                return;
            }
            status.setText("Measuring " + spec.name() + "…");
            spec.measure(cost -> {
                // page left meanwhile: another page's traffic (a stream) may have run during the window, keep nothing
                if (p.getParent() == null) return;
                measured[0]++;
                MEASURED.put(spec.name(), cost);
                show.accept(idx + 1, cost);
                next[0].accept(idx + 1);
            });
        };
        run.addClickHandler(e -> {
            run.setEnabled(false);
            all[0] = true;
            measured[0] = 0;
            next[0].accept(0);
        });
        // Results known on this server are shown at once; the missing ones are measured as soon as the page opens
        for (int r = 0; r < specs.size(); r++) {
            final CostMeter.Cost cost = MEASURED.get(specs.get(r).name());
            if (cost != null) show.accept(r + 1, cost);
        }
        run.setText("Measure again");
        run.setEnabled(false);
        PScheduler.schedule(() -> next[0].accept(0), Duration.ofMillis(300));
        return p;
    }
}
