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
import static com.ponysdk.sample.client.DataGridPage.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAbsolutePanel;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PDatePicker;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PPushButton;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PFocusPanel;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PTextArea;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PRichTextArea;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PTabLayoutPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;

/** The measured cost of each component (memory, bytes to create and update it), shared by the pages that show it. */
final class Costs {

    static final java.util.function.BiConsumer<PWidget, Integer> STYLE_UPDATE =
        (w, i) -> w.setStyleProperty("color", i % 2 == 0 ? "#43e8b0" : "#f87171");

    private Costs() {
    }

    // ── Tab: Component costs ─────────────────────────────────────────────────

    /** What to measure for one component: how to build an instance and a typical update (receives the index). */
    record CostSpec(String name, Supplier<PWidget> factory, java.util.function.BiConsumer<PWidget, Integer> update,
                            String updateLabel, int sample, boolean async) {

        CostSpec(final String name, final Supplier<PWidget> factory,
                 final java.util.function.BiConsumer<PWidget, Integer> update, final String updateLabel) {
            this(name, factory, update, updateLabel, CostMeter.SAMPLE, false);
        }

        void measure(final java.util.function.Consumer<CostMeter.Cost> done) {
            final int[] counter = { 0 };
            CostMeter.measure(name, factory, update == null ? null : w -> update.accept(w, counter[0]++), updateLabel,
                sample, async, done);
        }
    }

    /** Showcase row label → measured component (the "Cost" button of each row). */
    static final Map<String, String> ROW_COSTS = Map.ofEntries(
        Map.entry("TextArea", "PTextArea"), Map.entry("Password", "PPasswordTextBox"),
        Map.entry("RichText editor", "PRichTextArea"), Map.entry("PushButton", "PPushButton"),
        Map.entry("DateBox dd/MM/yyyy", "PDateBox"), Map.entry("DateBox with picker", "PDateBox"),
        Map.entry("DatePicker", "PDatePicker"), Map.entry("ListBox (multi)", "PListBox (5 items)"),
        Map.entry("MenuBar", "PMenuBar (3 items)"), Map.entry("FileUpload", "PFileUpload"),
        Map.entry("HTML", "PHTML"), Map.entry("Anchor", "PAnchor"),
        Map.entry("FlowPanel", "PFlowPanel + 3 labels"), Map.entry("HorizontalPanel", "PHorizontalPanel + 3 labels"),
        Map.entry("VerticalPanel", "PVerticalPanel + 3 labels"), Map.entry("AbsolutePanel", "PAbsolutePanel + 3 labels"),
        Map.entry("ScrollPanel", "PScrollPanel + label"), Map.entry("SimplePanel", "PSimplePanel + label"),
        Map.entry("FocusPanel", "PFocusPanel + label"), Map.entry("DockLayoutPanel", "PDockLayoutPanel (4 regions)"),
        Map.entry("SplitLayoutPanel", "PSplitLayoutPanel (2 regions)"), Map.entry("StackLayoutPanel", "PStackLayoutPanel (3 sections)"),
        Map.entry("TabLayoutPanel", "PTabLayoutPanel (3 tabs)"), Map.entry("DisclosurePanel", "PDisclosurePanel + label"),
        Map.entry("my-counter", "PWebComponent <my-counter>"));

    static List<PWidget> threeLabels() {
        return List.of(Element.newPLabel("Bid"), Element.newPLabel("Ask"), Element.newPLabel("Spread"));
    }

