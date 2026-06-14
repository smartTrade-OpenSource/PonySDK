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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletResponse;

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
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PDatePicker;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFrame;
import com.ponysdk.core.ui.basic.PFunctionalLabel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PMenuBar;
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
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.view.ColumnFilterFooterDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ColumnVisibilitySelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ConfigSelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.ui.datagrid2.view.DataGridView.DecodeException;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;
import com.ponysdk.core.ui.datagrid2.view.RowSelectorColumnDataGridView;
import com.ponysdk.core.ui.eventbus2.EventBus.EventHandler;
import com.ponysdk.core.ui.form2.impl.formfield.ColorInputFormField;
import com.ponysdk.core.ui.form2.impl.formfield.NumberInputFormField;
import com.ponysdk.core.ui.form2.impl.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.core.ui.grid.AbstractGridWidget;
import com.ponysdk.core.ui.grid.GridTableWidget;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.refreshable.Cell;
import com.ponysdk.core.ui.list.refreshable.RefreshableDataGrid;
import com.ponysdk.core.ui.list.renderer.cell.CellRenderer;
import com.ponysdk.core.ui.list.valueprovider.IdentityValueProvider;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.core.ui.rich.PToolbar;
import com.ponysdk.core.ui.rich.PTwinListBox;
import com.ponysdk.core.ui.scene.AbstractScene;
import com.ponysdk.core.ui.scene.Router;
import com.ponysdk.core.ui.scene.Scene;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.page.addon.BinaryArgsAddOn;
import com.ponysdk.sample.client.page.addon.LoggerAddOn;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler {

    private static final Logger log = LoggerFactory.getLogger(UISampleEntryPoint.class);

    private PLabel mainLabel;
    int a = 0;
    private static int counter;

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setTerminalDataReceiver((object, instruction) -> log.debug("{} : {}", object, instruction));

        // Inject global styles
        PScript.execute(PWindow.getMain(), buildGlobalStyles());
        PScript.execute(PWindow.getMain(), buildModernTheme());

        // Header
        final PFlowPanel header = Element.newPFlowPanel();
        header.addStyleName("pony-header");

        final PFlowPanel logoWrap = Element.newPFlowPanel();
        logoWrap.addStyleName("pony-logo-wrap");
        final PElement logoIcon = Element.newDiv();
        logoIcon.addStyleName("pony-logo-icon");
        logoIcon.setInnerText("🐴");
        final PElement logoText = Element.newDiv();
        logoText.addStyleName("pony-logo-text");
        final PElement logoName = Element.newDiv();
        logoName.addStyleName("pony-logo");
        logoName.setInnerText("PonySDK");
        final PElement logoSub = Element.newDiv();
        logoSub.addStyleName("pony-subtitle");
        logoSub.setInnerText("WIDGET SHOWCASE");
        logoText.add(logoName, logoSub);
        logoWrap.add(logoIcon, logoText);

        final PElement divider = Element.newDiv();
        divider.addStyleName("pony-divider");

        // Header live stats
        final PFlowPanel hstats = Element.newPFlowPanel();
        hstats.addStyleName("pony-header-stats");
        final PElement latHVal = Element.newDiv(); latHVal.addStyleName("pony-hstat-val"); latHVal.setInnerText("—");
        final PElement latHLbl = Element.newDiv(); latHLbl.addStyleName("pony-hstat-lbl"); latHLbl.setInnerText("Latency ms");
        final PFlowPanel latHCard = Element.newPFlowPanel(); latHCard.addStyleName("pony-hstat"); latHCard.add(latHVal, latHLbl);
        final PElement heapHVal = Element.newDiv(); heapHVal.addStyleName("pony-hstat-val"); heapHVal.setInnerText("—");
        final PElement heapHLbl = Element.newDiv(); heapHLbl.addStyleName("pony-hstat-lbl"); heapHLbl.setInnerText("Heap MB");
        final PFlowPanel heapHCard = Element.newPFlowPanel(); heapHCard.addStyleName("pony-hstat"); heapHCard.add(heapHVal, heapHLbl);
        final PElement sessHVal = Element.newDiv(); sessHVal.addStyleName("pony-hstat-val"); sessHVal.setInnerText("1");
        final PElement sessHLbl = Element.newDiv(); sessHLbl.addStyleName("pony-hstat-lbl"); sessHLbl.setInnerText("Sessions");
        final PFlowPanel sessHCard = Element.newPFlowPanel(); sessHCard.addStyleName("pony-hstat"); sessHCard.add(sessHVal, sessHLbl);
        hstats.add(latHCard, heapHCard, sessHCard);

        final PElement badge = Element.newDiv();
        badge.addStyleName("pony-badge");
        badge.setInnerText("● LIVE");

        header.add(logoWrap, divider, hstats, badge);
        PWindow.getMain().add(header);

        // Refresh header stats every second
        final Runtime rt0 = Runtime.getRuntime();
        PScheduler.scheduleAtFixedRate(() -> {
            latHVal.setInnerText(String.format("%.0f", uiContext.getRoundtripLatency()));
            heapHVal.setInnerText(String.valueOf((rt0.totalMemory() - rt0.freeMemory()) / 1024 / 1024));
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

        // Build all tabs eagerly (some need to be attached to window)
        final String[][] tabs = {
            {"⌨", "Inputs"}, {"⊞", "Layouts"}, {"◈", "Data"},
            {"⊟", "DataGrid"}, {"◉", "Web Comp."}, {"⋯", "Misc"},
            {"⚡", "Perf"}, {"</>", "Code"}, {"≡", "Binary"}
        };

        // Active tab schedulers — cancelled on tab leave, list cleared and refilled on re-entry
        @SuppressWarnings("unchecked")
        final List<PScheduler.UIRunnable>[] tabSchedulers = new List[tabs.length];
        for (int i = 0; i < tabs.length; i++) tabSchedulers[i] = new ArrayList<>();

        // Only build the first tab eagerly — others are built lazily on first activation
        // This avoids starting schedulers for tabs that are never visited
        final PFlowPanel[] tabPanels = new PFlowPanel[tabs.length];
        tabPanels[0] = buildTabWithSchedulers(0, uiContext, tabSchedulers[0]);

        final int tabCount = tabs.length;
        final PButton[] tabButtons = new PButton[tabCount];
        final int[] activeTab = { 0 };

        // Group labels
        final PElement grp1 = Element.newDiv(); grp1.addStyleName("pony-nav-group"); grp1.setInnerText("Widgets");
        nav.add(grp1);
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            final PButton tab = Element.newPButton(tabs[i][0] + "  " + tabs[i][1]);
            tab.addStyleName("pony-tab");
            if (i == 0) tab.addStyleName("pony-tab-active");
            tabButtons[i] = tab;
            tab.addClickHandler(e -> {
                if (activeTab[0] == idx) return;
                // Cancel schedulers of the tab we're leaving
                tabSchedulers[activeTab[0]].forEach(PScheduler.UIRunnable::cancel);
                tabSchedulers[activeTab[0]].clear();
                // Rebuild the new tab fresh (restarts its schedulers)
                tabSchedulers[idx].clear();
                tabPanels[idx] = buildTabWithSchedulers(idx, uiContext, tabSchedulers[idx]);
                content.setWidget(tabPanels[idx]);
                tabButtons[activeTab[0]].removeStyleName("pony-tab-active");
                tabButtons[idx].addStyleName("pony-tab-active");
                activeTab[0] = idx;
            });
            nav.add(tab);
        }
        final PElement grp2 = Element.newDiv(); grp2.addStyleName("pony-nav-group"); grp2.setInnerText("Developer");
        nav.add(grp2);
        for (int i = 6; i < tabCount; i++) {
            final int idx = i;
            final PButton tab = Element.newPButton(tabs[i][0] + "  " + tabs[i][1]);
            tab.addStyleName("pony-tab");
            tabButtons[i] = tab;
            tab.addClickHandler(e -> {
                if (activeTab[0] == idx) return;
                tabSchedulers[activeTab[0]].forEach(PScheduler.UIRunnable::cancel);
                tabSchedulers[activeTab[0]].clear();
                tabSchedulers[idx].clear();
                tabPanels[idx] = buildTabWithSchedulers(idx, uiContext, tabSchedulers[idx]);
                content.setWidget(tabPanels[idx]);
                tabButtons[activeTab[0]].removeStyleName("pony-tab-active");
                tabButtons[idx].addStyleName("pony-tab-active");
                activeTab[0] = idx;
            });
            nav.add(tab);
        }

        // Nav footer with version
        final PFlowPanel navFooter = Element.newPFlowPanel();
        navFooter.addStyleName("pony-nav-footer");
        final PElement version = Element.newDiv();
        version.addStyleName("pony-version");
        version.setInnerText("PonySDK v2.8 · Java " + System.getProperty("java.version").split("\\.")[0]);
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
            case 0 -> buildInputsTab(schedulers);
            case 1 -> buildLayoutsTab(schedulers);
            case 2 -> buildDataTab(schedulers);
            case 3 -> buildDataGridTab(schedulers);
            case 4 -> buildWebCompTab(schedulers);
            case 5 -> buildMiscTab(schedulers);
            case 6 -> buildPerfTab(uiContext, schedulers);
            case 7 -> buildCodeTab();
            default -> buildBinaryAddonTab(schedulers);
        };
    }

    private PFlowPanel buildBinaryAddonTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PElement box = Element.newDiv();
        box.setAttribute("style", "padding:28px 32px;max-width:1040px");

        final PElement title = Element.newDiv();
        title.setAttribute("style", "font-size:20px;font-weight:800;margin-bottom:6px;color:var(--text,#f0f4ff)");
        title.setInnerText("Binary PAddOn protocol — live signals console");

        final PElement sub = Element.newDiv();
        sub.setAttribute("style", "color:var(--text2,#8892a4);margin-bottom:18px;line-height:1.6;max-width:840px");
        sub.setInnerText("Server-driven end to end: the controls below are PonySDK widgets whose handlers run in Java; "
            + "the server computes the signals and streams them as typed double arrays in pure binary, and the browser "
            + "only renders. Push series and resolution up to stream tens of thousands of typed values per frame "
            + "(uint31 length, no 255 cap, no JSON).");

        // The chart is configured entirely through pure-binary typed creation args:
        // color (String), maxPoints (int), lineWidth (double), fill (boolean), seed (long)
        final BinaryArgsAddOn addon = new BinaryArgsAddOn("#43e8b0", 1024, 2.0, false, 1_234_567_890_123L);
        addon.asWidget().setAttribute("style", "display:block;margin-top:4px");

        // Stream state owned by the server
        final boolean[] playing = { true };
        final int[] nSeries = { 2 };
        final int[] mPoints = { 1024 };
        final double[] phase = { 0 };

        // Controls — PonySDK widgets; every click handler runs server-side
        final PFlowPanel controls = Element.newPFlowPanel();
        controls.setAttribute("style", "display:flex;gap:28px;flex-wrap:wrap;align-items:flex-end;margin-bottom:18px;"
            + "background:rgba(255,255,255,.018);border:1px solid rgba(255,255,255,.06);border-radius:14px;padding:16px 20px");

        final PButton toggle = Element.newPButton("⏸  Pause");
        styleBinaryCtrl(toggle, true);
        toggle.addClickHandler(e -> {
            playing[0] = !playing[0];
            toggle.setText(playing[0] ? "⏸  Pause" : "▶  Resume");
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
            if (!playing[0]) return;
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
        caption.setAttribute("style",
            "font-size:10px;color:#5a6b85;text-transform:uppercase;letter-spacing:.8px;margin-bottom:6px");
        caption.setInnerText(label);
        group.add(caption);
        group.add(content);
        return group;
    }

    private static void styleBinaryCtrl(final PButton button, final boolean active) {
        // Monochrome button theme: mark the selected control with the accent via outline (box-shadow is
        // forced to none by the theme) + full opacity; dim the rest. Colour = meaning (selection), not decoration.
        button.setAttribute("style", "margin-right:8px;font-variant-numeric:tabular-nums;transition:opacity .15s;"
            + (active
                ? "opacity:1;outline:1px solid var(--accent2,#43e8b0);outline-offset:-1px;"
                : "opacity:.45;"));
    }

    private static String buildGlobalStyles() {
        return "var s = document.createElement('style'); s.textContent = `" +
            // Variables
            ":root{" +
            "--accent:#7c6fff;--accent2:#43e8b0;--accent3:#f59e0b;--danger:#f87171;" +
            "--bg:#080b14;--surface:#0e1220;--surface2:#131929;--surface3:#1a2235;--surface4:#1f2a40;" +
            "--text:#f0f4ff;--text2:#8892a4;--text3:#4a5568;" +
            "--border:#1e2d45;--border2:#253550;" +
            "--radius:10px;--radius2:16px;" +
            "--shadow:0 4px 24px rgba(0,0,0,.5);--shadow2:0 8px 40px rgba(0,0,0,.6);" +
            "--glow:0 0 20px rgba(124,111,255,.3);--glow2:0 0 30px rgba(67,232,176,.2);" +
            "}" +
            "*{box-sizing:border-box;margin:0;padding:0;}" +
            "body{font-family:-apple-system,BlinkMacSystemFont,'Inter','Segoe UI',sans-serif;background:var(--bg);color:var(--text);min-height:100vh;overflow-x:hidden;}" +

            // Scrollbar
            "::-webkit-scrollbar{width:5px;height:5px;}" +
            "::-webkit-scrollbar-track{background:var(--surface);}" +
            "::-webkit-scrollbar-thumb{background:var(--border2);border-radius:3px;}" +
            "::-webkit-scrollbar-thumb:hover{background:var(--accent);}" +

            // Header
            ".pony-header{" +
            "background:linear-gradient(135deg,rgba(14,18,32,.98) 0%,rgba(19,25,41,.98) 100%);" +
            "padding:0 32px;height:64px;display:flex;align-items:center;gap:24px;" +
            "border-bottom:1px solid var(--border);position:sticky;top:0;z-index:200;" +
            "backdrop-filter:blur(20px);-webkit-backdrop-filter:blur(20px);" +
            "box-shadow:0 1px 0 var(--border),0 4px 32px rgba(0,0,0,.4);}" +
            ".pony-logo-wrap{display:flex;align-items:center;gap:12px;}" +
            ".pony-logo-icon{font-size:28px;filter:drop-shadow(0 0 8px rgba(124,111,255,.6));}" +
            ".pony-logo-text{display:flex;flex-direction:column;}" +
            ".pony-logo{font-size:18px;font-weight:800;letter-spacing:-.5px;" +
            "background:linear-gradient(90deg,#a78bfa,var(--accent2));-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;}" +
            ".pony-subtitle{font-size:10px;color:var(--text3);letter-spacing:1px;text-transform:uppercase;margin-top:1px;}" +
            ".pony-divider{width:1px;height:32px;background:var(--border);}" +
            ".pony-header-stats{display:flex;gap:24px;}" +
            ".pony-hstat{display:flex;flex-direction:column;align-items:center;}" +
            ".pony-hstat-val{font-size:15px;font-weight:700;color:var(--accent2);line-height:1;}" +
            ".pony-hstat-lbl{font-size:9px;color:var(--text3);text-transform:uppercase;letter-spacing:.8px;margin-top:2px;}" +
            ".pony-badge{background:linear-gradient(135deg,var(--accent),var(--accent2));color:#fff;" +
            "font-size:9px;font-weight:800;padding:3px 10px;border-radius:20px;letter-spacing:1px;margin-left:auto;" +
            "box-shadow:var(--glow);animation:pulse 2s infinite;}" +
            "@keyframes pulse{0%,100%{box-shadow:0 0 8px rgba(124,111,255,.4);}50%{box-shadow:0 0 20px rgba(124,111,255,.8);}}" +

            // Shell
            ".pony-shell{display:flex;min-height:calc(100vh - 64px);}" +

            // Sidebar
            ".pony-nav{width:220px;min-width:220px;background:var(--surface);" +
            "border-right:1px solid var(--border);padding:20px 12px;" +
            "display:flex;flex-direction:column;gap:2px;" +
            "position:sticky;top:64px;height:calc(100vh - 64px);overflow-y:auto;}" +
            ".pony-nav-group{font-size:9px;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;" +
            "color:var(--text3);padding:16px 12px 6px;}" +
            ".pony-tab{width:100%;text-align:left;padding:9px 12px;cursor:pointer;border-radius:8px;" +
            "font-size:13px;font-weight:500;color:var(--text2);border:none;background:transparent;" +
            "transition:all .15s ease;user-select:none;outline:none;display:flex;align-items:center;gap:10px;}" +
            ".pony-tab:hover{background:rgba(124,111,255,.1);color:var(--text);transform:translateX(2px);}" +
            ".pony-tab-active{background:linear-gradient(135deg,rgba(124,111,255,.2),rgba(67,232,176,.08));" +
            "color:var(--text);font-weight:600;box-shadow:inset 0 0 0 1px rgba(124,111,255,.3);" +
            "text-shadow:0 0 20px rgba(124,111,255,.5);}" +
            ".pony-tab-icon{font-size:15px;width:22px;text-align:center;opacity:.8;}" +
            ".pony-nav-footer{margin-top:auto;padding:16px 12px;border-top:1px solid var(--border);}" +
            ".pony-version{font-size:10px;color:var(--text3);text-align:center;}" +

            // Content
            ".pony-content{flex:1;padding:32px 40px;overflow-y:auto;max-width:1200px;}" +

            // Page title
            ".pony-page-title{font-size:22px;font-weight:700;color:var(--text);margin-bottom:4px;}" +
            ".pony-page-desc{font-size:13px;color:var(--text2);margin-bottom:28px;line-height:1.5;}" +

            // Section title
            ".pony-section-title{font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;" +
            "color:var(--accent);margin:28px 0 10px;padding-bottom:8px;" +
            "border-bottom:1px solid var(--border);display:flex;align-items:center;gap:8px;}" +
            ".pony-section-title::before{content:'';display:inline-block;width:3px;height:11px;" +
            "background:linear-gradient(var(--accent),var(--accent2));border-radius:2px;flex-shrink:0;}" +

            // Widget rows
            ".pony-row{display:flex;align-items:center;gap:16px;padding:11px 16px;margin:3px 0;" +
            "background:var(--surface2);border-radius:var(--radius);border:1px solid var(--border);" +
            "transition:all .15s;}" +
            ".pony-row:hover{border-color:rgba(124,111,255,.35);background:var(--surface3);}" +
            ".pony-label{min-width:230px;font-size:12px;color:var(--text2);font-weight:500;letter-spacing:.2px;}" +

            // GWT overrides
            ".gwt-Button{background:linear-gradient(135deg,var(--accent),#9b8fff)!important;color:#fff!important;border:none!important;" +
            "padding:7px 18px!important;border-radius:7px!important;font-size:12px!important;font-weight:600!important;" +
            "cursor:pointer!important;transition:all .15s!important;letter-spacing:.3px!important;}" +
            ".gwt-Button:hover{opacity:.88!important;transform:translateY(-1px)!important;box-shadow:0 4px 12px rgba(124,111,255,.4)!important;}" +
            ".gwt-TextBox,.gwt-TextArea,.gwt-PasswordTextBox{background:var(--surface3)!important;color:var(--text)!important;" +
            "border:1px solid var(--border2)!important;border-radius:7px!important;padding:7px 11px!important;" +
            "font-size:13px!important;outline:none!important;transition:border-color .15s!important;}" +
            ".gwt-TextBox:focus,.gwt-TextArea:focus,.gwt-PasswordTextBox:focus{border-color:var(--accent)!important;box-shadow:0 0 0 3px rgba(124,111,255,.15)!important;}" +
            ".gwt-CheckBox label,.gwt-RadioButton label{color:var(--text)!important;font-size:13px!important;}" +
            ".gwt-ListBox{background:var(--surface3)!important;color:var(--text)!important;border:1px solid var(--border2)!important;border-radius:7px!important;padding:5px 9px!important;}" +
            ".gwt-MenuBar{background:var(--surface3)!important;border:1px solid var(--border)!important;border-radius:7px!important;}" +
            ".gwt-MenuBar .gwt-MenuItem{color:var(--text)!important;padding:6px 14px!important;font-size:13px!important;}" +
            ".gwt-MenuBar .gwt-MenuItem-selected{background:var(--accent)!important;border-radius:5px!important;}" +

            // Perf tab
            ".perf-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:14px;margin-bottom:24px;}" +
            ".perf-card{background:var(--surface2);border:1px solid var(--border);border-radius:var(--radius2);" +
            "padding:22px 18px;text-align:center;transition:all .2s;position:relative;overflow:hidden;}" +
            ".perf-card::before{content:'';position:absolute;top:0;left:0;right:0;height:2px;" +
            "background:linear-gradient(90deg,var(--accent),var(--accent2));opacity:0;transition:opacity .2s;}" +
            ".perf-card:hover{border-color:rgba(124,111,255,.4);transform:translateY(-2px);box-shadow:var(--glow);}" +
            ".perf-card:hover::before{opacity:1;}" +
            ".perf-icon{font-size:22px;margin-bottom:10px;opacity:.8;}" +
            ".perf-value{font-size:30px;font-weight:800;line-height:1;" +
            "background:linear-gradient(135deg,#a78bfa,var(--accent2));-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;}" +
            ".perf-unit{font-size:10px;color:var(--text3);margin-top:3px;letter-spacing:.5px;}" +
            ".perf-label{font-size:11px;color:var(--text2);margin-top:10px;font-weight:600;letter-spacing:.3px;}" +
            ".perf-bar-wrap{background:var(--surface3);border-radius:4px;height:4px;margin-top:12px;overflow:hidden;}" +
            ".perf-bar{height:4px;border-radius:4px;background:linear-gradient(90deg,var(--accent),var(--accent2));transition:width .8s cubic-bezier(.4,0,.2,1);}" +
            ".perf-trend{font-size:10px;margin-top:6px;}" +
            ".perf-up{color:var(--accent2);}" +
            ".perf-down{color:var(--danger);}" +

            // Sparkline canvas
            ".perf-spark{width:100%;height:40px;margin-top:8px;opacity:.7;}" +

            // Code tab
            ".code-snippet{background:#0a0e1a;border:1px solid var(--border);border-radius:var(--radius2);margin:8px 0 20px;overflow:hidden;box-shadow:var(--shadow);}" +
            ".code-header{background:var(--surface2);padding:10px 16px;font-size:11px;color:var(--text2);" +
            "display:flex;align-items:center;gap:8px;border-bottom:1px solid var(--border);}" +
            ".code-dot{width:11px;height:11px;border-radius:50%;flex-shrink:0;}" +
            ".code-title{flex:1;text-align:center;font-weight:600;letter-spacing:.3px;}" +
            ".code-lang{background:rgba(124,111,255,.2);color:var(--accent);font-size:9px;font-weight:700;" +
            "padding:2px 8px;border-radius:4px;letter-spacing:.5px;}" +
            ".code-body{padding:20px 24px;font-family:'JetBrains Mono','Fira Code','Cascadia Code',monospace;" +
            "font-size:12.5px;line-height:1.75;color:#cdd9e5;white-space:pre;overflow-x:auto;counter-reset:line;}" +
            ".code-line{display:block;counter-increment:line;}" +
            ".code-line::before{content:counter(line);display:inline-block;width:28px;color:var(--text3);" +
            "font-size:10px;text-align:right;margin-right:20px;user-select:none;}" +
            ".code-kw{color:#f47067;}" +
            ".code-type{color:#6cb6ff;}" +
            ".code-str{color:#96d0ff;}" +
            ".code-comment{color:#636e7b;font-style:italic;}" +
            ".code-method{color:#dcbdfb;}" +
            ".code-num{color:#f69d50;}" +
            ".code-ann{color:#6cb6ff;opacity:.7;}" +

            // Highlight box
            ".highlight-box{background:linear-gradient(135deg,rgba(124,111,255,.08),rgba(67,232,176,.05));" +
            "border:1px solid rgba(124,111,255,.2);border-radius:var(--radius2);padding:20px 24px;margin:16px 0;" +
            "display:flex;align-items:flex-start;gap:16px;}" +
            ".highlight-icon{font-size:24px;flex-shrink:0;margin-top:2px;}" +
            ".highlight-title{font-size:13px;font-weight:700;color:var(--text);margin-bottom:4px;}" +
            ".highlight-desc{font-size:12px;color:var(--text2);line-height:1.6;}" +
            "` ; document.head.appendChild(s);";
    }

    /** Appended on top of {@link #buildGlobalStyles()} — a restrained, modern (Geist/Linear) layer. */
    private static String buildModernTheme() {
        return "var s=document.createElement('style');s.textContent=`"
            // Blueprint grid background (Vercel/Geist): subtle, technical, no decoration
            + "body{background-color:#08090c !important;"
            +   "background-image:linear-gradient(rgba(255,255,255,.022) 1px,transparent 1px),"
            +   "linear-gradient(90deg,rgba(255,255,255,.022) 1px,transparent 1px) !important;"
            +   "background-size:32px 32px !important;background-position:-1px -1px !important;background-attachment:fixed !important;}"
            // Header: hairline divider, no glow
            + ".pony-header{box-shadow:none !important;border-bottom:1px solid rgba(255,255,255,.07) !important;}"
            // Titles: clean near-white, tight tracking (no gradient text)
            + ".pony-page-title{color:#fafafa !important;-webkit-text-fill-color:#fafafa !important;"
            +   "letter-spacing:-.02em !important;font-weight:700 !important;}"
            + ".pony-page-desc{color:#8b8b94 !important;}"
            // Content rows: flat, neutral, hairline border — readable in grayscale
            + ".pony-row{background:rgba(255,255,255,.018) !important;border:1px solid rgba(255,255,255,.06) !important;"
            +   "backdrop-filter:none !important;-webkit-backdrop-filter:none !important;"
            +   "transition:background .15s,border-color .15s !important;}"
            + ".pony-row:hover{background:rgba(255,255,255,.032) !important;border-color:rgba(255,255,255,.13) !important;"
            +   "transform:none !important;box-shadow:none !important;}"
            // Buttons: clean monochrome (colour is reserved for meaning, not every button)
            + ".gwt-Button{background:#16171b !important;border:1px solid rgba(255,255,255,.12) !important;"
            +   "color:#fafafa !important;box-shadow:none !important;font-weight:500 !important;letter-spacing:0 !important;}"
            + ".gwt-Button:hover{background:#1c1d22 !important;border-color:rgba(255,255,255,.22) !important;"
            +   "opacity:1 !important;transform:none !important;}"
            + ".gwt-Button:active{background:#0f1013 !important;transform:none !important;}"
            // Sidebar (after .gwt-Button so nav items win): flat, a single accent on the active item
            + ".pony-tab{background:transparent !important;color:#8b8b94 !important;box-shadow:none !important;"
            +   "border:1px solid transparent !important;font-weight:500 !important;transition:background .14s,color .14s !important;}"
            + ".pony-tab:hover{background:rgba(255,255,255,.05) !important;color:#fafafa !important;transform:none !important;}"
            + ".pony-tab-active{background:rgba(255,255,255,.07) !important;color:#fafafa !important;"
            +   "border-color:transparent !important;box-shadow:inset 2px 0 0 var(--accent2,#43e8b0) !important;}"
            + "`;document.head.appendChild(s);";
    }

    // ── Tab: Inputs ──────────────────────────────────────────────────────────

    private PFlowPanel buildInputsTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Input Widgets", "All form controls rendered server-side, zero client-side state.");

        p.add(sectionTitle("Text inputs"));
        p.add(labeled("TextBox (Enter = alert)", createPTextBox()));
        p.add(labeled("TextArea", Element.newPTextArea()));
        p.add(labeled("Password", Element.newPPasswordTextBox("Password")));
        p.add(labeled("RichText editor", buildRichText()));

        p.add(sectionTitle("Buttons & Checks"));
        p.add(labeled("Button", createButton()));
        p.add(labeled("CheckBox", Element.newPCheckBox("Checkbox")));
        p.add(labeled("RadioButton (group)", buildRadioGroup()));
        p.add(labeled("PushButton", Element.newPPushButton(Element.newPImage())));

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

        p.add(sectionTitle("Panels"));
        p.add(labeled("FlowPanel (nested)", createPFlowPanel()));
        p.add(labeled("HorizontalPanel", Element.newPHorizontalPanel()));
        p.add(labeled("VerticalPanel", Element.newPVerticalPanel()));
        p.add(labeled("AbsolutePanel", createBlock(createAbsolutePanel())));
        p.add(labeled("ScrollPanel", Element.newPScrollPanel()));
        p.add(labeled("SimplePanel", Element.newPSimplePanel()));
        p.add(labeled("FocusPanel", Element.newPFocusPanel()));
        p.add(labeled("HeaderPanel", Element.newPHeaderPanel()));
        p.add(labeled("DecoratorPanel", Element.newPDecoratorPanel()));

        p.add(sectionTitle("Layout panels"));
        p.add(labeled("DockLayoutPanel", createDockLayoutPanel()));
        p.add(labeled("LayoutPanel", Element.newPLayoutPanel()));
        p.add(labeled("SimpleLayoutPanel", Element.newPSimpleLayoutPanel()));
        p.add(labeled("SplitLayoutPanel", Element.newPSplitLayoutPanel()));
        p.add(labeled("StackLayoutPanel", createStackLayoutPanel()));
        p.add(labeled("TabLayoutPanel", createTabLayoutPanel()));
        p.add(labeled("TabPanel", Element.newPTabPanel()));

        p.add(sectionTitle("Disclosure / Dialog"));
        p.add(labeled("DisclosurePanel", Element.newPDisclosurePanel("Click to expand")));
        p.add(labeled("DialogBox", Element.newPDialogBox()));
        p.add(labeled("DecoratedPopupPanel", Element.newPDecoratedPopupPanel(false)));

        p.add(sectionTitle("Visibility handler"));
        testVisibilityHandler(p, schedulers);

        return p;
    }

    // ── Tab: Data ────────────────────────────────────────────────────────────

    private PFlowPanel buildDataTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();

        p.add(sectionTitle("Tree"));
        p.add(createTree());

        p.add(sectionTitle("Grid (legacy RefreshableDataGrid)"));
        p.add(createGrid(schedulers));

        p.add(sectionTitle("FlexTable"));
        p.add(Element.newPFlexTable());

        p.add(sectionTitle("Grid 2x3"));
        p.add(Element.newPGrid(2, 3));

        return p;
    }

    // ── Tab: DataGrid ────────────────────────────────────────────────────────

    private PFlowPanel buildDataGridTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        p.add(sectionTitle("DataGrid2 — 10 000 rows, 52 columns"));
        buildDataGridInto(p);
        return p;
    }

    // ── Tab: WebComponents ───────────────────────────────────────────────────

    private PFlowPanel buildWebCompTab(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        p.add(sectionTitle("Web Components"));
        buildWebComponentsInto(p);
        return p;
    }

    // ── Tab: Perf ─────────────────────────────────────────────────────────────

    private PFlowPanel buildPerfTab(final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Performance Monitor", "Live JVM & network metrics — updated every second.");
        final Runtime rt = Runtime.getRuntime();
        final long maxMem = rt.maxMemory();
        final long startTime = System.currentTimeMillis();

        p.add(sectionTitle("JVM Memory"));
        final PFlowPanel heapGrid = Element.newPFlowPanel();
        heapGrid.addStyleName("perf-grid");

        final PElement heapIcon = el("perf-icon", "🧠");
        final PElement heapVal = el("perf-value", "—");
        final PElement heapUnit = el("perf-unit", "MB USED");
        final PElement heapLbl = el("perf-label", "Heap Used");
        final PElement heapBarWrap = Element.newDiv(); heapBarWrap.addStyleName("perf-bar-wrap");
        final PElement heapBar = Element.newDiv(); heapBar.addStyleName("perf-bar"); heapBar.setAttribute("style", "width:0%");
        heapBarWrap.add(heapBar);
        final PElement heapTrend = el("perf-trend perf-up", "");
        final PFlowPanel heapCard = perfCard(heapIcon, heapVal, heapUnit, heapLbl, heapBarWrap, heapTrend);

        final PElement maxIcon = el("perf-icon", "📦");
        final PElement maxVal = el("perf-value", String.valueOf(maxMem / 1024 / 1024));
        final PElement maxUnit = el("perf-unit", "MB MAX");
        final PElement maxLbl = el("perf-label", "Heap Max");
        final PFlowPanel maxCard = perfCard(maxIcon, maxVal, maxUnit, maxLbl);

        final PElement freeIcon = el("perf-icon", "💚");
        final PElement freeVal = el("perf-value", "—");
        final PElement freeUnit = el("perf-unit", "MB FREE");
        final PElement freeLbl = el("perf-label", "Heap Free");
        final PFlowPanel freeCard = perfCard(freeIcon, freeVal, freeUnit, freeLbl);

        final PElement cpuIcon = el("perf-icon", "⚙️");
        final PElement cpuVal = el("perf-value", String.valueOf(rt.availableProcessors()));
        final PElement cpuUnit = el("perf-unit", "CORES");
        final PElement cpuLbl = el("perf-label", "CPU");
        final PFlowPanel cpuCard = perfCard(cpuIcon, cpuVal, cpuUnit, cpuLbl);

        final PElement uptimeIcon = el("perf-icon", "⏱");
        final PElement uptimeVal = el("perf-value", "0");
        final PElement uptimeUnit = el("perf-unit", "SECONDS");
        final PElement uptimeLbl = el("perf-label", "Uptime");
        final PFlowPanel uptimeCard = perfCard(uptimeIcon, uptimeVal, uptimeUnit, uptimeLbl);

        heapGrid.add(heapCard, maxCard, freeCard, cpuCard, uptimeCard);
        p.add(heapGrid);

        p.add(sectionTitle("Network & Sessions"));
        final PFlowPanel netGrid = Element.newPFlowPanel();
        netGrid.addStyleName("perf-grid");

        final PElement sessIcon = el("perf-icon", "👥");
        final PElement sessVal = el("perf-value", "1");
        final PElement sessUnit = el("perf-unit", "ACTIVE");
        final PElement sessLbl = el("perf-label", "Sessions");
        final PFlowPanel sessCard = perfCard(sessIcon, sessVal, sessUnit, sessLbl);

        final PElement latIcon = el("perf-icon", "📡");
        final PElement latVal = el("perf-value", "—");
        final PElement latUnit = el("perf-unit", "MS ROUNDTRIP");
        final PElement latLbl = el("perf-label", "Latency");
        final PElement latBar2Wrap = Element.newDiv(); latBar2Wrap.addStyleName("perf-bar-wrap");
        final PElement latBar2 = Element.newDiv(); latBar2.addStyleName("perf-bar"); latBar2.setAttribute("style", "width:0%");
        latBar2Wrap.add(latBar2);
        final PFlowPanel latCard = perfCard(latIcon, latVal, latUnit, latLbl, latBar2Wrap);

        final PElement sentIcon = el("perf-icon", "📤");
        final PElement sentVal = el("perf-value", "—");
        final PElement sentUnit = el("perf-unit", "KB/s · deflate");
        final PElement sentLbl = el("perf-label", "Sent (compressed)");
        final PFlowPanel sentCard = perfCard(sentIcon, sentVal, sentUnit, sentLbl);

        final PElement rcvIcon = el("perf-icon", "📥");
        final PElement rcvVal = el("perf-value", "—");
        final PElement rcvUnit = el("perf-unit", "KB/s");
        final PElement rcvLbl = el("perf-label", "Received");
        final PFlowPanel rcvCard = perfCard(rcvIcon, rcvVal, rcvUnit, rcvLbl);

        final PElement netIcon = el("perf-icon", "🔌");
        final PElement netVal = el("perf-value", "WS");
        final PElement netUnit = el("perf-unit", "PROTOCOL");
        final PElement netLbl = el("perf-label", "Transport");
        final PFlowPanel netCard = perfCard(netIcon, netVal, netUnit, netLbl);

        netGrid.add(sessCard, latCard, sentCard, rcvCard, netCard);
        p.add(netGrid);

        // Dictionary section
        p.add(sectionTitle("String Dictionary"));
        final PFlowPanel dictGrid = Element.newPFlowPanel();
        dictGrid.addStyleName("perf-grid");

        final PElement dictSizeIcon = el("perf-icon", "📖");
        final PElement dictSizeVal = el("perf-value", "—");
        final PElement dictSizeUnit = el("perf-unit", "ENTRIES");
        final PElement dictSizeLbl = el("perf-label", "Dict Size");
        final PElement dictBarWrap = Element.newDiv(); dictBarWrap.addStyleName("perf-bar-wrap");
        final PElement dictBar = Element.newDiv(); dictBar.addStyleName("perf-bar"); dictBar.setAttribute("style", "width:0%");
        dictBarWrap.add(dictBar);
        final PFlowPanel dictSizeCard = perfCard(dictSizeIcon, dictSizeVal, dictSizeUnit, dictSizeLbl, dictBarWrap);

        final PElement dictMaxIcon = el("perf-icon", "📦");
        final PElement dictMaxVal = el("perf-value", "4096");
        final PElement dictMaxUnit = el("perf-unit", "MAX ENTRIES");
        final PElement dictMaxLbl = el("perf-label", "Dict Capacity");
        final PFlowPanel dictMaxCard = perfCard(dictMaxIcon, dictMaxVal, dictMaxUnit, dictMaxLbl);

        final PElement dictHitIcon = el("perf-icon", "🎯");
        final PElement dictHitVal = el("perf-value", "—");
        final PElement dictHitUnit = el("perf-unit", "% HIT RATE");
        final PElement dictHitLbl = el("perf-label", "Dict Hit Rate");
        final PFlowPanel dictHitCard = perfCard(dictHitIcon, dictHitVal, dictHitUnit, dictHitLbl);

        dictGrid.add(dictSizeCard, dictMaxCard, dictHitCard);
        p.add(dictGrid);

        // Highlight box
        p.add(highlightBox("🚀", "Zero polling overhead",
            "PonySDK uses a persistent WebSocket connection. No HTTP polling, no REST calls. " +
            "Each UI update is a binary delta — typically 5–20 bytes. " +
            "A 10 000-row DataGrid update costs less than a single REST request."));

        // Live refresh every second
        final long[] prevHeap = {0};
        final long[] prevSent = {0};
        final long[] prevRcv = {0};
        final long[] totalDictLookups = {0};
        final long[] totalDictHits = {0};
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            final long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            final long free = rt.freeMemory() / 1024 / 1024;
            final long max = maxMem / 1024 / 1024;
            final int pct = (int) (used * 100 / Math.max(max, 1));
            heapVal.setInnerText(String.valueOf(used));
            freeVal.setInnerText(String.valueOf(free));
            heapBar.setAttribute("style", "width:" + pct + "%;height:4px;border-radius:4px;background:linear-gradient(90deg,#7c6fff,#43e8b0);transition:width .8s cubic-bezier(.4,0,.2,1);");
            heapTrend.setInnerText(used > prevHeap[0] ? "▲ " + (used - prevHeap[0]) + " MB" : used < prevHeap[0] ? "▼ " + (prevHeap[0] - used) + " MB" : "● stable");
            heapTrend.setAttribute("class", used > prevHeap[0] ? "perf-trend perf-down" : "perf-trend perf-up");
            prevHeap[0] = used;

            final long upSec = (System.currentTimeMillis() - startTime) / 1000;
            uptimeVal.setInnerText(upSec < 60 ? String.valueOf(upSec) : (upSec / 60) + "m " + (upSec % 60) + "s");
            uptimeUnit.setInnerText(upSec < 60 ? "SECONDS" : "UPTIME");

            final double lat = uiContext.getRoundtripLatency();
            latVal.setInnerText(String.format("%.0f", lat));
            final int latPct = (int) Math.min(lat / 2, 100);
            latBar2.setAttribute("style", "width:" + latPct + "%;height:4px;border-radius:4px;background:linear-gradient(90deg," + (latPct < 30 ? "#43e8b0,#7c6fff" : latPct < 70 ? "#f59e0b,#f87171" : "#f87171,#dc2626") + ");transition:width .8s;");

            final PonySDKMetrics metrics = uiContext.getMetrics();
            if (metrics != null) {
                sessVal.setInnerText(String.valueOf(metrics.getActiveContexts()));
                final long curSent = metrics.getTotalBytesSent();
                final long curRcv = metrics.getTotalBytesReceived();
                final long deltaSentKb = (curSent - prevSent[0]) / 1024;
                final long deltaRcvKb = (curRcv - prevRcv[0]) / 1024;
                sentVal.setInnerText(String.valueOf(deltaSentKb));
                rcvVal.setInnerText(String.valueOf(deltaRcvKb));
                prevSent[0] = curSent;
                prevRcv[0] = curRcv;
            }

            // Dictionary stats
            final com.ponysdk.core.server.application.StringDictionary dict = uiContext.getStringDictionary();
            if (dict != null) {
                final int dictSize = dict.size();
                final int dictMax = dict.getMaxSize();
                dictSizeVal.setInnerText(String.valueOf(dictSize));
                dictMaxVal.setInnerText(String.valueOf(dictMax));
                final int dictPct = (int) (dictSize * 100L / Math.max(dictMax, 1));
                dictBar.setAttribute("style", "width:" + dictPct + "%;height:4px;border-radius:4px;background:linear-gradient(90deg,#7c6fff,#43e8b0);transition:width .8s;");
                // Approximate hit rate from session frequency map
                final java.util.Map<String, Long> freq = dict.getSessionFrequency();
                long hits = 0, lookups = 0;
                for (final long v : freq.values()) { lookups += v; if (v > 1) hits += v - 1; }
                totalDictLookups[0] = lookups;
                totalDictHits[0] = hits;
                final int hitPct = lookups > 0 ? (int) (hits * 100 / lookups) : 0;
                dictHitVal.setInnerText(String.valueOf(hitPct));
            }
        }, Duration.ofSeconds(1)));

        return p;
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
        final PElement ico = Element.newDiv(); ico.addStyleName("highlight-icon"); ico.setInnerText(icon);
        final PFlowPanel txt = Element.newPFlowPanel();
        final PElement ttl = Element.newDiv(); ttl.addStyleName("highlight-title"); ttl.setInnerText(title);
        final PElement dsc = Element.newDiv(); dsc.addStyleName("highlight-desc"); dsc.setInnerText(desc);
        txt.add(ttl, dsc);
        box.add(ico, txt);
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
            ln("    btn." + mt("addClickHandler") + "(e -> btn." + mt("setText") + "(" + st("Clicked! 🎉") + "));") +
            ln("") +
            ln("    " + cm("Add to main window — delta sent over WebSocket")) +
            ln("    " + ty("PWindow") + "." + mt("getMain") + "()." + mt("add") + "(btn);") +
            ln("    " + cm("No JS. No REST. No serialization. Just Java.")) +
            ln("  }") +
            ln("}");
    }

    private static String snippet_liveCounter() {
        return ln(cm("Server pushes updates every 100ms — ~5 bytes per frame")) +
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

    // ── Tab: Misc ────────────────────────────────────────────────────────────

    private PFlowPanel buildMiscTab(final List<PScheduler.UIRunnable> schedulers) {        final PFlowPanel p = section();

        p.add(sectionTitle("HTML / Anchors"));
        p.add(labeled("HTML", Element.newPHTML("Hello <b>HTML</b>")));
        p.add(labeled("Anchor", Element.newPAnchor("PonySDK site", "https://github.com/smartTrade-OpenSource/PonySDK")));

        p.add(sectionTitle("Script"));
        final PButton alertBtn = Element.newPButton("Run JS alert");
        alertBtn.addClickHandler(e -> PScript.execute(PWindow.getMain(), "alert('Hello from PScript!')"));
        p.add(labeled("PScript", alertBtn));

        p.add(sectionTitle("Toolbar"));
        p.add(new PToolbar());

        p.add(sectionTitle("Frame (self)"));
        final PFrame frame = Element.newPFrame("http://localhost:8081/sample/");
        frame.setHeight("200px");
        frame.setWidth("400px");
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
        return row;
    }

    private static PFlowPanel labeled(final String label, final IsPWidget widget) {
        return labeled(label, widget.asWidget());
    }

    private static PFlowPanel buildRadioGroup() {
        final PFlowPanel p = Element.newPFlowPanel();
        p.setStyleProperty("display", "flex");
        p.setStyleProperty("gap", "8px");
        p.add(Element.newPRadioButton("Option A"));
        p.add(Element.newPRadioButton("Option B"));
        p.add(Element.newPRadioButton("Option C"));
        return p;
    }

    private static PWidget buildRichText() {
        final PFlowPanel p = Element.newPFlowPanel();
        final PRichTextArea rta = Element.newPRichTextArea();
        p.add(Element.newPRichTextToolbar(rta));
        p.add(rta);
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
            @Override public void onCreateHeaderRow(final IsPWidget r) { r.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue"); }
            @Override public void onCreateFooterRow(final IsPWidget r) { r.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue"); }
            @Override public void onCreateRow(final IsPWidget r) {}
            @Override public boolean hasHeader() { return true; }
            @Override public boolean hasFooter() { return false; }
            @Override public IsPWidget createLoadingDataWidget() {
                final PComplexPanel div = Element.newDiv();
                div.setWidth("100%"); div.setHeight("100%");
                div.setStyleProperty("background-color", "#FFFFFF7F");
                return div;
            }
            @Override public void onCreateColumnResizer(final IsPWidget r) {}
            @Override public boolean isSelectionEnabled() { return true; }
        });
        gridView.setPollingDelayMillis(250L);

        final DataGridController<Integer, MyRow> controller = gridView.getController();
        gridView.asWidget().setHeight("500px");
        gridView.asWidget().setWidth("100%");
        gridView.asWidget().setStyleProperty("resize", "both");
        gridView.asWidget().setStyleProperty("overflow", "hidden");

        // Controls row
        final PFlowPanel controls = Element.newPFlowPanel();
        controls.setStyleProperty("display", "flex");
        controls.setStyleProperty("gap", "8px");
        controls.setStyleProperty("margin", "8px 0");

        final PTextBox pollingDelay = Element.newPTextBox();
        pollingDelay.setPlaceholder("Polling delay ms");
        pollingDelay.setStyleProperty("width", "140px");
        final PButton changePollingDelay = Element.newPButton("Set polling delay");
        changePollingDelay.addClickHandler(e -> gridView.setPollingDelayMillis(Integer.parseInt(pollingDelay.getText().trim())));

        final PButton clearSortsButton = Element.newPButton("Clear sorts");
        clearSortsButton.addClickHandler(e -> gridView.clearSorts(false));

        controls.add(pollingDelay, changePollingDelay, clearSortsButton);
        controls.add(columnVisibilitySelectorDataGridView.getDecoratorWidget());
        controls.add(configSelectorDataGridView.getDecoratorWidget());

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
        statusLabel.setStyleProperty("color", "#666");
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
        final PButton refreshBtn = Element.newPButton("⟳ Refresh");
        final PButton exportBtn = Element.newPButton("↓ Export");
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
        final PButton likeBtn = Element.newPButton("♥ Like");
        final PButton shareBtn = Element.newPButton("↗ Share");
        card.slot("actions").add(likeBtn, shareBtn);
        likeBtn.addClickHandler(e -> statusLabel.setText("Liked!"));
        shareBtn.addClickHandler(e -> statusLabel.setText("Shared!"));

        // Counter controls
        final PFlowPanel counterControls = Element.newPFlowPanel();
        counterControls.setStyleProperty("display", "flex");
        counterControls.setStyleProperty("gap", "8px");
        counterControls.setStyleProperty("margin", "8px 0");
        final PButton resetBtn = Element.newPButton("Reset counter");
        resetBtn.addClickHandler(e -> { counter.call("reset"); statusLabel.setText("reset() called"); });
        final PButton setCountBtn = Element.newPButton("Set count = 42");
        setCountBtn.addClickHandler(e -> { counter.property("count").set(42); statusLabel.setText("count set to 42"); });
        counterControls.add(resetBtn, setCountBtn);

        container.add(labeled("my-counter", counter));
        container.add(counterControls);
        container.add(labeled("my-dashboard", dashboard));
        container.add(labeled("my-card", card));
    }

    private void testScene() {
        final Scene scene1 = new AbstractScene("scene1", "Scene 1", "scene1") {

            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("Scene1 Started");
            }
        };

        final Scene scene2 = new AbstractScene("scene2", "Scene 2", "scene2") {

            @Override
            public PWidget buildGUI() {
                return Element.newPLabel("Scene2 Started");
            }
        };

        final PSimplePanel layout = Element.newPSimplePanel();
        final Router router = new Router("sample");
        router.setLayout(layout);
        router.push(scene1);
        router.push(scene2);

        PWindow.getMain().add(layout);

        router.go("scene2");
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
        final PLabel liveVisibility = Element.newPLabel("Live Visibility : Unknown");
        container.add(liveVisibility);

        final PButton button = Element.newPButton("Check visibility");
        container.add(button);

        final PLabel visibilityLabel = Element.newPLabel("Visibility : Unknown");
        container.add(visibilityLabel);

        final PScrollPanel frame = Element.newPScrollPanel();
        frame.setHeight("200px");
        frame.setWidth("300px");
        container.add(frame);

        final PFlowPanel panel = Element.newPFlowPanel();
        panel.setHeight("2000px");
        frame.add(panel);

        final PFlowPanel subPanel = Element.newPFlowPanel();
        subPanel.setStyleProperty("backgroundColor", "red");
        subPanel.setHeight("125px");
        subPanel.setWidth("200px");
        panel.add(subPanel);

        final PLabel label = Element.newPLabel("Increment : " + a++);
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
        label.setText("Increment : " + text);
    }

    private void createFunctionalLabel() {
        final TextFunction textFunction = new TextFunction(args -> {
            log.debug("{} {}", args[0], args[1]);
            return (String) args[0];
        }, "console.log(args[0] + \" \" + args[1]); return args[0];");
        final PFunctionalLabel newPFunctionalLabel = Element.newPFunctionalLabel(textFunction);
        PWindow.getMain().add(newPFunctionalLabel);
        newPFunctionalLabel.setArgs("A", "B");
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

    private void readFileItem(final FileItem item) throws IOException, FileNotFoundException {
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

    private void testPerf() {
        final PWindow w = Element.newPWindow("Window 1", "resizable=yes,location=0,status=0,scrollbars=0");
        final List<PLabel> labels = new ArrayList<>(1000);

        final PButton start = Element.newPButton("Start");
        w.add(start);
        start.addClickHandler(event -> scheduleUpdate(labels));

        for (int i = 0; i < 1000; i++) {
            final PLabel label = Element.newPLabel(counter + "-" + i);
            labels.add(label);
            w.add(label);
        }

        w.open();
    }

    private void scheduleUpdate(final List<PLabel> labels) {
        PScheduler.schedule(() -> {
            int i = 0;
            counter++;
            for (final PLabel label : labels) {
                label.setText(counter + "-" + i);
                i++;
            }
            if (counter < 20) scheduleUpdate(labels);
            else counter = 0;
        }, Duration.ofMillis(20));
    }

    private void createReconnectingPanel() {
        final PSimplePanel reconnectionPanel = Element.newPSimplePanel();
        reconnectionPanel.setAttribute("id", "reconnection");
        final PSimplePanel reconnectingPanel = Element.newPSimplePanel();
        reconnectingPanel.setAttribute("id", "reconnecting");
        reconnectionPanel.setWidget(reconnectingPanel);
        PWindow.getMain().add(reconnectionPanel);
    }

    private void downloadFile() {
        final PButton downloadImageButton = Element.newPButton("Download Pony image");
        downloadImageButton.addClickHandler(event -> UIContext.get().stackStreamRequest((request, response, uiContext1) -> {
            response.reset();
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", "attachment; filename=pony_image.png");

            try {
                final OutputStream output = response.getOutputStream();
                final InputStream input = getClass().getClassLoader().getResourceAsStream("images/pony.png");

                final byte[] buff = new byte[1024];
                while (input.read(buff) != -1) {
                    output.write(buff);
                }

                output.flush();
                output.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }));
        PWindow.getMain().add(downloadImageButton);
    }

    private void testPAddon() {
        final LoggerAddOn addon = createPAddOn();
        addon.attach(PWindow.getMain());

        // final PElementAddOn elementAddOn = new PElementAddOn();
        // elementAddOn.setInnerText("Coucou");
        // flowPanel.add(elementAddOn);

        // highChartsStackedColumnAddOn = new HighChartsStackedColumnAddOn();
        // PWindow.getMain().add(highChartsStackedColumnAddOn);
        // highChartsStackedColumnAddOn.setSeries("");

        // final HighChartsStackedColumnAddOn h2 = new
        // HighChartsStackedColumnAddOn();
        // a.add(h2);
        // h2.setSeries("");
        // final PElementAddOn elementAddOn2 = new PElementAddOn();
        // elementAddOn2.setInnerText("Coucou dans window");
        // a.add(elementAddOn2);
    }

    private void createNewEvent() {
        final EventHandler<PClickEvent> handler = UIContext.get().getNewEventBus().subscribe(PClickEvent.class,
            event -> log.debug("Event: {}", event));
        UIContext.get().getNewEventBus().post(new PClickEvent(this));
        UIContext.get().getNewEventBus().post(new PClickEvent(this));
        UIContext.get().getNewEventBus().unsubscribe(handler);
        UIContext.get().getNewEventBus().post(new PClickEvent(this));
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
        columnDescriptor1.setHeaderCellRenderer(() -> Element.newPLabel("A"));
        columnDescriptor1.setValueProvider(new IdentityValueProvider<>());
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);

        for (int i = 0; i < 40; i++) {

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
            columnDescriptor3.setHeaderCellRenderer(() -> Element.newPLabel("B"));
            columnDescriptor3.setValueProvider(new IdentityValueProvider<>());
            grid.addDataGridColumnDescriptor(columnDescriptor3);
        }

        grid.setData(0, 1, new Data(1, "AA"));
        grid.setData(1, 2, new Data(2, "BB"));
        final Data data = new Data(3, "CC");
        grid.setData(2, 3, data);

        final AtomicInteger i = new AtomicInteger();
        schedulers.add(PScheduler.scheduleWithFixedDelay(() -> {
            for (int key = 1; key < 50; key++) {
                grid.setData(key - 1, key, new Data(key, "" + i.incrementAndGet()));
            }
        }, Duration.ofMillis(500), Duration.ofMillis(200)));

        return grid;
    }

    private void testUIDelegator() {
        final PLabel a = Element.newPLabel();
        PWindow.getMain().add(a);
        final AtomicInteger ai = new AtomicInteger();
        PScheduler.scheduleAtFixedRate(() -> a.setText("a " + ai.incrementAndGet()), Duration.ofMillis(0), Duration.ofMillis(10));

        final PLabel p = Element.newPLabel();
        PWindow.getMain().add(p);
    }

    private PWindow createWindow() {
        final PWindow w = Element.newPWindow("Window 1", "resizable=yes,location=0,status=0,scrollbars=0");

        final PFlowPanel windowContainer = Element.newPFlowPanel();
        w.add(windowContainer);

        final PLabel child = Element.newPLabel("Window 1");
        child.setText("Modified Window 1");
        windowContainer.add(child);

        final PButton button = Element.newPButton("Modified main label on main window");
        windowContainer.add(button);
        button.addClickHandler(event -> {
            mainLabel.setText("Touched by God : " + child.getWindow());
            child.setText("Clicked Window 1");
        });
        windowContainer.add(button);

        final AtomicInteger i = new AtomicInteger();

        final PButton button1 = Element.newPButton("Open linked window");
        windowContainer.add(button1);
        button1.addClickHandler(event -> {
            final PWindow newPWindow = Element.newPWindow(w, "Sub Window 1 " + i.incrementAndGet(),
                "resizable=yes,location=0,status=0,scrollbars=0");
            newPWindow.add(Element.newPLabel("Sub window"));
            newPWindow.open();
        });

        final PButton button2 = Element.newPButton("Open not linked window");
        windowContainer.add(button2);
        button2.addClickHandler(event -> {
            final PWindow newPWindow = Element.newPWindow("Not Sub Window 1 " + i.incrementAndGet(),
                "resizable=yes,location=0,status=0,scrollbars=0");
            newPWindow.add(Element.newPLabel("Sub window"));
            newPWindow.open();
        });

        PScheduler.scheduleAtFixedRate(() -> {
            final PLabel label = Element.newPLabel();
            label.setText("Window 1 " + i.incrementAndGet());
            windowContainer.add(label);
            windowContainer.add(Element.newPCheckBox("Checkbox"));
        }, Duration.ofSeconds(1), Duration.ofSeconds(10));

        final PFrame frame = Element.newPFrame("http://localhost:8081/sample/");
        frame.add(Element.newPLabel("Inside the frame"));
        w.add(frame);

        return w;
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }

    private static final PStackLayoutPanel createStackLayoutPanel() {
        final PStackLayoutPanel child = Element.newPStackLayoutPanel(PUnit.CM);
        child.add(Element.newPLabel("Text"), "Text", false, 1.0);
        return child;
    }

    private static final PListBox createListBox() {
        final PListBox pListBox = Element.newPListBox(true);
        pListBox.addItem("A");
        pListBox.addItem("B");
        pListBox.insertItem("C", 1);
        pListBox.addItemsInGroup("sport", "Baseball", "Basketball", "Football", "Hockey", "Water Polo");
        return pListBox;
    }

    private static final PTabLayoutPanel createTabLayoutPanel() {
        final PTabLayoutPanel child = Element.newPTabLayoutPanel();
        child.add(Element.newPLabel("text"), "text");
        return child;
    }

    private static final PMenuBar createMenu() {
        final PMenuBar pMenuBar = Element.newPMenuBar(true);
        pMenuBar.addItem(Element.newPMenuItem("Menu 1", Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 2", true, Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 3", () -> log.debug("Menu click")));
        pMenuBar.addSeparator();
        return pMenuBar;
    }

    private static final PFlowPanel createPFlowPanel() {
        final PFlowPanel panel1 = Element.newPFlowPanel();
        panel1.setAttribute("id", "panel1");
        final PFlowPanel panel2_1 = Element.newPFlowPanel();
        panel2_1.setAttribute("id", "panel2_1");
        final PFlowPanel panel3_1_1 = Element.newPFlowPanel();
        panel3_1_1.setAttribute("id", "panel3_1_1");
        final PFlowPanel panel3_1_2 = Element.newPFlowPanel();
        panel3_1_2.setAttribute("id", "panel3_1_2");
        final PFlowPanel panel2_2 = Element.newPFlowPanel();
        panel2_2.setAttribute("id", "panel2_2");
        final PFlowPanel panel3_2_1 = Element.newPFlowPanel();
        panel3_2_1.setAttribute("id", "panel3_2_1");
        final PFlowPanel panel3_2_2 = Element.newPFlowPanel();
        panel3_2_2.setAttribute("id", "panel3_2_2");

        panel1.add(panel2_1);
        panel2_1.add(panel3_1_1);
        panel2_1.add(panel3_1_2);
        panel1.add(panel2_2);
        panel2_2.add(panel3_2_1);
        panel2_2.add(panel3_2_2);

        return panel1;
    }

    private static final PWidget createBlock(final PWidget child) {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(child);
        return panel;
    }

    private static final PDockLayoutPanel createDockLayoutPanel() {
        final PDockLayoutPanel pDockLayoutPanel = Element.newPDockLayoutPanel(PUnit.CM);
        pDockLayoutPanel.addNorth(Element.newPLabel("LabelDock"), 1.5);
        return pDockLayoutPanel;
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

    private static final LoggerAddOn createPAddOn() {
        final LoggerAddOn labelPAddOn = new LoggerAddOn();
        labelPAddOn.log("addon logger test");

        labelPAddOn.setAjaxHandler((req, resp) -> {
            final String header = req.getHeader("info");

            if (header.equals("Get Data")) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json");
                resp.getWriter().print("{\"response\": \"" + header + "\"}");
                resp.getWriter().flush();
            } else {
                resp.sendError(500);
            }
        });

        labelPAddOn.setTerminalHandler(event -> log.debug("AddOn event: {}", event));

        return labelPAddOn;
    }

    private static final PTextBox createPTextBox() {
        final PTextBox pTextBox = Element.newPTextBox();

        pTextBox.addKeyUpHandler(new PKeyUpHandler() {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                PScript.execute(PWindow.getMain(), "alert('" + keyUpEvent + "');");
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] { PKeyCodes.ENTER };
            }
        });
        return pTextBox;
    }

    private static final PTree createTree() {
        final PTree tree = Element.newPTree();

        final PTreeItem firstFolder = tree.add("First");
        firstFolder.add("2");
        firstFolder.add(0, Element.newPTreeItem("1"));

        firstFolder.setState(true);

        final PTreeItem secondFolder = Element.newPTreeItem("Second");
        final PTreeItem subItem = secondFolder.add(Element.newPTreeItem());
        subItem.setText("3");
        secondFolder.add(Element.newPTreeItem(Element.newPLabel("4")));
        tree.add(secondFolder);

        secondFolder.setSelected(true);

        return tree;
    }

    private static final PAbsolutePanel createAbsolutePanel() {
        final PAbsolutePanel pAbsolutePanel = Element.newPAbsolutePanel();
        pAbsolutePanel.add(Element.newDiv());
        pAbsolutePanel.add(Element.newP());
        return pAbsolutePanel;
    }

    private static final PButton createButton() {
        final PButton pButton = Element.newPButton("Button 1");
        pButton.addClickHandler(handler -> pButton.setText("Button 1 clicked"));
        return pButton;
    }

}
