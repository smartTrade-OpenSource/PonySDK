/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.wa.*;
import com.ponysdk.core.ui.wa.datatable.ColumnDef;
import com.ponysdk.core.ui.wa.datatable.PDataTable;
import com.ponysdk.core.ui.wa.form.PForm;
import com.ponysdk.core.ui.wa.layout.PContainer;
import com.ponysdk.core.ui.wa.layout.PResponsiveGrid;
import com.ponysdk.core.ui.wa.layout.PStack;
import com.ponysdk.core.ui.wa.theme.ThemeEngine;

/**
 * EntryPoint for the Web Awesome Showcase.
 * Demonstrates all hand-written Web Awesome wrapper components.
 * <p>
 * Run with:
 * <pre>
 *   ./gradlew :sample:run -Dapplication.pointclass=com.ponysdk.sample.client.WebAwesomeShowcaseEntryPoint
 * </pre>
 * Then open http://localhost:8081 in Chrome.
 */
public class WebAwesomeShowcaseEntryPoint implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(WebAwesomeShowcaseEntryPoint.class);

    @Override
    public void start(final UIContext uiContext) {
        log.info("=== Web Awesome Showcase EntryPoint ===");

        // Load Web Awesome registry and wait for it to be ready
        final String loadRegistry = 
            "(function() {" +
            "  window.waRegistryReady = false;" +
            "  const script = document.createElement('script');" +
            "  script.src = 'script/web-awesome-registry.js?v=2';" +
            "  script.onload = function() {" +
            "    setTimeout(function() {" +
            "      window.waRegistryReady = true;" +
            "      console.log('[EntryPoint] Web Awesome registry ready');" +
            "    }, 100);" +
            "  };" +
            "  document.head.appendChild(script);" +
            "})();";
        PScript.execute(PWindow.getMain(), loadRegistry);

        // Load Web Awesome from CDN
        final String loadWebAwesome = 
            "(function() {" +
            "  if (!customElements.get('wa-button')) {" +
            "    const link = document.createElement('link');" +
            "    link.rel = 'stylesheet';" +
            "    link.href = 'https://cdn.jsdelivr.net/npm/@awesome.me/webawesome@latest/dist-cdn/styles/themes/default.css';" +
            "    document.head.appendChild(link);" +
            "    const script = document.createElement('script');" +
            "    script.type = 'module';" +
            "    script.src = 'https://cdn.jsdelivr.net/npm/@awesome.me/webawesome@latest/dist-cdn/webawesome.loader.js';" +
            "    document.head.appendChild(script);" +
            "    console.log('Web Awesome loaded from CDN');" +
            "  }" +
            "})();";
        PScript.execute(PWindow.getMain(), loadWebAwesome);
        Txn.get().flush();


        final PFlowPanel root = Element.newPFlowPanel();
        PWindow.getMain().add(root);

        // Title
        final PLabel title = Element.newPLabel("Web Awesome Showcase - Check Console Logs");
        title.setStyleProperty("font-size", "2rem");
        title.setStyleProperty("font-weight", "bold");
        title.setStyleProperty("margin", "1rem");
        root.add(title);

        final PLabel subtitle = Element.newPLabel("All components are instantiated and logged to the server console.");
        subtitle.setStyleProperty("font-size", "1rem");
        subtitle.setStyleProperty("margin", "1rem");
        subtitle.setStyleProperty("color", "#666");
        root.add(subtitle);

        // Theme section
        log.info("=== Theme Engine ===");
        setupTheme();

        // Layout section
        log.info("=== Layout Components ===");
        setupGrid();
        setupVStack();
        setupHStack();

        // Container/Divider/SplitPanel section
        log.info("=== Container, Divider, SplitPanel ===");
        setupContainer();
        setupDivider();
        setupSplitPanel();

        // Shared enums section
        log.info("=== Shared Enums (Variant, Size) ===");
        setupSharedEnums();

        // Web Awesome components section
        log.info("=== Web Awesome Components (waiting for registry) ===");
        
        // Delay component creation to ensure registry is loaded
        try {
            Thread.sleep(500); // Wait 500ms for registry to load
        } catch (InterruptedException e) {
            log.warn("Sleep interrupted", e);
        }

        setupWebAwesomeComponents(root);

        // Form section
        log.info("=== Form ===");
        setupForm();

        // DataTable section
        log.info("=== DataTable ===");
        setupDataTable();

        log.info("=== Showcase complete ===");
    }

    private void setupTheme() {
        final ThemeEngine theme = new ThemeEngine();
        theme.applyTheme("light");
        theme.setToken("primary-color", "#1e40af");
        theme.createCustomTheme("brand", "light", Map.of(
            "primary-color", "#7c3aed",
            "success-color", "#059669"
        ));
        theme.applyTheme("brand");
        log.info("Theme: active={}", theme.getActiveThemeId());
    }

    private PResponsiveGrid setupGrid() {
        final PResponsiveGrid grid = new PResponsiveGrid();
        grid.setColumns(3);
        grid.setGap("1rem");
        grid.setHideOnMobile(false);
        log.info("Grid: columns={}, gap={}", grid.getCurrentProps().columns(), grid.getCurrentProps().gap());
        return grid;
    }

    private PStack setupVStack() {
        final PStack vStack = new PStack();
        vStack.setOrientation("vertical");
        vStack.setGap("0.5rem");
        vStack.setAlignment("stretch");
        vStack.setJustification("flex-start");
        log.info("VStack: orientation={}, gap={}", vStack.getCurrentProps().orientation(), vStack.getCurrentProps().gap());
        return vStack;
    }

    private PStack setupHStack() {
        final PStack hStack = new PStack();
        hStack.setOrientation("horizontal");
        hStack.setGap("1rem");
        hStack.setWrap(true);
        log.info("HStack: orientation={}, gap={}, wrap={}", hStack.getCurrentProps().orientation(), hStack.getCurrentProps().gap(), hStack.getCurrentProps().wrap());
        return hStack;
    }

    private PContainer setupContainer() {
        final PContainer container = new PContainer();
        container.setMaxWidth("1200px");
        container.setPadding("1rem");
        container.setCentered(true);
        log.info("Container: maxWidth={}, padding={}, centered={}", container.getCurrentProps().maxWidth(), container.getCurrentProps().padding(), container.getCurrentProps().centered());
        return container;
    }

    private WADivider setupDivider() {
        final WADivider divider = new WADivider();
        divider.setOrientation("horizontal");
        log.info("Divider: orientation={}", divider.getCurrentProps().orientation());
        return divider;
    }

    private WASplitPanel setupSplitPanel() {
        final WASplitPanel splitPanel = new WASplitPanel();
        splitPanel.setPosition(30);
        splitPanel.setOrientation("horizontal");
        splitPanel.setDisabled(false);
        log.info("SplitPanel: position={}, orientation={}, disabled={}", splitPanel.getCurrentProps().position(), splitPanel.getCurrentProps().orientation(), splitPanel.getCurrentProps().disabled());
        return splitPanel;
    }

    private void setupSharedEnums() {
        for (final Variant v : Variant.values()) {
            log.info("Variant: {} → {}", v.name(), v.getValue());
        }
        for (final Size s : Size.values()) {
            log.info("Size: {} → {}", s.name(), s.getValue());
        }
    }

    private void setupWebAwesomeComponents(final PFlowPanel root) {
        // Section title
        final PLabel sectionTitle = Element.newPLabel("Web Awesome Components (58 generated wa-* components)");
        sectionTitle.setStyleProperty("font-size", "1.5rem");
        sectionTitle.setStyleProperty("font-weight", "bold");
        sectionTitle.setStyleProperty("margin-top", "2rem");
        sectionTitle.setStyleProperty("margin-bottom", "1rem");
        sectionTitle.setStyleProperty("color", "#0969da");
        root.add(sectionTitle);

        // Form inputs
        final WAButton button = new WAButton();
        button.setVariant("primary");
        button.setSize("medium");
        button.attach(PWindow.getMain());
        log.info("Created wa-button");

        final WAInput input = new WAInput();
        input.setType("text");
        input.setPlaceholder("Enter text...");
        input.attach(PWindow.getMain());
        log.info("Created wa-input");

        final WANumberInput numberInput = new WANumberInput();
        numberInput.setPlaceholder("Enter number...");
        numberInput.attach(PWindow.getMain());
        log.info("Created wa-number-input");

        final WACheckbox checkbox = new WACheckbox();
        checkbox.setChecked(false);
        checkbox.attach(PWindow.getMain());
        log.info("Created wa-checkbox");

        final WASwitch switchComp = new WASwitch();
        switchComp.setChecked(false);
        switchComp.attach(PWindow.getMain());
        log.info("Created wa-switch");

        final WARadio radio = new WARadio();
        radio.attach(PWindow.getMain());
        log.info("Created wa-radio");

        final WARadioGroup radioGroup = new WARadioGroup();
        radioGroup.attach(PWindow.getMain());
        log.info("Created wa-radio-group");

        final WASelect select = new WASelect();
        select.setPlaceholder("Select an option");
        select.attach(PWindow.getMain());
        log.info("Created wa-select");

        final WAOption option = new WAOption();
        option.attach(PWindow.getMain());
        log.info("Created wa-option");

        final WATextarea textarea = new WATextarea();
        textarea.setPlaceholder("Enter text...");
        textarea.setRows(3);
        textarea.attach(PWindow.getMain());
        log.info("Created wa-textarea");

        final WASlider slider = new WASlider();
        slider.attach(PWindow.getMain());
        log.info("Created wa-slider");

        final WAColorPicker colorPicker = new WAColorPicker();
        colorPicker.attach(PWindow.getMain());
        log.info("Created wa-color-picker");

        // Progress & feedback
        final WAProgressBar progressBar = new WAProgressBar();
        progressBar.setValue(50);
        progressBar.setLabel("Loading...");
        progressBar.attach(PWindow.getMain());
        log.info("Created wa-progress-bar");

        final WAProgressRing progressRing = new WAProgressRing();
        progressRing.attach(PWindow.getMain());
        log.info("Created wa-progress-ring");

        final WASpinner spinner = new WASpinner();
        spinner.attach(PWindow.getMain());
        log.info("Created wa-spinner");

        final WASkeleton skeleton = new WASkeleton();
        skeleton.attach(PWindow.getMain());
        log.info("Created wa-skeleton");

        // Display components
        final WABadge badge = new WABadge();
        badge.setVariant("primary");
        badge.attach(PWindow.getMain());
        log.info("Created wa-badge");

        final WATag tag = new WATag();
        tag.setVariant("primary");
        tag.attach(PWindow.getMain());
        log.info("Created wa-tag");

        final WAAvatar avatar = new WAAvatar();
        avatar.attach(PWindow.getMain());
        log.info("Created wa-avatar");

        final WAIcon icon = new WAIcon();
        icon.attach(PWindow.getMain());
        log.info("Created wa-icon");

        final WAAnimatedImage animatedImage = new WAAnimatedImage();
        animatedImage.attach(PWindow.getMain());
        log.info("Created wa-animated-image");

        final WAQrCode qrCode = new WAQrCode();
        qrCode.attach(PWindow.getMain());
        log.info("Created wa-qr-code");

        final WARating rating = new WARating();
        rating.attach(PWindow.getMain());
        log.info("Created wa-rating");

        // Layout & containers
        final WACard card = new WACard();
        card.attach(PWindow.getMain());
        log.info("Created wa-card");

        final WADialog dialog = new WADialog();
        dialog.attach(PWindow.getMain());
        log.info("Created wa-dialog");

        final WADrawer drawer = new WADrawer();
        drawer.attach(PWindow.getMain());
        log.info("Created wa-drawer");

        final WAPopup popup = new WAPopup();
        popup.attach(PWindow.getMain());
        log.info("Created wa-popup");

        final WATooltip tooltip = new WATooltip();
        tooltip.attach(PWindow.getMain());
        log.info("Created wa-tooltip");

        final WADetails details = new WADetails();
        details.attach(PWindow.getMain());
        log.info("Created wa-details");

        final WADivider divider = new WADivider();
        divider.attach(PWindow.getMain());
        log.info("Created wa-divider");

        final WASplitPanel splitPanel = new WASplitPanel();
        splitPanel.attach(PWindow.getMain());
        log.info("Created wa-split-panel");

        // Navigation
        final WABreadcrumb breadcrumb = new WABreadcrumb();
        breadcrumb.attach(PWindow.getMain());
        log.info("Created wa-breadcrumb");

        final WABreadcrumbItem breadcrumbItem = new WABreadcrumbItem();
        breadcrumbItem.attach(PWindow.getMain());
        log.info("Created wa-breadcrumb-item");

        final WADropdown dropdown = new WADropdown();
        dropdown.attach(PWindow.getMain());
        log.info("Created wa-dropdown");

        final WATabGroup tabGroup = new WATabGroup();
        tabGroup.attach(PWindow.getMain());
        log.info("Created wa-tab-group");

        final WATab tab = new WATab();
        tab.attach(PWindow.getMain());
        log.info("Created wa-tab");

        final WATabPanel tabPanel = new WATabPanel();
        tabPanel.attach(PWindow.getMain());
        log.info("Created wa-tab-panel");

        final WATree tree = new WATree();
        tree.attach(PWindow.getMain());
        log.info("Created wa-tree");

        final WATreeItem treeItem = new WATreeItem();
        treeItem.attach(PWindow.getMain());
        log.info("Created wa-tree-item");

        // Utilities
        final WAButtonGroup buttonGroup = new WAButtonGroup();
        buttonGroup.attach(PWindow.getMain());
        log.info("Created wa-button-group");

        final WACarousel carousel = new WACarousel();
        carousel.attach(PWindow.getMain());
        log.info("Created wa-carousel");

        final WACarouselItem carouselItem = new WACarouselItem();
        carouselItem.attach(PWindow.getMain());
        log.info("Created wa-carousel-item");

        final WACopyButton copyButton = new WACopyButton();
        copyButton.attach(PWindow.getMain());
        log.info("Created wa-copy-button");

        final WAFormatBytes formatBytes = new WAFormatBytes();
        formatBytes.attach(PWindow.getMain());
        log.info("Created wa-format-bytes");

        // TODO: Fix WAFormatDate and WAFormatNumber imports
        // final WAFormatDate formatDate = new WAFormatDate();
        // formatDate.attach(PWindow.getMain());
        // log.info("Created wa-format-date");

        // final WAFormatNumber formatNumber = new WAFormatNumber();
        // formatNumber.attach(PWindow.getMain());
        // log.info("Created wa-format-number");

        final WAInclude include = new WAInclude();
        include.attach(PWindow.getMain());
        log.info("Created wa-include");

        final WARelativeTime relativeTime = new WARelativeTime();
        relativeTime.attach(PWindow.getMain());
        log.info("Created wa-relative-time");

        final WAResizeObserver resizeObserver = new WAResizeObserver();
        resizeObserver.attach(PWindow.getMain());
        log.info("Created wa-resize-observer");

        // Additional components that were missing from the old registry
        final WAPopover popover = new WAPopover();
        popover.attach(PWindow.getMain());
        log.info("Created wa-popover");

        final WACallout callout = new WACallout();
        callout.attach(PWindow.getMain());
        log.info("Created wa-callout");

        final WADropdownItem dropdownItem = new WADropdownItem();
        dropdownItem.attach(PWindow.getMain());
        log.info("Created wa-dropdown-item");

        final WAAnimation animation = new WAAnimation();
        animation.attach(PWindow.getMain());
        log.info("Created wa-animation");

        final WAComparison comparison = new WAComparison();
        comparison.attach(PWindow.getMain());
        log.info("Created wa-comparison");

        final WAIntersectionObserver intersectionObserver = new WAIntersectionObserver();
        intersectionObserver.attach(PWindow.getMain());
        log.info("Created wa-intersection-observer");

        final WAMutationObserver mutationObserver = new WAMutationObserver();
        mutationObserver.attach(PWindow.getMain());
        log.info("Created wa-mutation-observer");

        final WAScroller scroller = new WAScroller();
        scroller.attach(PWindow.getMain());
        log.info("Created wa-scroller");

        final WAZoomableFrame zoomableFrame = new WAZoomableFrame();
        zoomableFrame.attach(PWindow.getMain());
        log.info("Created wa-zoomable-frame");

        final PLabel note = Element.newPLabel("(58 Web Awesome components instantiated using generated wrappers)");
        note.setStyleProperty("font-size", "0.875rem");
        note.setStyleProperty("color", "#666");
        note.setStyleProperty("margin-bottom", "1rem");
        root.add(note);
    }

    // ================================================================
    // Form with input components
    // ================================================================

    private PForm setupForm() {
        final PForm form = new PForm();
        form.onSubmit(values -> log.info("Form submitted: {}", values));
        log.info("Form: {} fields", form.getFields().size());
        return form;
    }

    private PDataTable setupDataTable() {
        final PDataTable table = new PDataTable();

        table.setColumns(List.of(
            ColumnDef.sortable("name", "Name"),
            ColumnDef.sortable("email", "Email"),
            ColumnDef.of("role", "Role"),
            new ColumnDef("age", "Age", "number", true, false, Optional.of(80))
        ));

        table.setData(List.of(
            Map.of("name", "Alice", "email", "alice@example.com", "role", "Admin", "age", 32),
            Map.of("name", "Bob", "email", "bob@example.com", "role", "User", "age", 28),
            Map.of("name", "Charlie", "email", "charlie@example.com", "role", "User", "age", 45)
        ));

        table.setPage(0);
        table.setPageSize(25);
        table.setVirtualScroll(false);

        table.onSort(sortEvent ->
            log.info("Sort: field={}, direction={}", sortEvent.field(), sortEvent.direction()));
        table.onPageChange(page ->
            log.info("Page changed to: {}", page));
        table.onSelectionChange(selected ->
            log.info("Selected rows: {}", selected));

        log.info("DataTable: {} columns, {} rows", table.getCurrentProps().columns().size(), table.getCurrentProps().data().size());
        return table;
    }
}
