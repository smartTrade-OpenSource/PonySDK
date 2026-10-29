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
import com.ponysdk.core.ui.basic.PHost;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.signal.Bind;

public class UISampleEntryPoint implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(UISampleEntryPoint.class);

    /** Opens a page of the showcase (index in the sidebar), from anywhere in the showcase. */
    private java.util.function.IntConsumer navigate = i -> { };
    /** The pages in sidebar order: a page is built and opened by its name, not its position. */
    private String[] tabNames;

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setTerminalDataReceiver((object, instruction) -> log.debug("{} : {}", object, instruction));
        Bind.configure(Duration.ofMillis(50), null); // what the Signals page binds reaches the browser every 50 ms at most

        final String token = uiContext.getHistory().getToken();
        if (token != null && token.startsWith(ReconnectionPage.CLIENT_TOKEN)) {
            ReconnectionPage.buildClient(uiContext, token); // the session in the Reconnection page's frame
            return;
        }
        if (token != null && token.startsWith(OraclePages.TOKEN)) {
            OraclePages.build(token); // a page of the behaviour oracle (sample/tests/oracle.spec.js)
            return;
        }
        if (token != null && token.startsWith(ThemePage.PREVIEW_TOKEN)) {
            ThemePage.buildPreview(); // the frame of the Theme page
            return;
        }
        if (token != null && token.startsWith(TerminalPages.TOKEN)) {
            TerminalPages.build(token); // a page of the terminal's features (sample/tests/terminal-pages.spec.js)
            return;
        }
        // region embedded: in a <pony-app> element of another page (docs/decisions/0008-embedded-application.md)
        final PHost host = PHost.get();
        if (host != null && EmbeddedTicket.VIEW.equals(host.getAttribute("view"))) {
            EmbeddedTicket.build(uiContext, host);
            return;
        }
        // endregion: the portal (portal/portal.html) shows this region

        // Styles: css/showcase.css (declared in ShowcaseMain)
        // Favicon: the "P" monogram of the top bar, inline SVG (no extra request); an embedded showcase leaves the
        // host page's
        if (host == null) PScript.call(PWindow.getMain(), "showcaseFavicon"); // script/sample.js

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

        PWindow.getMain().add(reconnectionNotice()); // the badge follows it (showcase.css)

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
            {"◎", "Overview"}, {"</>", "Code"},
            {"⌨", "Inputs"}, {"☑", "Forms"}, {"⊞", "Layouts"}, {"◈", "Data"},
            {"⊟", "DataGrid"}, {"◉", "Web Components"}, {"▦", "FlexLayout"}, {"∿", "Charts"}, {"⋯", "Misc"}, {"◐", "Theme"},
            {"⚡", "Performance"}, {"≋", "Streams"}, {"⇅", "Signals"}, {"⚖", "Component costs"}, {"≡", "Binary protocol"}, {"⟲", "Reconnection"},
            {"⧉", "Embedding"}, {"⇄", "FDC3"}, {"↦", "Routing"}, {"Aa", "i18n"}
        };
        final String[] groupLabels = {"Start", "Widgets", "Under the hood"};
        final int[] groupStarts = {0, 2, 12};
        tabNames = new String[tabs.length];
        for (int i = 0; i < tabs.length; i++) tabNames[i] = tabs[i][1];

        // Active tab schedulers — cancelled on tab leave, list cleared and refilled on re-entry
        @SuppressWarnings("unchecked")
        final List<PScheduler.UIRunnable>[] tabSchedulers = new List[tabs.length];
        for (int i = 0; i < tabs.length; i++) tabSchedulers[i] = new ArrayList<>();

        // Deep links: each tab has its URL token (#component-costs); a reload or a shared link opens that tab
        final String[] slugs = new String[tabs.length];
        for (int i = 0; i < tabs.length; i++) slugs[i] = tabs[i][1].toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
        final int first = Math.max(0, java.util.Arrays.asList(slugs).indexOf(slugOf(uiContext.getHistory().getToken())));

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
            if (host == null) PScript.call(PWindow.getMain(), "scrollTo", 0, 0); // embedded: the host's scroll
        };
        // back / forward buttons of the browser
        uiContext.getHistory().addValueChangeHandler(e -> {
            final int idx = java.util.Arrays.asList(slugs).indexOf(slugOf(e.getData()));
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

    /** A tab's slug is the first segment of the token: a page with routes of its own keeps the rest (#routing/orders/42). */
    private static String slugOf(final String token) {
        return token == null ? null : token.split("[/?]", 2)[0];
    }

    @SuppressWarnings("unchecked")
    private PFlowPanel buildTabWithSchedulers(final int idx, final UIContext uiContext, final List<PScheduler.UIRunnable> schedulers) {
        return switch (tabNames[idx]) {
            case "Overview" -> OverviewPage.build(uiContext, schedulers,
                name -> navigate.accept(java.util.Arrays.asList(tabNames).indexOf(name)));
            case "Code" -> CodePage.build(schedulers);
            case "Inputs" -> InputsPage.build(schedulers);
            case "Forms" -> FormsPage.build();
            case "Layouts" -> LayoutsPage.build(schedulers);
            case "Data" -> DataPage.build(schedulers);
            case "DataGrid" -> DataGridPage.build(schedulers);
            case "Web Components" -> WebComponentsPage.build(schedulers);
            case "FlexLayout" -> FlexLayoutPage.build();
            case "Charts" -> ChartsPage.build(uiContext, schedulers);
            case "Misc" -> MiscPage.build(schedulers);
            case "Theme" -> ThemePage.build();
            case "Performance" -> PerformancePage.build(uiContext, schedulers);
            case "Streams" -> StreamsPage.build(uiContext, schedulers);
            case "Signals" -> SignalsPage.build(uiContext, schedulers);
            case "Component costs" -> CostsPage.build();
            case "Binary protocol" -> BinaryProtocolPage.build(schedulers);
            case "Reconnection" -> ReconnectionPage.build(uiContext, schedulers);
            case "Embedding" -> EmbeddingPage.build(uiContext, schedulers);
            case "FDC3" -> Fdc3Page.build();
            case "Routing" -> RoutingPage.build(uiContext);
            case "i18n" -> I18nPage.build(uiContext);
            default -> throw new IllegalArgumentException("No page " + tabNames[idx]);
        };
    }

}
