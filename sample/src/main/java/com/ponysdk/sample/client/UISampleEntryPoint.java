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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.ponysdk.sample.client.Costs.*;
import static com.ponysdk.sample.client.Ui.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

public class UISampleEntryPoint implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(UISampleEntryPoint.class);

    /** Opens a page of the showcase (index in the sidebar), from anywhere in the showcase. */
    private java.util.function.IntConsumer navigate = i -> { };

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setTerminalDataReceiver((object, instruction) -> log.debug("{} : {}", object, instruction));

        // Styles: css/showcase.css (declared in ShowcaseMain)
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

        // Deep links: each tab has its URL token (#component-costs); a reload or a shared link opens that tab
        final String[] slugs = new String[tabs.length];
        for (int i = 0; i < tabs.length; i++) slugs[i] = tabs[i][1].toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
        final int first = Math.max(0, java.util.Arrays.asList(slugs).indexOf(uiContext.getHistory().getToken()));

        // Only the opened tab is built eagerly: the others are built (and their schedulers started) on activation
        final PFlowPanel[] tabPanels = new PFlowPanel[tabs.length];
        tabPanels[first] = buildTabWithSchedulers(first, uiContext, tabSchedulers[first]);

        final int tabCount = tabs.length;
        final PButton[] tabButtons = new PButton[tabCount];
        final int[] activeTab = { first };
        final java.util.function.IntConsumer showCrumb = k -> {
            int g = 0;
            while (g + 1 < groupStarts.length && groupStarts[g + 1] <= k) g++;
            crumb.setInnerHTML(groupLabels[g] + " &nbsp;/&nbsp; <b>" + tabs[k][1] + "</b>");
        };
        showCrumb.accept(first);

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
            if (i == first) tab.addStyleName("pony-tab-active");
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
            uiContext.getHistory().newItem(slugs[idx], false);
            PScript.execute(PWindow.getMain(), "window.scrollTo(0,0)");
        };
        // back / forward buttons of the browser
        uiContext.getHistory().addValueChangeHandler(e -> {
            final int idx = java.util.Arrays.asList(slugs).indexOf(e.getData());
            navigate.accept(idx >= 0 ? idx : 0);
        });

        // Nav footer with version
        final PFlowPanel navFooter = Element.newPFlowPanel();
        navFooter.addStyleName("pony-nav-footer");
        final PElement version = Element.newDiv();
        version.addStyleName("pony-version");
        version.setInnerText("PonySDK 3.0 · Java " + System.getProperty("java.version").split("\\.")[0]);
        navFooter.add(version);
        nav.add(navFooter);

        // Show the opened tab
        content.setWidget(tabPanels[first]);

        shell.add(nav);
        shell.add(content);
        PWindow.getMain().add(shell);
    }

    @SuppressWarnings("unchecked")
    private PFlowPanel buildTabWithSchedulers(final int idx, final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        return switch (idx) {
            case 0 -> OverviewPage.build(uiContext, schedulers, i -> navigate.accept(i));
            case 1 -> InputsPage.build(schedulers);
            case 2 -> LayoutsPage.build(schedulers);
            case 3 -> DataPage.build(schedulers);
            case 4 -> DataGridPage.build(schedulers);
            case 5 -> WebComponentsPage.build(schedulers);
            case 6 -> MiscPage.build(schedulers);
            case 7 -> PerformancePage.build(uiContext, schedulers);
            case 8 -> CostsPage.build();
            case 9 -> BinaryProtocolPage.build(schedulers);
            case 10 -> CodePage.build();
            default -> FlexLayoutPage.build();
        };
    }

}
