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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAbsolutePanel;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PDatePicker;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFrame;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PRadioButtonGroup;
import com.ponysdk.core.ui.basic.PPushButton;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PFocusPanel;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PTextArea;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PRichTextArea;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PTabLayoutPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.view.ColumnFilterFooterDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ColumnVisibilitySelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ConfigSelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;
import com.ponysdk.core.ui.datagrid2.view.RowSelectorColumnDataGridView;
import com.ponysdk.core.ui.form2.impl.formfield.ColorInputFormField;
import com.ponysdk.core.ui.form2.impl.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.grid.AbstractGridWidget;
import com.ponysdk.core.ui.grid.GridTableWidget;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.refreshable.Cell;
import com.ponysdk.core.ui.list.refreshable.RefreshableDataGrid;
import com.ponysdk.core.ui.list.renderer.cell.CellRenderer;
import com.ponysdk.core.ui.list.valueprovider.IdentityValueProvider;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.rich.PToolbar;
import com.ponysdk.core.ui.rich.PTwinListBox;
import com.ponysdk.sample.client.page.addon.BinaryArgsAddOn;
import com.ponysdk.sample.client.page.addon.FlexLayoutDemoEntryPoint;

public class UISampleEntryPoint implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(UISampleEntryPoint.class);

    int a = 0;

    /** Opens a page of the showcase (index in the sidebar), from anywhere in the showcase. */
    private java.util.function.IntConsumer navigate = i -> { };

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setTerminalDataReceiver((object, instruction) -> log.debug("{} : {}", object, instruction));

        // Styles: css/showcase.css (declared in ShowcaseMain / server_application.xml)
        // Favicon: the "P" monogram of the top bar, inline SVG (no extra request)
        PScript.execute(PWindow.getMain(), "var l=document.createElement('link');l.rel='icon';l.type='image/svg+xml';"
            + "l.href='data:image/svg+xml,'+encodeURIComponent('<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 32 32\">"
            + "<rect width=\"32\" height=\"32\" rx=\"8\" fill=\"#0f172a\"/><text x=\"16\" y=\"22\" font-family=\"Arial\" "
            + "font-size=\"17\" font-weight=\"700\" fill=\"#fff\" text-anchor=\"middle\">P</text></svg>');"
            + "document.head.appendChild(l);");

        // Header
        final PFlowPanel header = Element.newPFlowPanel();
        header.addStyleName("pony-header");

        final PFlowPanel logoWrap = Element.newPFlowPanel();
        logoWrap.addStyleName("pony-logo-wrap");
        final PElement logoIcon = Element.newDiv();
        logoIcon.addStyleName("pony-logo-icon");
        logoIcon.setInnerText("P");
        final PElement logoText = Element.newDiv();
        logoText.addStyleName("pony-logo-text");
        final PElement logoName = Element.newDiv();
        logoName.addStyleName("pony-logo");
        logoName.setInnerText("PonySDK");
        final PElement logoSub = Element.newDiv();
        logoSub.addStyleName("pony-subtitle");
        logoSub.setInnerText("Showcase");
        logoText.add(logoName, logoSub);
        logoWrap.add(logoIcon, logoText);

        final PElement divider = Element.newDiv();
        divider.addStyleName("pony-divider");
        // Where we are: "Group / Page", updated by the navigation
        final PElement crumb = Element.newDiv();
        crumb.addStyleName("pony-crumb");

        // Header live stats
        final PFlowPanel hstats = Element.newPFlowPanel();
        hstats.addStyleName("pony-header-stats");
        final PElement latHVal = Element.newDiv(); latHVal.addStyleName("pony-hstat-val"); latHVal.setInnerText("—");
        final PElement latHLbl = Element.newDiv(); latHLbl.addStyleName("pony-hstat-lbl"); latHLbl.setInnerText("Latency");
        final PFlowPanel latHCard = Element.newPFlowPanel(); latHCard.addStyleName("pony-hstat"); latHCard.add(latHVal, latHLbl);
        final PElement heapHVal = Element.newDiv(); heapHVal.addStyleName("pony-hstat-val"); heapHVal.setInnerText("—");
        final PElement heapHLbl = Element.newDiv(); heapHLbl.addStyleName("pony-hstat-lbl"); heapHLbl.setInnerText("Live heap");
        final PFlowPanel heapHCard = Element.newPFlowPanel(); heapHCard.addStyleName("pony-hstat"); heapHCard.add(heapHVal, heapHLbl);
        final PElement sessHVal = Element.newDiv(); sessHVal.addStyleName("pony-hstat-val"); sessHVal.setInnerText("1");
        final PElement sessHLbl = Element.newDiv(); sessHLbl.addStyleName("pony-hstat-lbl"); sessHLbl.setInnerText("Sessions");
        final PFlowPanel sessHCard = Element.newPFlowPanel(); sessHCard.addStyleName("pony-hstat"); sessHCard.add(sessHVal, sessHLbl);
        hstats.add(latHCard, heapHCard, sessHCard);

        final PElement badge = Element.newDiv();
        badge.addStyleName("pony-badge");
        badge.setInnerText("Live");

        header.add(logoWrap, divider, crumb, hstats, badge);
        PWindow.getMain().add(header);

        // Refresh header stats every second
        PScheduler.scheduleAtFixedRate(() -> {
            if (idle()) return;
            latHVal.setInnerText(String.format("%.0f ms", uiContext.getRoundtripLatency()));
            heapHVal.setInnerText(LiveHeap.bytes() / 1024 / 1024 + " MB");
            final PonySDKMetrics m = uiContext.getMetrics();
            if (m != null) sessHVal.setInnerText(String.valueOf(m.getActiveContexts()));
        }, Duration.ofSeconds(1));

        // Shell = sidebar nav + content
        final PFlowPanel shell = Element.newPFlowPanel();
        shell.addStyleName("pony-shell");

        // Sidebar nav
        final PFlowPanel nav = Element.newPFlowPanel();
        nav.addStyleName("pony-nav");

        // Content area
        final PSimplePanel content = Element.newPSimplePanel();
        content.addStyleName("pony-content");

        // Sidebar entries, by group: { icon, label }
        final String[][] tabs = {
            {"◎", "Overview"},
            {"⌨", "Inputs"}, {"⊞", "Layouts"}, {"◈", "Data"},
            {"⊟", "DataGrid"}, {"◉", "Web Components"}, {"⋯", "Misc"},
            {"⚡", "Performance"}, {"⚖", "Component costs"}, {"≡", "Binary protocol"}, {"</>", "Code"},
            {"▦", "FlexLayout"}
        };
        final String[] groupLabels = {"Start", "Widgets", "Under the hood"};
        final int[] groupStarts = {0, 1, 7};

        // Active tab schedulers — cancelled on tab leave, list cleared and refilled on re-entry
        @SuppressWarnings("unchecked")
        final List<PScheduler.UIRunnable>[] tabSchedulers = new List[tabs.length];
        for (int i = 0; i < tabs.length; i++) tabSchedulers[i] = new ArrayList<>();

        // Only the first tab is built eagerly: the others are built (and their schedulers started) on activation
        final PFlowPanel[] tabPanels = new PFlowPanel[tabs.length];
        tabPanels[0] = buildTabWithSchedulers(0, uiContext, tabSchedulers[0]);

        final int tabCount = tabs.length;
        final PButton[] tabButtons = new PButton[tabCount];
        final int[] activeTab = { 0 };
        final java.util.function.IntConsumer showCrumb = k -> {
            int g = 0;
            while (g + 1 < groupStarts.length && groupStarts[g + 1] <= k) g++;
            crumb.setInnerHTML(groupLabels[g] + " &nbsp;/&nbsp; <b>" + tabs[k][1] + "</b>");
        };
        showCrumb.accept(0);

        int group = 0;
        for (int i = 0; i < tabCount; i++) {
            if (group < groupStarts.length && groupStarts[group] == i) {
                final PElement grp = Element.newDiv();
                grp.addStyleName("pony-nav-group");
                grp.setInnerText(groupLabels[group]);
                nav.add(grp);
                group++;
            }
            final int idx = i;
            final PButton tab = Element.newPButton(tabs[i][1]);
            tab.addStyleName("pony-tab");
            if (i == 0) tab.addStyleName("pony-tab-active");
            tabButtons[i] = tab;
            tab.addClickHandler(e -> navigate.accept(idx));
            nav.add(tab);
        }
        navigate = idx -> {
            if (activeTab[0] == idx) return;
            // Cancel the schedulers of the tab we leave, rebuild the new one fresh (restarts its schedulers)
            tabSchedulers[activeTab[0]].forEach(PScheduler.UIRunnable::cancel);
            tabSchedulers[activeTab[0]].clear();
            tabSchedulers[idx].clear();
            final PFlowPanel left = tabPanels[activeTab[0]];
            tabPanels[activeTab[0]] = null;
            tabPanels[idx] = buildTabWithSchedulers(idx, uiContext, tabSchedulers[idx]);
            content.setWidget(tabPanels[idx]);
            // destroy what the left tab owns (e.g. the DataGrid's own polling task): it is rebuilt on return
            left.onDestroy();
            tabButtons[activeTab[0]].removeStyleName("pony-tab-active");
            tabButtons[idx].addStyleName("pony-tab-active");
            activeTab[0] = idx;
            showCrumb.accept(idx);
            PScript.execute(PWindow.getMain(), "window.scrollTo(0,0)");
        };

        // Nav footer with version
        final PFlowPanel navFooter = Element.newPFlowPanel();
        navFooter.addStyleName("pony-nav-footer");
        final PElement version = Element.newDiv();
        version.addStyleName("pony-version");
        version.setInnerText("PonySDK 3.0 · Java " + System.getProperty("java.version").split("\\.")[0]);
        navFooter.add(version);
        nav.add(navFooter);

        // Show first tab
        content.setWidget(tabPanels[0]);

        shell.add(nav);
        shell.add(content);
        PWindow.getMain().add(shell);
    }

    @SuppressWarnings("unchecked")
    private PFlowPanel buildTabWithSchedulers(final int idx, final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        return switch (idx) {
            case 0 -> buildOverviewTab(uiContext, schedulers);
            case 1 -> buildInputsTab(schedulers);
            case 2 -> buildLayoutsTab(schedulers);
            case 3 -> buildDataTab(schedulers);
            case 4 -> buildDataGridTab(schedulers);
            case 5 -> buildWebCompTab(schedulers);
            case 6 -> buildMiscTab(schedulers);
            case 7 -> buildPerfTab(uiContext, schedulers);
            case 8 -> buildCostsTab();
            case 9 -> buildBinaryAddonTab(schedulers);
            case 10 -> buildCodeTab();
            default -> buildFlexLayoutTab();
        };
    }

    private PFlowPanel buildBinaryAddonTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PElement box = Element.newDiv();
        box.setAttribute("style", "max-width:1040px");

        final PElement title = Element.newDiv();
        title.addStyleName("pony-page-title");
        title.setInnerText("Binary protocol — live signal console");

        final PElement sub = Element.newDiv();
        sub.addStyleName("pony-page-desc");
        sub.setAttribute("style", "margin-bottom:24px");
        sub.setInnerText("Server-driven end to end: the controls below are PonySDK widgets whose handlers run in Java; "
            + "the server computes the signals and streams them as typed double arrays in pure binary, and the browser "
            + "only renders. Push series and resolution up to stream tens of thousands of typed values per frame "
            + "(uint31 length, no 255 cap, no JSON).");

        // The chart is configured entirely through pure-binary typed creation args:
        // color (String), maxPoints (int), lineWidth (double), fill (boolean), seed (long)
        final BinaryArgsAddOn addon = new BinaryArgsAddOn("#4f46e5", 1024, 2.0, false, 1_234_567_890_123L);
        addon.asWidget().setAttribute("style", "display:block;margin-top:4px");

        // Stream state owned by the server
        final boolean[] playing = { true };
        final int[] nSeries = { 2 };
        final int[] mPoints = { 1024 };
        final double[] phase = { 0 };

        // Controls — PonySDK widgets; every click handler runs server-side
        final PFlowPanel controls = Element.newPFlowPanel();
        controls.addStyleName("ov-controls");
        controls.setAttribute("style", "align-items:flex-end");

        final PButton toggle = Element.newPButton("Pause");
        styleBinaryCtrl(toggle, true);
        toggle.addClickHandler(e -> {
            playing[0] = !playing[0];
            toggle.setText(playing[0] ? "Pause" : "Resume");
            styleBinaryCtrl(toggle, playing[0]);
        });
        final PFlowPanel toggleRow = Element.newPFlowPanel();
        toggleRow.setAttribute("style", "display:flex");
        toggleRow.add(toggle);
        controls.add(binaryCtrlGroup("Stream", toggleRow));

        final PButton[] sBtns = new PButton[4];
        final PFlowPanel seriesRow = Element.newPFlowPanel();
        seriesRow.setAttribute("style", "display:flex");
        for (int k = 1; k <= sBtns.length; k++) {
            final int val = k;
            final PButton b = Element.newPButton(String.valueOf(k));
            styleBinaryCtrl(b, k == nSeries[0]);
            b.addClickHandler(e -> {
                nSeries[0] = val;
                for (int j = 0; j < sBtns.length; j++) styleBinaryCtrl(sBtns[j], j + 1 == val);
            });
            sBtns[k - 1] = b;
            seriesRow.add(b);
        }
        controls.add(binaryCtrlGroup("Series", seriesRow));

        final int[] resVals = { 256, 1024, 4096 };
        final PButton[] rBtns = new PButton[resVals.length];
        final PFlowPanel resRow = Element.newPFlowPanel();
        resRow.setAttribute("style", "display:flex");
        for (int k = 0; k < resVals.length; k++) {
            final int idx = k;
            final int val = resVals[k];
            final PButton b = Element.newPButton(String.valueOf(val));
            styleBinaryCtrl(b, val == mPoints[0]);
            b.addClickHandler(e -> {
                mPoints[0] = val;
                for (int j = 0; j < rBtns.length; j++) styleBinaryCtrl(rBtns[j], j == idx);
            });
            rBtns[k] = b;
            resRow.add(b);
        }
        controls.add(binaryCtrlGroup("Points / series", resRow));

        box.add(title, sub, controls, addon.asWidget());
        panel.add(box);

        // Deterministic verification frame (kept stable for the browser IT): 1000 ints, 0..999
        final Object[] ramp = new Object[1000];
        for (int i = 0; i < ramp.length; i++) ramp[i] = i;
        addon.verify(ramp);

        // Live multi-series stream — one structured typed binary payload per frame: [n, m, n*m doubles]
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (!playing[0] || idle()) return;
            final int n = nSeries[0];
            final int m = mPoints[0];
            final Object[] frame = new Object[2 + n * m];
            frame[0] = n;
            frame[1] = m;
            for (int s = 0; s < n; s++) {
                final double off = s * 0.7;
                final int base = 2 + s * m;
                for (int i = 0; i < m; i++) {
                    final double t = (double) i / m;
                    frame[base + i] = Math.sin(t * Math.PI * (6 + s * 2) + phase[0] + off)
                        * (0.6 + 0.4 * Math.sin(t * Math.PI * 2 + phase[0] * 0.5))
                        + 0.06 * Math.sin(t * Math.PI * 40 + phase[0] * 3);
                }
            }
            phase[0] += 0.15;
            addon.stream(frame);
        }, Duration.ofMillis(120)));

        return panel;
    }

    private static PFlowPanel binaryCtrlGroup(final String label, final PFlowPanel content) {
        final PFlowPanel group = Element.newPFlowPanel();
        final PElement caption = Element.newDiv();
        caption.setAttribute("style", "font-size:12px;font-weight:500;color:var(--text-3);margin-bottom:6px");
        caption.setInnerText(label);
        group.add(caption);
        group.add(content);
        return group;
    }

    private static void styleBinaryCtrl(final PButton button, final boolean active) {
        // A segmented choice: the selected value is filled with the accent
        button.setAttribute("style", "margin-right:6px;min-width:44px;font-variant-numeric:tabular-nums;"
            + (active ? "background:var(--accent);border-color:var(--accent);color:#fff;" : ""));
    }

    // ── Tab: Component costs ─────────────────────────────────────────────────

    /** What to measure for one component: how to build an instance and a typical update (receives the index). */
    private record CostSpec(String name, Supplier<PWidget> factory, java.util.function.BiConsumer<PWidget, Integer> update,
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
    private static final Map<String, String> ROW_COSTS = Map.ofEntries(
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

    private static List<PWidget> threeLabels() {
        return List.of(Element.newPLabel("Bid"), Element.newPLabel("Ask"), Element.newPLabel("Spread"));
    }

    private static final java.util.function.BiConsumer<PWidget, Integer> STYLE_UPDATE =
        (w, i) -> w.setStyleProperty("color", i % 2 == 0 ? "#43e8b0" : "#f87171");

    private static List<CostSpec> costSpecs() {
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
        l.add(new CostSpec("DataGrid2 (8 cols × 50 rows)", UISampleEntryPoint::smallDataGrid,
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
    private static PWidget smallDataGrid() {
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
    private static final class CostSpecs {
        static final List<CostSpec> ALL = costSpecs();
    }

    /**
     * Last measurement of each component on this server. Memory and protocol bytes do not change between runs of a
     * build: a visit shows them at once instead of re-creating 3 300 instances for 20 s, "Measure again" re-measures.
     */
    private static final Map<String, CostMeter.Cost> MEASURED = new ConcurrentHashMap<>();

    private static CostSpec costSpec(final String name) {
        return CostSpecs.ALL.stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    private PFlowPanel buildCostsTab() {
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
                status.setText(measured[0] > 0 ? "Done: " + measured[0] + " components measured."
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

    private static String bytes(final double v) {
        if (v < 0) return "n/a";
        return v < 10 ? String.format(java.util.Locale.ROOT, "%.1f B", v) : String.format(java.util.Locale.ROOT, "%.0f B", v);
    }

    /** "Cost" view of an example card: measures the component on demand. */
    private static PFlowPanel costBody(final CostSpec spec) {
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

    // ── Tab: Overview ────────────────────────────────────────────────────────

    /** One measured row of the 2.10 vs 3.0 comparison. */
    private record Measure(String metric, String gain, String factor, String before, String after, int afterPct, String hint) {}

    private PFlowPanel buildOverviewTab(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
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
        tryIt.addClickHandler(e -> navigate.accept(1));
        final PButton protocol = Element.newPButton("See the protocol");
        protocol.addClickHandler(e -> navigate.accept(9));
        final PButton costs = Element.newPButton("What a widget costs");
        costs.addClickHandler(e -> navigate.accept(8));
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
        heroCodeBody.setInnerHTML(
            ln(kw("public class") + " " + ty("LivePrice") + " " + kw("implements") + " " + ty("EntryPoint") + " {")
                + ln("  " + kw("public void") + " " + mt("start") + "(" + ty("UIContext") + " ctx) {")
                + ln("    " + ty("PLabel") + " price = " + ty("Element") + "." + mt("newPLabel") + "(" + st("1.08512") + ");")
                + ln("    " + ty("PWindow") + "." + mt("getMain") + "()." + mt("add") + "(price);")
                + ln("    " + ty("PScheduler") + "." + mt("scheduleAtFixedRate") + "(")
                + ln("        () -> price." + mt("setText") + "(feed." + mt("last") + "()),")
                + ln("        " + ty("Duration") + "." + mt("ofMillis") + "(50));")
                + ln("    " + cm("each tick: about 10 bytes on the wire"))
                + ln("  }")
                + ln("}"));
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
            new Measure("CPU per UI update", "−40 %", "1.8× less", "1 850 ns", "1 050 ns", 57, "Server time to apply and encode one update"),
            new Measure("Bandwidth on the wire", "−58 %", "2.4× less", "1.22 MB/s", "0.51 MB/s", 42, "Same updates, bytes on the TCP stream"),
            new Measure("Allocation per update", "−50 %", "1.9× less", "140 B", "72 B", 51, "Garbage produced per update"),
            new Measure("Process memory (RSS)", "−28 %", "1.4× less", "1 060 MB", "750 MB", 71, "Whole JVM, 200 live sessions"),
            new Measure("Retained heap", "−14 %", "1.2× less", "37.4 MB", "32.3 MB", 86, "Live objects after a full GC, same benchmark") }) {
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
            + "(260 000 updates/s). Values rounded."));

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
            { "Terminal", "GWT → DOM" } }) {
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
                final String color = delta >= 0 ? "#047857" : "#b91c1c";
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
                if (k >= 3 && perSec[2] > 0) stageGain[k].setInnerText("÷" + String.format(java.util.Locale.ROOT, "%.0f", perSec[2] / Math.max(0.001, perSec[k])) + " vs JSON");
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

    private static PFlowPanel compareRow(final String version, final String value, final int pct, final boolean current) {
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

    // ── Tab: Inputs ──────────────────────────────────────────────────────────

    private PFlowPanel buildInputsTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Input Widgets",
            "Each control below is live. Interact with it and watch what the server receives — then flip to Code to see the exact Java behind it. No JavaScript, anywhere.");

        p.add(sectionTitle("Interactive — try it, then read the code"));

        // 1) TextBox — the value is read by a server-side handler
        final PTextBox tb = Element.newPTextBox();
        final PElement tbEcho = echoIdle("server: waiting for input… (type, then press Enter)");
        tb.addValueChangeHandler(e -> echoSet(tbEcho, "server received → \"" + e.getData() + "\""));
        p.add(exampleCard("PTextBox",
            "A text field whose value is read by a Java handler the moment it changes (Enter or blur).",
            previewWithEcho(tb, tbEcho), snippet_exTextBox(), costSpec("PTextBox")));

        // 2) Button — every click is a server-side lambda
        final PButton btn = Element.newPButton("Click me");
        final int[] clicks = { 0 };
        final PElement btnEcho = echoIdle("server: not clicked yet");
        btn.addClickHandler(e -> echoSet(btnEcho, "server handled click #" + (++clicks[0]) + " (pure Java lambda)"));
        p.add(exampleCard("PButton",
            "Every click invokes a Java lambda on the JVM. Only a tiny binary delta crosses the wire.",
            previewWithEcho(btn, btnEcho), snippet_exButton(), costSpec("PButton")));

        // 3) CheckBox — state lives on the server
        final PCheckBox cb = Element.newPCheckBox("Enable feature");
        final PElement cbEcho = echoIdle("server: checked = false");
        cb.addValueChangeHandler(e -> echoSet(cbEcho, "server: checked = " + e.getData()));
        p.add(exampleCard("PCheckBox",
            "The checked state lives on the server. The handler fires the instant the box toggles.",
            previewWithEcho(cb, cbEcho), snippet_exCheckBox(), costSpec("PCheckBox")));

        // 4) RadioButton group — mutually-exclusive selection
        final PFlowPanel radios = Element.newPFlowPanel();
        radios.setStyleProperty("display", "flex");
        radios.setStyleProperty("gap", "16px");
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("size");
        final PElement rbEcho = echoIdle("server: no selection");
        for (final String opt : List.of("Small", "Medium", "Large")) {
            final PRadioButton rb = Element.newPRadioButton(opt);
            group.addRadioButton(rb);
            rb.addValueChangeHandler(e -> {
                if (Boolean.TRUE.equals(e.getData())) echoSet(rbEcho, "server: selected = " + opt);
            });
            radios.add(rb);
        }
        p.add(exampleCard("PRadioButton (group)",
            "Grouped radios share a name; selecting one clears the others and the server learns the choice.",
            previewWithEcho(radios, rbEcho), snippet_exRadio(), costSpec("PRadioButton")));

        // ── The long tail, kept compact for completeness ───────────────────────
        p.add(sectionTitle("More input widgets"));
        p.add(labeled("TextArea", Element.newPTextArea()));
        p.add(labeled("Password", Element.newPPasswordTextBox("Password")));
        p.add(labeled("RichText editor", buildRichText()));
        final PPushButton push = Element.newPPushButton(Element.newPImage());
        push.setText("Push me");
        p.add(labeled("PushButton", push));

        p.add(sectionTitle("Date & Time"));
        p.add(labeled("DateBox dd/MM/yyyy", Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy"))));
        p.add(labeled("DateBox with picker", Element.newPDateBox(Element.newPDatePicker(), new SimpleDateFormat("yyyy/MM/dd"))));
        p.add(labeled("DateBox with time", buildDateBoxWithLog()));
        p.add(labeled("DatePicker", Element.newPDatePicker()));
        p.add(labeled("DateBox (reset)", createDateBox()));

        p.add(sectionTitle("Selection"));
        p.add(labeled("ListBox (multi)", createListBox()));
        p.add(labeled("TwinListBox", new PTwinListBox<>()));
        p.add(labeled("MenuBar", createMenu()));

        p.add(sectionTitle("Form fields"));
        p.add(labeled("String FormField", new StringTextBoxFormField("String")));
        p.add(labeled("Color FormField", new ColorInputFormField("Color")));

        p.add(sectionTitle("File"));
        p.add(labeled("FileUpload", createPFileUpload()));

        return p;
    }

    // ── Tab: Layouts ─────────────────────────────────────────────────────────

    private PFlowPanel buildLayoutsTab(final List<PScheduler.UIRunnable> schedulers) {
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

    /** A small labelled box used to fill the layout demos. */
    private static PLabel chip(final String text) {
        final PLabel l = Element.newPLabel(text);
        l.addStyleName("lay-chip");
        return l;
    }

    /** Layout panels need a definite size: give them a framed 180 px box. */
    private static PWidget sized(final PWidget w) {
        w.setHeight("180px");
        w.setWidth("min(560px,100%)");
        w.addStyleName("lay-frame");
        return w;
    }

    // ── Tab: Data ────────────────────────────────────────────────────────────

    private PFlowPanel buildDataTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();

        pageTitle(p, "Data", "Trees and tables whose content is pushed from the server.");

        p.add(sectionTitle("Tree"));
        p.add(createTree());

        p.add(sectionTitle("RefreshableDataGrid — 50 rows refreshed every 200 ms"));
        final PScrollPanel gridFrame = Element.newPScrollPanel();
        gridFrame.addStyleName("lay-frame");
        gridFrame.addStyleName("data-table");
        gridFrame.setHeight("280px");
        gridFrame.setWidget(createGrid(schedulers));
        p.add(gridFrame);

        p.add(sectionTitle("FlexTable"));
        final com.ponysdk.core.ui.basic.PFlexTable flex = Element.newPFlexTable();
        flex.addStyleName("data-table");
        final String[][] fx = { { "Pair", "Bid", "Ask", "Spread" }, { "EUR/USD", "1.0851", "1.0853", "0.2" },
                                { "GBP/USD", "1.2710", "1.2713", "0.3" }, { "USD/JPY", "149.21", "149.24", "0.3" } };
        for (int row = 0; row < fx.length; row++) {
            for (int col = 0; col < fx[row].length; col++) {
                final PLabel cell = Element.newPLabel(fx[row][col]);
                if (row == 0) cell.addStyleName("data-th");
                flex.setWidget(row, col, cell);
            }
        }
        p.add(flex);

        p.add(sectionTitle("Grid 2×3"));
        final com.ponysdk.core.ui.basic.PGrid grid2 = Element.newPGrid(2, 3);
        grid2.addStyleName("data-table");
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) grid2.setWidget(row, col, chip("r" + row + " · c" + col));
        }
        p.add(grid2);

        return p;
    }

    // ── Tab: DataGrid ────────────────────────────────────────────────────────

    private PFlowPanel buildDataGridTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "DataGrid", "52 columns, up to 10 000 rows. Sort, filter, resize, pin and select: the data and "
            + "the logic stay on the server, only the visible window of rows is sent to the browser.");
        buildDataGridInto(p);
        return p;
    }

    // ── Tab: WebComponents ───────────────────────────────────────────────────

    private PFlowPanel buildWebCompTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Web Components", "Standard custom elements driven from Java: attributes, properties, events, "
            + "slots and methods. Object properties travel as JSON merge-patches, only the changed keys.");
        buildWebComponentsInto(p);
        return p;
    }

    // ── Tab: Perf ─────────────────────────────────────────────────────────────

    private PFlowPanel buildPerfTab(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
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
    private static final class Sparkline {
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

    /**
     * Nobody is looking (background tab, minimised window) or the connection is suspended (its writes would
     * pile up in the reconnection buffer): periodic showcase work skips the tick, no work and no bytes.
     */
    private static boolean idle() {
        return UIContext.get().isSuspended() || !PWindow.getMain().isShown();
    }

    private static PElement el(final String cssClass, final String text) {
        final PElement e = Element.newDiv();
        for (final String c : cssClass.split(" ")) e.addStyleName(c);
        if (!text.isEmpty()) e.setInnerText(text);
        return e;
    }

    private static PFlowPanel perfCard(final PElement... elements) {
        final PFlowPanel c = Element.newPFlowPanel();
        c.addStyleName("perf-card");
        for (final PElement e : elements) c.add(e);
        return c;
    }

    private static PFlowPanel highlightBox(final String icon, final String title, final String desc) {
        final PFlowPanel box = Element.newPFlowPanel();
        box.addStyleName("highlight-box");
        final PFlowPanel txt = Element.newPFlowPanel();
        final PElement ttl = Element.newDiv(); ttl.addStyleName("highlight-title"); ttl.setInnerText(title);
        final PElement dsc = Element.newDiv(); dsc.addStyleName("highlight-desc"); dsc.setInnerText(desc);
        txt.add(ttl, dsc);
        box.add(txt);
        return box;
    }

    // ── Tab: Code ─────────────────────────────────────────────────────────────

    private static PFlowPanel buildCodeTab() {
        final PFlowPanel p = section();
        pageTitle(p, "How It Works", "PonySDK lets you build rich UIs in pure Java — no JavaScript, no REST, no serialization.");

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

    private static PFlowPanel codeSnippet(final String title, final String lang, final String html) {
        final PFlowPanel wrap = Element.newPFlowPanel();
        wrap.addStyleName("code-snippet");
        final PFlowPanel hdr = Element.newPFlowPanel();
        hdr.addStyleName("code-header");
        final PElement d1 = Element.newDiv(); d1.addStyleName("code-dot"); d1.setAttribute("style", "background:#ff5f57");
        final PElement d2 = Element.newDiv(); d2.addStyleName("code-dot"); d2.setAttribute("style", "background:#febc2e");
        final PElement d3 = Element.newDiv(); d3.addStyleName("code-dot"); d3.setAttribute("style", "background:#28c840");
        final PElement t = Element.newDiv(); t.addStyleName("code-title"); t.setInnerText(title);
        final PElement l = Element.newDiv(); l.addStyleName("code-lang"); l.setInnerText(lang);
        hdr.add(d1, d2, d3, t, l);
        final PElement body = Element.newDiv();
        body.addStyleName("code-body");
        body.setInnerHTML(html);
        wrap.add(hdr, body);
        return wrap;
    }

    private static String kw(final String s) { return "<span class='code-kw'>" + s + "</span>"; }
    private static String ty(final String s) { return "<span class='code-type'>" + s + "</span>"; }
    private static String st(final String s) { return "<span class='code-str'>\"" + s + "\"</span>"; }
    private static String cm(final String s) { return "<span class='code-comment'>// " + s + "</span>"; }
    private static String mt(final String s) { return "<span class='code-method'>" + s + "</span>"; }
    private static String ln(final String s) { return "<span class='code-line'>" + s + "</span>"; }

    private static String snippet_helloWorld() {
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

    private static String snippet_liveCounter() {
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

    private static String snippet_datagrid() {
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

    private static String snippet_protocol() {
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

    // ── Interactive example cards (a live widget + the exact Java behind it) ───

    /** A card pairing a live, interactive widget with the Java source that built it (Preview / Code). */
    private static PFlowPanel exampleCard(final String title, final String desc,
                                          final PWidget preview, final String codeHtml) {
        return exampleCard(title, desc, preview, codeHtml, null);
    }

    private static PFlowPanel exampleCard(final String title, final String desc,
                                          final PWidget preview, final String codeHtml, final CostSpec cost) {
        final PFlowPanel card = Element.newPFlowPanel();
        card.addStyleName("pony-ex");

        final PFlowPanel head = Element.newPFlowPanel();
        head.addStyleName("pony-ex-head");
        final PFlowPanel titleWrap = Element.newPFlowPanel();
        titleWrap.addStyleName("pony-ex-titlewrap");
        final PElement t = Element.newDiv(); t.addStyleName("pony-ex-title"); t.setInnerText(title);
        final PElement d = Element.newDiv(); d.addStyleName("pony-ex-desc"); d.setInnerText(desc);
        titleWrap.add(t, d);

        final PFlowPanel seg = Element.newPFlowPanel();
        seg.addStyleName("pony-ex-seg");
        final PButton bPreview = Element.newPButton("Preview");
        final PButton bCode = Element.newPButton("Code");
        bPreview.addStyleName("pony-ex-seg-on");
        seg.add(bPreview, bCode);
        final PButton bCost = cost != null ? Element.newPButton("Cost") : null;
        if (bCost != null) seg.add(bCost);
        head.add(titleWrap, seg);

        final PFlowPanel previewBody = Element.newPFlowPanel();
        previewBody.addStyleName("pony-ex-body");
        previewBody.addStyleName("pony-ex-preview");
        previewBody.add(preview);

        final PFlowPanel codeBody = Element.newPFlowPanel();
        codeBody.addStyleName("pony-ex-code");
        final PElement body = Element.newDiv();
        body.addStyleName("code-body");
        body.setInnerHTML(codeHtml);
        codeBody.add(body);
        codeBody.setVisible(false);
        final PFlowPanel costBody = cost != null ? costBody(cost) : null;
        if (costBody != null) costBody.setVisible(false);

        // The toggle itself runs on the server (setVisible → binary delta) — the value prop, demoed by the demo.
        final PWidget[] bodies = { previewBody, codeBody, costBody };
        final PButton[] buttons = { bPreview, bCode, bCost };
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] == null) continue;
            final int sel = i;
            buttons[i].addClickHandler(e -> {
                for (int j = 0; j < buttons.length; j++) {
                    if (buttons[j] == null) continue;
                    bodies[j].setVisible(j == sel);
                    if (j == sel) buttons[j].addStyleName("pony-ex-seg-on");
                    else buttons[j].removeStyleName("pony-ex-seg-on");
                }
            });
        }

        card.add(head, previewBody, codeBody);
        if (costBody != null) card.add(costBody);
        return card;
    }

    /** A live "server received…" readout — proves each interaction is handled in Java, server-side. */
    private static PElement echoIdle(final String text) {
        final PElement e = Element.newDiv();
        e.addStyleName("pony-ex-echo");
        e.addStyleName("pony-ex-echo-idle");
        e.setInnerText(text);
        return e;
    }

    private static void echoSet(final PElement echo, final String text) {
        echo.removeStyleName("pony-ex-echo-idle");
        echo.setInnerText(text);
    }

    /** Stacks a live control above its server-echo readout. */
    private static PFlowPanel previewWithEcho(final PWidget control, final PElement echo) {
        final PFlowPanel wrap = Element.newPFlowPanel();
        wrap.setStyleProperty("display", "flex");
        wrap.setStyleProperty("flex-direction", "column");
        wrap.setStyleProperty("gap", "14px");
        wrap.setStyleProperty("align-items", "flex-start");
        wrap.add(control);
        wrap.add(echo);
        return wrap;
    }

    private static String nu(final String s) { return "<span class='code-num'>" + s + "</span>"; }

    private static String snippet_exTextBox() {
        return ln(ty("PTextBox") + " box = " + ty("Element") + "." + mt("newPTextBox") + "();") +
            ln(cm("fires when the value changes (Enter / blur)")) +
            ln("box." + mt("addValueChangeHandler") + "(e ->") +
            ln("    echo." + mt("setText") + "(" + st("server received → ") + " + e." + mt("getData") + "()));") +
            ln(ty("PWindow") + "." + mt("getMain") + "()." + mt("add") + "(box);");
    }

    private static String snippet_exButton() {
        return ln(ty("PButton") + " btn = " + ty("Element") + "." + mt("newPButton") + "(" + st("Click me") + ");") +
            ln(ty("int") + "[] clicks = {" + nu("0") + "};") +
            ln(cm("every click is a Java lambda — runs on the JVM")) +
            ln("btn." + mt("addClickHandler") + "(e ->") +
            ln("    echo." + mt("setText") + "(" + st("handled click #") + " + (++clicks[" + nu("0") + "])));");
    }

    private static String snippet_exCheckBox() {
        return ln(ty("PCheckBox") + " cb = " + ty("Element") + "." + mt("newPCheckBox") + "(" + st("Enable feature") + ");") +
            ln(cm("state lives on the server, not in the browser")) +
            ln("cb." + mt("addValueChangeHandler") + "(e ->") +
            ln("    echo." + mt("setText") + "(" + st("checked = ") + " + e." + mt("getData") + "()));");
    }

    private static String snippet_exRadio() {
        return ln(ty("PRadioButtonGroup") + " group = " + ty("Element") + "." + mt("newPRadioButtonGroup") + "(" + st("size") + ");") +
            ln(kw("for") + " (" + ty("String") + " opt : " + ty("List") + "." + mt("of") + "(" + st("Small") + ", " + st("Medium") + ", " + st("Large") + ")) {") +
            ln("  " + ty("PRadioButton") + " rb = " + ty("Element") + "." + mt("newPRadioButton") + "(opt);") +
            ln("  group." + mt("addRadioButton") + "(rb);  " + cm("mutual exclusion")) +
            ln("  rb." + mt("addValueChangeHandler") + "(e -> {") +
            ln("    " + kw("if") + " (" + ty("Boolean") + ".TRUE." + mt("equals") + "(e." + mt("getData") + "()))") +
            ln("      echo." + mt("setText") + "(" + st("selected = ") + " + opt);") +
            ln("  });") +
            ln("  row." + mt("add") + "(rb);") +
            ln("}");
    }

    // ── Tab: Misc ────────────────────────────────────────────────────────────

    private PFlowPanel buildMiscTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Misc", "HTML, scripts, frames and cookies, all handled from the server.");

        p.add(sectionTitle("HTML / Anchors"));
        p.add(labeled("HTML", Element.newPHTML("Hello <b>HTML</b>")));
        p.add(labeled("Anchor", Element.newPAnchor("PonySDK site", "https://github.com/smartTrade-OpenSource/PonySDK")));

        p.add(sectionTitle("Script"));
        final PButton alertBtn = Element.newPButton("Run JS alert");
        alertBtn.addClickHandler(e -> PScript.execute(PWindow.getMain(), "alert('Hello from PScript!')"));
        p.add(labeled("PScript", alertBtn));

        p.add(sectionTitle("Toolbar"));
        final PToolbar toolbar = new PToolbar();
        toolbar.addStyleName("misc-toolbar");
        final PLabel toolbarEcho = Element.newPLabel("server: no action yet");
        toolbarEcho.addStyleName("pony-ex-echo");
        for (final String action : new String[] { "New", "Open", "Save", "Export" }) {
            final PButton b = Element.newPButton(action);
            b.addClickHandler(e -> toolbarEcho.setText("server: " + action + " clicked"));
            toolbar.add(b);
        }
        p.add(toolbar);
        p.add(toolbarEcho);

        p.add(sectionTitle("Frame"));
        final PFrame frame = Element.newPFrame("html/frame-demo.html"); // relative: same origin as the showcase
        frame.setHeight("260px");
        frame.setWidth("min(640px,100%)");
        frame.addStyleName("lay-frame");
        p.add(frame);

        p.add(sectionTitle("Cookies"));
        final PCookies cookies = new PCookies();
        cookies.setCookie("showcase", "true");
        final PLabel cookieLabel = Element.newPLabel("Cookie 'showcase' set to: true");
        p.add(cookieLabel);

        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static PFlowPanel section() {
        return Element.newPFlowPanel();
    }

    private PFlowPanel buildFlexLayoutTab() {
        final PFlowPanel p = Element.newPFlowPanel();
        pageTitle(p, "FlexLayout",
            "Dockable panel layout add-on — drag tabs, split views, collapsible sidebars, themes. "
                + "Its creation arguments travel over the v3 pure-binary protocol (no JSON).");
        // The FlexLayout panel positions its content absolutely, so it needs a parent with a
        // definite height; the showcase content area scrolls, so we give it a fixed-height box.
        final PFlowPanel box = Element.newPFlowPanel();
        box.setStyleProperty("height", "78vh");
        box.setStyleProperty("min-height", "480px");
        box.setStyleProperty("margin-top", "16px");
        box.setStyleProperty("border", "1px solid var(--border)");
        box.setStyleProperty("border-radius", "10px");
        box.setStyleProperty("overflow", "hidden");
        box.add(FlexLayoutDemoEntryPoint.buildDemo(false));
        p.add(box);
        return p;
    }

    private static void pageTitle(final PFlowPanel p, final String title, final String desc) {
        final PElement h = Element.newDiv(); h.addStyleName("pony-page-title"); h.setInnerText(title);
        final PElement d = Element.newDiv(); d.addStyleName("pony-page-desc"); d.setInnerText(desc);
        p.add(h, d);
    }

    private static PElement sectionTitle(final String text) {
        final PElement h = Element.newDiv();
        h.addStyleName("pony-section-title");
        h.setInnerText(text);
        return h;
    }

    private static PFlowPanel labeled(final String label, final PWidget widget) {
        final PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("pony-row");
        final PElement lbl = Element.newDiv();
        lbl.addStyleName("pony-label");
        lbl.setInnerText(label);
        row.add(lbl);
        row.add(widget);
        final String costName = ROW_COSTS.get(label);
        if (costName != null) row.add(rowCost(costSpec(costName)));
        return row;
    }

    /** "Cost" button of a row: measures the component in place and shows memory / create / update on the wire. */
    private static PFlowPanel rowCost(final CostSpec spec) {
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

    private static PFlowPanel labeled(final String label, final IsPWidget widget) {
        return labeled(label, widget.asWidget());
    }

    private static PWidget buildRichText() {
        final PFlowPanel p = Element.newPFlowPanel();
        final PRichTextArea rta = Element.newPRichTextArea();
        // the toolbar links to the area: the area is created first, the toolbar is still shown above it
        p.setStyleProperty("display", "flex");
        p.setStyleProperty("flex-direction", "column-reverse");
        p.add(rta);
        p.add(Element.newPRichTextToolbar(rta));
        return p;
    }

    private static PDateBox buildDateBoxWithLog() {
        final PDateBox db = Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"), true);
        db.addValueChangeHandler(e -> log.debug("DateBox value: {}", e.getData()));
        return db;
    }

    private void buildDataGridInto(final PFlowPanel container) {
        final DefaultDataGridView<Integer, MyRow> simpleGridView = new DefaultDataGridView<>();
        simpleGridView.setRefreshOnColumnVisibilityChanged(true);

        final ColumnVisibilitySelectorDataGridView<Integer, MyRow> columnVisibilitySelectorDataGridView = new ColumnVisibilitySelectorDataGridView<>(simpleGridView);
        final RowSelectorColumnDataGridView<Integer, MyRow> rowSelectorColumnDataGridView = new RowSelectorColumnDataGridView<>(columnVisibilitySelectorDataGridView);
        final ColumnFilterFooterDataGridView<Integer, MyRow> columnFilterFooterDataGridView = new ColumnFilterFooterDataGridView<>(rowSelectorColumnDataGridView);
        final ConfigSelectorDataGridView<Integer, MyRow> configSelectorDataGridView = new ConfigSelectorDataGridView<>(columnFilterFooterDataGridView, "DEFAULT");

        final DataGridView<Integer, MyRow> gridView = configSelectorDataGridView;
        gridView.setAdapter(new DataGridAdapter<>() {
            private final List<ColumnDefinition<MyRow>> columns = new ArrayList<>();
            {
                for (char c = 'a'; c <= 'z'; c++) {
                    final String ss = c + "";
                    columns.add(new DefaultColumnDefinition<>(ss, v -> v.getValue(ss), (v, s) -> v.putValue(ss, s)));
                }
                for (char c = 'A'; c <= 'Z'; c++) {
                    final String ss = c + "";
                    columns.add(new DefaultColumnDefinition<>(ss, v -> v.getValue(ss), (v, s) -> v.putValue(ss, s)));
                }
            }
            @Override public void onUnselectRow(final IsPWidget r) { r.asWidget().removeStyleName("selected-row"); }
            @Override public void onSelectRow(final IsPWidget r) { r.asWidget().addStyleName("selected-row"); }
            @Override public boolean isAscendingSortByInsertionOrder() { return false; }
            @Override public Integer getKey(final MyRow v) { return v.id; }
            @Override public List<ColumnDefinition<MyRow>> getColumnDefinitions() { return columns; }
            @Override public int compareDefault(final MyRow v1, final MyRow v2) { return 0; }
            @Override public void onCreateHeaderRow(final IsPWidget r) { r.asWidget().getParent().asWidget().addStyleName("dg-head"); }
            @Override public void onCreateFooterRow(final IsPWidget r) { r.asWidget().getParent().asWidget().addStyleName("dg-head"); }
            @Override public void onCreateRow(final IsPWidget r) {}
            @Override public boolean hasHeader() { return true; }
            @Override public boolean hasFooter() { return false; }
            @Override public IsPWidget createLoadingDataWidget() {
                final PComplexPanel div = Element.newDiv();
                div.setWidth("100%"); div.setHeight("100%");
                div.setStyleProperty("background-color", "rgba(255,255,255,.7)");
                return div;
            }
            @Override public void onCreateColumnResizer(final IsPWidget r) {}
            @Override public boolean isSelectionEnabled() { return true; }
        });
        gridView.setPollingDelayMillis(250L);

        final DataGridController<Integer, MyRow> controller = gridView.getController();
        gridView.asWidget().setHeight("500px");
        gridView.asWidget().setWidth("100%");
        gridView.asWidget().setStyleProperty("resize", "vertical");
        gridView.asWidget().addStyleName("dg-frame");
        gridView.asWidget().setStyleProperty("overflow", "hidden");

        // Controls row
        final PFlowPanel controls = Element.newPFlowPanel();
        controls.addStyleName("dg-controls");

        final PTextBox pollingDelay = Element.newPTextBox();
        pollingDelay.setPlaceholder("Polling delay ms");
        pollingDelay.setStyleProperty("width", "140px");
        final PButton changePollingDelay = Element.newPButton("Set polling delay");
        changePollingDelay.addClickHandler(e -> gridView.setPollingDelayMillis(Integer.parseInt(pollingDelay.getText().trim())));

        final PButton clearSortsButton = Element.newPButton("Clear sorts");
        clearSortsButton.addClickHandler(e -> gridView.clearSorts(false));

        controls.add(pollingDelay, changePollingDelay, clearSortsButton);
        controls.add(configSelectorDataGridView.getDecoratorWidget());
        // 52 column checkboxes: collapsed by default
        final PDisclosurePanel columnsPicker = Element.newPDisclosurePanel("Columns…");
        columnsPicker.addStyleName("dg-columns");
        columnsPicker.setContent(columnVisibilitySelectorDataGridView.getDecoratorWidget());
        controls.add(columnsPicker);

        container.add(controls);
        container.add(gridView);

        gridView.addRowAction(UISampleEntryPoint.class, new RowAction<>() {
            @Override public boolean testRow(final MyRow t, final int index) { return (index & 1) == 0; }
            @Override public void cancel(final IsPWidget row) { row.asWidget().removeStyleName("unpair-row"); }
            @Override public void apply(final IsPWidget row) { row.asWidget().addStyleName("unpair-row"); }
            @Override public boolean isActionApplied(final IsPWidget row) { return row.asWidget().hasStyleName("unpair-row"); }
        });

        controller.setBound(false);
        final int PAGE_SIZE = 200;
        final int TOTAL_ROWS = 10_000;
        final int[] loaded = {0};

        // Load first page
        for (int i = 0; i < PAGE_SIZE; i++) {
            controller.setData(createMyRow(i));
        }
        loaded[0] = PAGE_SIZE;
        controller.setBound(true);

        // "Load more" button
        final PButton loadMoreButton = Element.newPButton("Load more (" + PAGE_SIZE + " rows)");
        loadMoreButton.setStyleProperty("margin-top", "8px");
        loadMoreButton.addClickHandler(e -> {
            if (loaded[0] >= TOTAL_ROWS) {
                loadMoreButton.setEnabled(false);
                loadMoreButton.setText("All rows loaded");
                return;
            }
            controller.setBound(false);
            final int end = Math.min(loaded[0] + PAGE_SIZE, TOTAL_ROWS);
            for (int i = loaded[0]; i < end; i++) {
                controller.setData(createMyRow(i));
            }
            loaded[0] = end;
            controller.setBound(true);
            if (loaded[0] >= TOTAL_ROWS) {
                loadMoreButton.setEnabled(false);
                loadMoreButton.setText("All rows loaded (" + TOTAL_ROWS + ")");
            } else {
                loadMoreButton.setText("Load more (" + loaded[0] + "/" + TOTAL_ROWS + ")");
            }
        });
        container.add(loadMoreButton);
    }

    private void buildWebComponentsInto(final PFlowPanel container) {
        final PLabel statusLabel = Element.newPLabel("Status: waiting for events...");
        statusLabel.setStyleProperty("color", "var(--text-3)");
        statusLabel.setStyleProperty("font-style", "italic");

        // my-counter
        final PWebComponent counter = new PWebComponent("my-counter");
        counter.attr("label", "Clicks");
        counter.property("count").set(0);
        counter.onEvent("count-changed", event -> {
            final String detail = event.containsKey(ClientToServerModel.WC_EVENT_DETAIL.toStringValue())
                ? event.getString(ClientToServerModel.WC_EVENT_DETAIL.toStringValue()) : "{}";
            statusLabel.setText("count-changed: " + detail);
        });

        // my-dashboard
        final PWebComponent dashboard = new PWebComponent("my-dashboard");
        dashboard.attr("title", "PonySdk Dashboard");
        final PButton refreshBtn = Element.newPButton("Refresh");
        final PButton exportBtn = Element.newPButton("Export");
        dashboard.slot("toolbar").add(refreshBtn, exportBtn);
        final PFlowPanel contentPanel = Element.newPFlowPanel();
        final PTextBox searchBox = Element.newPTextBox();
        searchBox.setPlaceholder("Search (PTextBox inside WC)...");
        contentPanel.add(Element.newPLabel("PLabel inside 'content' slot"), searchBox);
        dashboard.slot("content").add(contentPanel);
        dashboard.slot("footer").add(statusLabel);
        refreshBtn.addClickHandler(e -> { statusLabel.setText("Refreshed at " + new java.util.Date()); dashboard.attr("theme", "light"); });
        exportBtn.addClickHandler(e -> { statusLabel.setText("Export triggered!"); dashboard.attr("theme", "dark"); });

        // my-card
        final PWebComponent card = new PWebComponent("my-card");
        card.attr("title", "PonySdk Card");
        card.property("content").set("\"Card body text set from server via property API.\"");
        final PButton likeBtn = Element.newPButton("Like");
        final PButton shareBtn = Element.newPButton("Share");
        card.slot("actions").add(likeBtn, shareBtn);
        likeBtn.addClickHandler(e -> statusLabel.setText("Liked!"));
        shareBtn.addClickHandler(e -> statusLabel.setText("Shared!"));

        // Counter controls
        final PFlowPanel counterControls = Element.newPFlowPanel();
        counterControls.setStyleProperty("display", "flex");
        counterControls.setStyleProperty("gap", "8px");
        final PButton resetBtn = Element.newPButton("Reset counter");
        resetBtn.addClickHandler(e -> { counter.call("reset"); statusLabel.setText("reset() called"); });
        final PButton setCountBtn = Element.newPButton("Set count = 42");
        setCountBtn.addClickHandler(e -> { counter.property("count").set(42); statusLabel.setText("count set to 42"); });
        counterControls.add(resetBtn, setCountBtn);

        // my-stats — object property updated through JSON merge-patch (only changed keys travel)
        final PWebComponent stats = new PWebComponent("my-stats");
        final int[] trades = { 0 };
        final String statsTemplate = "{\"desk\":\"FX Spot EUR/USD\",\"trader\":\"demo-trader\",\"trades\":%d%s}";
        stats.property("stats").set(String.format(statsTemplate, 0, ",\"note\":\"initial snapshot\""));
        final PFlowPanel statsControls = Element.newPFlowPanel();
        statsControls.setStyleProperty("display", "flex");
        statsControls.setStyleProperty("gap", "8px");
        final PButton tradeBtn = Element.newPButton("Patch: +1 trade");
        tradeBtn.addClickHandler(e -> {
            trades[0]++;
            stats.property("stats").set(String.format(statsTemplate, trades[0], ",\"note\":\"initial snapshot\""));
            statusLabel.setText("stats patched: trades=" + trades[0]);
        });
        final PButton dropNoteBtn = Element.newPButton("Patch: drop note");
        dropNoteBtn.addClickHandler(e -> {
            stats.property("stats").set(String.format(statsTemplate, trades[0], ""));
            statusLabel.setText("stats patched: note removed");
        });
        statsControls.add(tradeBtn, dropNoteBtn);

        // Slot children added after the component is live
        final int[] toolbarItems = { 0 };
        final PButton addToolbarItemBtn = Element.newPButton("Add toolbar item");
        addToolbarItemBtn.addClickHandler(e -> {
            final PLabel item = Element.newPLabel("Item " + ++toolbarItems[0]);
            item.addStyleName("dyn-toolbar-item");
            dashboard.slot("toolbar").add(item);
        });
        counterControls.add(addToolbarItemBtn);

        container.add(exampleCard("<my-counter>",
            "An attribute and a property set from Java, a method call, and the element's own event handled on the server.",
            stack(counter, counterControls),
            ln(ty("PWebComponent") + " counter = " + kw("new") + " " + ty("PWebComponent") + "(" + st("my-counter") + ");")
                + ln("counter." + mt("attr") + "(" + st("label") + ", " + st("Clicks") + ");")
                + ln("counter." + mt("property") + "(" + st("count") + ")." + mt("set") + "(0);")
                + ln("counter." + mt("onEvent") + "(" + st("count-changed") + ", e -> status." + mt("setText") + "(...));")
                + ln("reset." + mt("addClickHandler") + "(e -> counter." + mt("call") + "(" + st("reset") + "));"),
            costSpec("PWebComponent <my-counter>")));
        container.add(exampleCard("<my-stats>",
            "An object property: each change travels as a JSON merge-patch carrying only the keys that changed.",
            stack(stats, statsControls),
            ln(cm("only \"trades\" travels; a key set to null is removed on the element"))
                + ln("stats." + mt("property") + "(" + st("stats") + ")." + mt("set") + "(json);")));
        container.add(exampleCard("<my-dashboard>",
            "Named slots filled with PonySDK widgets - buttons, a text box, a label - and children added while it is live.",
            dashboard,
            ln("dashboard." + mt("slot") + "(" + st("toolbar") + ")." + mt("add") + "(refresh, export);")
                + ln("dashboard." + mt("slot") + "(" + st("content") + ")." + mt("add") + "(panel);")
                + ln("dashboard." + mt("slot") + "(" + st("footer") + ")." + mt("add") + "(status);")));
        container.add(exampleCard("<my-card>",
            "A property set from the server and an actions slot whose buttons are handled in Java.",
            card,
            ln("card." + mt("property") + "(" + st("content") + ")." + mt("set") + "(" + st("Card body text...") + ");")
                + ln("card." + mt("slot") + "(" + st("actions") + ")." + mt("add") + "(like, share);")));
    }

    /** Widgets stacked vertically (an example's preview). */
    private static PFlowPanel stack(final PWidget... widgets) {
        final PFlowPanel p = Element.newPFlowPanel();
        p.addStyleName("pony-stack");
        for (final PWidget w : widgets) p.add(w);
        return p;
    }

    private static class MyRow {

        private final int id;
        private Map<String, String> map;
        private final String format;

        public MyRow(final int id) {
            super();
            this.id = id;
            format = String.format("%09d", id);
        }

        public void putValue(final String key, final String value) {
            if (map == null) map = new ConcurrentHashMap<>();
            map.put(key, value);
        }

        public String getValue(final String key) {
            if (map != null) {
                final String v = map.get(key);
                if (v != null) return v;
            }
            return key + format;
        }

        @Override
        public MyRow clone() {
            final MyRow model = new MyRow(id);
            if (map == null) return model;
            model.map = new HashMap<>(map);
            return model;
        }

        @Override
        public String toString() {
            return "SampleModel [id=" + id + "]";
        }

    }

    private static MyRow createMyRow(final int index) {
        return new MyRow(index);
    }

    private void testVisibilityHandler(final PComplexPanel container, final List<PScheduler.UIRunnable> schedulers) {
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

        final PLabel label = Element.newPLabel("Server counter: " + a++);
        subPanel.add(label);

        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            a++;
            if (subPanel.isShown() && subPanel.getWindow() != null && subPanel.getWindow().isShown()) updateLabel(label, String.valueOf(a));
        }, Duration.ofSeconds(1)));

        subPanel.addVisibilityHandler(event -> {
            if (event.getData() && subPanel.getWindow() != null) {
                subPanel.getWindow().addVisibilityHandler(windowEvent -> {
                    if (windowEvent.getData()) {
                        log.debug("Force refresh, because window became visible");
                        updateLabel(label, String.valueOf(a));
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
                updateLabel(label, String.valueOf(a));
            }
        });
        button.addClickHandler(event -> {
            visibilityLabel.setText("Visibility : " + subPanel.isShown());
        });
    }

    private static void updateLabel(final PLabel label, final String text) {
        log.debug("Update label {}", text);
        label.setText("Server counter: " + text);
    }

    public PFlowPanel createPFileUpload() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PFileUpload fileUpload = Element.newPFileUpload();
        fileUpload.setName("file");
        panel.add(fileUpload);
        fileUpload.addChangeHandler(event -> {
            final PFileUpload pFileUpload = (PFileUpload) event.getSource();
            log.debug("File name: {}", pFileUpload.getFileName());
            log.debug("File size: {} bytes", pFileUpload.getFileSize());
        });
        fileUpload.addStreamHandler((request, response, context) -> {
            try {
                final List<FileItem> items = new JakartaServletFileUpload(DiskFileItemFactory.builder().get()).parseRequest(request);
                for (final FileItem item : items) {
                    if (!item.isFormField()) readFileItem(item);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        final PButton button = Element.newPButton("Submit");
        button.addClickHandler(event -> fileUpload.submit());
        panel.add(button);
        return panel;
    }

    private void readFileItem(final FileItem<?> item) throws IOException, FileNotFoundException {
        // Store the uploaded file on the server (don't forget to remove)
        final String fileName = FilenameUtils.getName(item.getName());
        final InputStream fileContent = item.getInputStream();
        final File uploadedFile = File.createTempFile(fileName, "fileUpload");
        IOUtils.copy(fileContent, new FileOutputStream(uploadedFile));
        uploadedFile.deleteOnExit();

        // Read directly the input stream
        final BufferedReader reader = new BufferedReader(new InputStreamReader(item.getInputStream(), "UTF-8"));
        final StringBuilder value = new StringBuilder();
        final char[] buffer = new char[1024];
        for (int length = 0; (length = reader.read(buffer)) > 0;) {
            value.append(buffer, 0, length);
        }
        log.debug("File content: {}", value);
    }

    private static final class Data {

        protected Integer key;
        protected String value;

        public Data(final Integer key, final String value) {
            this.key = key;
            this.value = value;
        }
    }

    private RefreshableDataGrid<Integer, Data> createGrid(final List<PScheduler.UIRunnable> schedulers) {
        final AbstractGridWidget listView = new GridTableWidget();
        listView.setStyleProperty("table-layout", "fixed");
        final RefreshableDataGrid<Integer, Data> grid = new RefreshableDataGrid<>(listView);

        final DataGridColumnDescriptor<Data, Data> columnDescriptor1 = new DataGridColumnDescriptor<>();
        columnDescriptor1.setCellRenderer(new CellRenderer<UISampleEntryPoint.Data, PLabel>() {

            @Override
            public void update(final Data value, final Cell<Data, PLabel> current) {
                current.getWidget().setText(value.key + "");
            }

            @Override
            public PLabel render(final int row, final Data value) {
                return Element.newPLabel(value.key + "");
            }
        });
        columnDescriptor1.setHeaderCellRenderer(() -> Element.newPLabel("Key"));
        columnDescriptor1.setValueProvider(new IdentityValueProvider<>());
        grid.addDataGridColumnDescriptor(columnDescriptor1);

        for (int i = 0; i < 6; i++) {
            final String header = "Value " + (i + 1);

            final DataGridColumnDescriptor<Data, Data> columnDescriptor3 = new DataGridColumnDescriptor<>();
            columnDescriptor3.setCellRenderer(new CellRenderer<UISampleEntryPoint.Data, PLabel>() {

                @Override
                public void update(final Data value, final Cell<Data, PLabel> current) {
                    current.getWidget().setText(value.value);
                }

                @Override
                public PLabel render(final int row, final Data value) {
                    return Element.newPLabel(value.value);
                }
            });
            columnDescriptor3.setHeaderCellRenderer(() -> Element.newPLabel(header));
            columnDescriptor3.setValueProvider(new IdentityValueProvider<>());
            grid.addDataGridColumnDescriptor(columnDescriptor3);
        }

        grid.setData(0, 1, new Data(1, "AA"));
        grid.setData(1, 2, new Data(2, "BB"));
        final Data data = new Data(3, "CC");
        grid.setData(2, 3, data);

        final AtomicInteger i = new AtomicInteger();
        schedulers.add(PScheduler.scheduleWithFixedDelay(() -> {
            if (idle()) return;
            for (int key = 1; key < 50; key++) {
                grid.setData(key - 1, key, new Data(key, "" + i.incrementAndGet()));
            }
        }, Duration.ofMillis(500), Duration.ofMillis(200)));

        return grid;
    }

    private static final PListBox createListBox() {
        final PListBox pListBox = Element.newPListBox(true);
        pListBox.addItem("A");
        pListBox.addItem("B");
        pListBox.insertItem("C", 1);
        pListBox.addItemsInGroup("sport", "Baseball", "Basketball", "Football", "Hockey", "Water Polo");
        return pListBox;
    }

    private static final PMenuBar createMenu() {
        final PMenuBar pMenuBar = Element.newPMenuBar(true);
        pMenuBar.addItem(Element.newPMenuItem("Menu 1", Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 2", true, Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 3", () -> log.debug("Menu click")));
        pMenuBar.addSeparator();
        return pMenuBar;
    }

    private static final PFlowPanel createDateBox() {
        final PFlowPanel flowPanel = Element.newPFlowPanel();

        final PDatePicker datePicker = Element.newPDatePicker();
        final Date a = new Date();

        datePicker.setTransientEnabledOnDates(false, List.of(new Date(), new Date(a.getYear(), a.getMonth(), 26)));
        datePicker.addStyleToDates("toto", List.of(new Date()));

        final PDateBox dateBox = Element.newPDateBox(datePicker, new SimpleDateFormat("dd/MM/yyyy"));
        //dateBox.setValue(new Date(0));
        flowPanel.add(dateBox);
        datePicker.addShowRangeHandler(e -> {
            datePicker.setTransientEnabledOnDates(false, List.of(new Date(), new Date(a.getYear(), a.getMonth(), 26)));
            datePicker.addStyleToDates("toto", List.of(new Date()));
        });

        final PButton button = Element.newPButton("reset");
        button.addClickHandler(event -> dateBox.setValue(null));
        flowPanel.add(button);
        return flowPanel;
    }

    private static final PTree createTree() {
        final PTree tree = Element.newPTree();

        tree.addStyleName("data-tree");
        final PTreeItem fx = tree.add("FX desk");
        fx.add("EUR/USD");
        fx.add(0, Element.newPTreeItem("GBP/USD"));   // inserted before
        fx.add("USD/JPY");
        fx.setState(true);

        final PTreeItem rates = Element.newPTreeItem("Rates desk");
        final PTreeItem bund = rates.add(Element.newPTreeItem());
        bund.setText("Bund 10Y");                    // text set after creation
        rates.add(Element.newPTreeItem(Element.newPLabel("UST 10Y (widget item)")));
        tree.add(rates);
        rates.setState(true);

        bund.setSelected(true);

        return tree;
    }

}