    static List<CostSpec> costSpecs() {
        final List<CostSpec> l = new ArrayList<>();
        l.add(new CostSpec("PLabel", () -> Element.newPLabel("EUR/USD 1.08512"),
            (w, i) -> ((PLabel) w).setText("EUR/USD 1." + (8500 + i)), "setText"));
        l.add(new CostSpec("PHTML", () -> Element.newPHTML("<b>Trade</b> confirmed"),
            (w, i) -> ((PHTML) w).setHTML("<b>Trade</b> #" + i), "setHTML"));
        l.add(new CostSpec("PButton", () -> Element.newPButton("Buy"),
            (w, i) -> ((PButton) w).setText("Sell " + i), "setText"));
        l.add(new CostSpec("PAnchor", () -> Element.newPAnchor("Docs", "https://github.com/smartTrade-OpenSource/PonySDK"),
            (w, i) -> ((PAnchor) w).setText("Docs " + i), "setText"));
        l.add(new CostSpec("PImage", () -> Element.newPImage("images/pony.png"),
            (w, i) -> w.setStyleProperty("opacity", i % 2 == 0 ? "0.5" : "0.8"), "style"));
        l.add(new CostSpec("PTextBox", () -> Element.newPTextBox(),
            (w, i) -> ((PTextBox) w).setText("value " + i), "setText"));
        l.add(new CostSpec("PPasswordTextBox", () -> Element.newPPasswordTextBox(),
            (w, i) -> ((PTextBox) w).setText("secret" + i), "setText"));
        l.add(new CostSpec("PTextArea", () -> Element.newPTextArea(),
            (w, i) -> ((PTextArea) w).setText("Line one\nline " + i), "setText"));
        l.add(new CostSpec("PCheckBox", () -> Element.newPCheckBox("Enable"),
            (w, i) -> ((PCheckBox) w).setValue(Boolean.TRUE), "setValue"));
        l.add(new CostSpec("PRadioButton", () -> Element.newPRadioButton("Option"),
            (w, i) -> ((PRadioButton) w).setValue(Boolean.TRUE), "setValue"));
        l.add(new CostSpec("PListBox (5 items)", () -> {
            final PListBox lb = Element.newPListBox();
            for (final String it : new String[] { "Spot", "Forward", "Swap", "NDF", "Option" }) lb.addItem(it);
            return lb;
        }, (w, i) -> ((PListBox) w).setSelectedIndex(i % 5, true), "select"));
        l.add(new CostSpec("PDateBox", () -> Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy")),
            (w, i) -> ((PDateBox) w).setValue(new Date(1_700_000_000_000L + i * 86_400_000L)), "setValue"));
        l.add(new CostSpec("PFlowPanel + 3 labels", () -> {
            final PFlowPanel fp = Element.newPFlowPanel();
            fp.add(Element.newPLabel("Bid"), Element.newPLabel("Ask"), Element.newPLabel("Spread"));
            return fp;
        }, (w, i) -> w.setStyleProperty("color", i % 2 == 0 ? "#43e8b0" : "#f87171"), "style"));
        l.add(new CostSpec("PFlexTable 3×3", () -> {
            final PFlexTable t = Element.newPFlexTable();
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) t.setWidget(r, c, Element.newPLabel(r + ":" + c));
            return t;
        }, (w, i) -> w.setStyleProperty("opacity", i % 2 == 0 ? "0.9" : "1"), "style"));
        l.add(new CostSpec("PTree (3 items)", () -> {
            final PTree t = Element.newPTree();
            final PTreeItem f = t.add("Desk");
            f.add("EUR/USD");
            f.add("GBP/USD");
            return t;
        }, (w, i) -> w.setStyleProperty("opacity", i % 2 == 0 ? "0.9" : "1"), "style"));
        l.add(new CostSpec("PWebComponent <my-counter>", () -> {
            final PWebComponent wc = new PWebComponent("my-counter");
            wc.attr("label", "Clicks");
            wc.property("count").set(0);
            return wc;
        }, (w, i) -> ((PWebComponent) w).property("count").set(i), "property"));
        l.add(new CostSpec("PPushButton", () -> {
            final PPushButton b = Element.newPPushButton(Element.newPImage());
            b.setText("Push");
            return b;
        }, (w, i) -> ((PPushButton) w).setText("Push " + i), "setText"));
        l.add(new CostSpec("PRichTextArea", () -> Element.newPRichTextArea(),
            (w, i) -> ((PRichTextArea) w).setHTML("<b>note</b> " + i), "setHTML"));
        l.add(new CostSpec("PDatePicker", () -> Element.newPDatePicker(), STYLE_UPDATE, "style"));
        l.add(new CostSpec("PMenuBar (3 items)", () -> {
            final PMenuBar m = Element.newPMenuBar(true);
            m.addItem(Element.newPMenuItem("File"));
            m.addItem(Element.newPMenuItem("Edit"));
            m.addItem(Element.newPMenuItem("View"));
            return m;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PFileUpload", () -> Element.newPFileUpload(), STYLE_UPDATE, "style"));
        l.add(new CostSpec("PHorizontalPanel + 3 labels", () -> {
            final PHorizontalPanel h = Element.newPHorizontalPanel();
            threeLabels().forEach(h::add);
            return h;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PVerticalPanel + 3 labels", () -> {
            final PVerticalPanel v = Element.newPVerticalPanel();
            threeLabels().forEach(v::add);
            return v;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PAbsolutePanel + 3 labels", () -> {
            final PAbsolutePanel a = Element.newPAbsolutePanel();
            threeLabels().forEach(a::add);
            return a;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PScrollPanel + label", () -> {
            final PScrollPanel sp = Element.newPScrollPanel();
            sp.setWidget(Element.newPLabel("Scrollable content"));
            return sp;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PSimplePanel + label", () -> {
            final PSimplePanel sp = Element.newPSimplePanel();
            sp.setWidget(Element.newPLabel("Only child"));
            return sp;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PFocusPanel + label", () -> {
            final PFocusPanel fp = Element.newPFocusPanel();
            fp.setWidget(Element.newPLabel("Focus me"));
            fp.addClickHandler(e -> { });
            return fp;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PDockLayoutPanel (4 regions)", () -> {
            final PDockLayoutPanel d = Element.newPDockLayoutPanel(PUnit.PX);
            d.addNorth(Element.newPLabel("N"), 20);
            d.addSouth(Element.newPLabel("S"), 20);
            d.addWest(Element.newPLabel("W"), 40);
            d.add(Element.newPLabel("C"));
            return d;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PSplitLayoutPanel (2 regions)", () -> {
            final PSplitLayoutPanel sp = Element.newPSplitLayoutPanel();
            sp.addWest(Element.newPLabel("W"), 80);
            sp.add(Element.newPLabel("C"));
            return sp;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PStackLayoutPanel (3 sections)", () -> {
            final PStackLayoutPanel st = Element.newPStackLayoutPanel(PUnit.PX);
            for (final String h : new String[] { "Mail", "Contacts", "Tasks" }) st.add(Element.newPLabel(h), h, false, 24);
            return st;
        }, STYLE_UPDATE, "style"));
        l.add(new CostSpec("PTabLayoutPanel (3 tabs)", () -> {
            final PTabLayoutPanel t = Element.newPTabLayoutPanel();
            for (final String h : new String[] { "One", "Two", "Three" }) t.add(Element.newPLabel(h), h);
            return t;
        }, (w, i) -> ((PTabLayoutPanel) w).selectTab(i % 3), "selectTab"));
        l.add(new CostSpec("PDisclosurePanel + label", () -> {
            final PDisclosurePanel dp = Element.newPDisclosurePanel("More");
            dp.setContent(Element.newPLabel("Details"));
            return dp;
        }, (w, i) -> ((PDisclosurePanel) w).setOpen(true), "setOpen"));
        // Heavy components: few instances, rendered off-screen, bytes counted over the settle window
        l.add(new CostSpec("DataGrid2 (8 cols × 50 rows)", Costs::smallDataGrid,
            null, "—", 5, true));
        l.add(new CostSpec("FlexLayout (3 tabs)", () -> {
            final com.ponysdk.core.ui.flexlayout.FlexLayoutPanel f = new com.ponysdk.core.ui.flexlayout.FlexLayoutPanel();
            for (final String t : new String[] { "Blotter", "Chart", "Orders" }) f.addTab(t, Element.newPLabel(t));
            final PWidget w = f.asWidget();
            w.setWidth("600px");
            w.setHeight("300px");
            return w;
        }, null, "—", 5, true));
        return l;
    }

    /** A compact DataGrid2 (8 columns, 50 rows) for the cost table. */
    static PWidget smallDataGrid() {
        final DefaultDataGridView<Integer, MyRow> view = new DefaultDataGridView<>();
        final List<ColumnDefinition<MyRow>> columns = new ArrayList<>();
        for (char c = 'a'; c <= 'h'; c++) {
            final String key = String.valueOf(c);
            columns.add(new DefaultColumnDefinition<>(key, v -> v.getValue(key), (v, val) -> v.putValue(key, val)));
        }
        view.setAdapter(new DataGridAdapter<>() {
            @Override public void onUnselectRow(final IsPWidget r) { }
            @Override public void onSelectRow(final IsPWidget r) { }
            @Override public boolean isAscendingSortByInsertionOrder() { return false; }
            @Override public Integer getKey(final MyRow v) { return v.id; }
            @Override public List<ColumnDefinition<MyRow>> getColumnDefinitions() { return columns; }
            @Override public int compareDefault(final MyRow v1, final MyRow v2) { return 0; }
            @Override public void onCreateHeaderRow(final IsPWidget r) { }
            @Override public void onCreateFooterRow(final IsPWidget r) { }
            @Override public void onCreateRow(final IsPWidget r) { }
            @Override public boolean hasHeader() { return true; }
            @Override public boolean hasFooter() { return false; }
            @Override public IsPWidget createLoadingDataWidget() { return Element.newDiv(); }
            @Override public void onCreateColumnResizer(final IsPWidget r) { }
            @Override public boolean isSelectionEnabled() { return false; }
        });
        view.asWidget().setHeight("400px");
        view.asWidget().setWidth("900px");
        final DataGridController<Integer, MyRow> controller = view.getController();
        controller.setBound(false);
        for (int i = 0; i < 50; i++) controller.setData(createMyRow(i));
        controller.setBound(true);
        return view.asWidget();
    }

    /** Built once, on first use: the specs are stateless (factories and update functions), shared by every session. */
    static final class CostSpecs {
        static final List<CostSpec> ALL = costSpecs();
    }

    /**
     * Last measurement of each component on this server. Memory and protocol bytes do not change between runs of a
     * build: a visit shows them at once instead of re-creating 3 300 instances for 20 s, "Measure again" re-measures.
     */
    static final Map<String, CostMeter.Cost> MEASURED = new ConcurrentHashMap<>();

    static CostSpec costSpec(final String name) {
        return CostSpecs.ALL.stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    /** "Cost" view of an example card: measures the component on demand. */
    static PFlowPanel costBody(final CostSpec spec) {
        final PFlowPanel body = Element.newPFlowPanel();
        body.addStyleName("pony-ex-body");
        body.addStyleName("cost-body");
        final PElement mem = el("perf-value", "—");
        final PElement create = el("perf-value", "—");
        final PElement upd = el("perf-value", "—");
        final PFlowPanel grid = Element.newPFlowPanel();
        grid.addStyleName("perf-grid");
        grid.addStyleName("ov-live");
        final PElement createUnit = el("perf-unit", "bytes on the wire");
        final PElement updUnit = el("perf-unit", "bytes on the wire");
        grid.add(perfCard(mem, el("perf-unit", "per instance"), el("perf-label", "Server memory")));
        grid.add(perfCard(create, createUnit, el("perf-label", "Create")));
        grid.add(perfCard(upd, updUnit, el("perf-label", "Update · " + spec.updateLabel())));
        final PButton measure = Element.newPButton("Measure");
        styleBinaryCtrl(measure, true);
        final PLabel hint = Element.newPLabel("Creates " + CostMeter.SAMPLE + " instances in this session, updates them once, then removes them.");
        hint.addStyleName("lay-hint");
        measure.addClickHandler(e -> {
            measure.setEnabled(false);
            spec.measure(cost -> {
                mem.setInnerText(CostMeter.memory(cost.memory()));
                create.setInnerText(bytes(cost.createWire()).replace(" B", ""));
                createUnit.setInnerText("B ON THE WIRE · " + bytes(cost.createProtocol()) + " BEFORE DEFLATE");
                upd.setInnerText(bytes(cost.updateWire()).replace(" B", ""));
                updUnit.setInnerText("B ON THE WIRE · " + bytes(cost.updateProtocol()) + " BEFORE DEFLATE");
                measure.setEnabled(true);
            });
        });
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("cost-row");
        row.add(measure, hint);
        body.add(row, grid);
        return body;
    }

    /** "Cost" button of a row: measures the component in place and shows memory / create / update on the wire. */
    static PFlowPanel rowCost(final CostSpec spec) {
        final PFlowPanel box = Element.newPFlowPanel();
        box.addStyleName("row-cost");
        final PLabel result = Element.newPLabel("");
        result.addStyleName("row-cost-result");
        final PButton b = Element.newPButton("Cost");
        b.addStyleName("row-cost-btn");
        b.addClickHandler(e -> {
            b.setEnabled(false);
            result.setText("measuring…");
            spec.measure(cost -> {
                result.setText(CostMeter.memory(cost.memory()) + " memory · create " + bytes(cost.createWire())
                    + (cost.updateWire() >= 0 ? " · " + cost.updateLabel() + " " + bytes(cost.updateWire()) : "")
                    + " on the wire");
                b.setEnabled(true);
            });
        });
        box.add(result, b);
        return box;
    }
}
