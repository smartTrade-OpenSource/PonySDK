package com.ponysdk.sample.client.page.addon;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

/**
 * Demo EntryPoint for FlexLayout integration.
 * Drag labels from the toolbar onto the layout to create new tabs with PWidgets.
 */
public class FlexLayoutDemoEntryPoint implements EntryPoint {

    @Override
    public void start(final UIContext uiContext) {
        // Main wrapper: full viewport
        final PFlowPanel root = Element.newPFlowPanel();
        root.setStyleProperty("display", "flex");
        root.setStyleProperty("flex-direction", "column");
        root.setStyleProperty("width", "100vw");
        root.setStyleProperty("height", "100vh");
        root.setStyleProperty("overflow", "hidden");

        // Toolbar with drag sources
        final PFlowPanel toolbar = Element.newPFlowPanel();
        toolbar.setStyleProperty("display", "flex");
        toolbar.setStyleProperty("align-items", "center");
        toolbar.setStyleProperty("gap", "8px");
        toolbar.setStyleProperty("padding", "6px 12px");
        toolbar.setStyleProperty("background", "#181825");
        toolbar.setStyleProperty("border-bottom", "1px solid #313244");
        toolbar.setStyleProperty("flex-shrink", "0");
        toolbar.setStyleProperty("user-select", "none");

        final PLabel title = Element.newPLabel("FlexLayout Demo");
        title.setStyleProperty("color", "#89b4fa");
        title.setStyleProperty("font-weight", "bold");
        title.setStyleProperty("font-size", "13px");
        toolbar.add(title);

        // Drag source: Label
        final PLabel dragLabel = Element.newPLabel("Drag: Label");
        dragLabel.addStyleName("fl-drag-src-label");
        styleDragSource(dragLabel);
        toolbar.add(dragLabel);

        // Drag source: Button
        final PLabel dragButton = Element.newPLabel("Drag: Button");
        dragButton.addStyleName("fl-drag-src-button");
        styleDragSource(dragButton);
        toolbar.add(dragButton);

        // Drag source: TextBox
        final PLabel dragTextBox = Element.newPLabel("Drag: TextBox");
        dragTextBox.addStyleName("fl-drag-src-textbox");
        styleDragSource(dragTextBox);
        toolbar.add(dragTextBox);

        // Drag source: Interactive
        final PLabel dragInteractive = Element.newPLabel("Drag: Interactive");
        dragInteractive.addStyleName("fl-drag-src-interactive");
        styleDragSource(dragInteractive);
        toolbar.add(dragInteractive);

        // Add tab button (programmatic)
        final PButton addBtn = Element.newPButton("+ Add Tab");
        addBtn.setStyleProperty("background", "#313244");
        addBtn.setStyleProperty("border", "1px solid #89b4fa");
        addBtn.setStyleProperty("color", "#89b4fa");
        addBtn.setStyleProperty("border-radius", "4px");
        addBtn.setStyleProperty("padding", "3px 10px");
        addBtn.setStyleProperty("cursor", "pointer");
        addBtn.setStyleProperty("font-size", "12px");
        toolbar.add(addBtn);

        // Theme selector
        final com.ponysdk.core.ui.basic.PListBox themeSelector = Element.newPListBox();
        themeSelector.addItem("Default (Catppuccin)", "");
        themeSelector.addItem("Light", "fl-theme-light");
        themeSelector.addItem("Gray (VS Code)", "fl-theme-gray");
        themeSelector.addItem("Nord", "fl-theme-nord");
        themeSelector.addItem("Solarized Dark", "fl-theme-solarized");
        themeSelector.addItem("GitHub Dark", "fl-theme-github");
        themeSelector.addItem("Monokai", "fl-theme-monokai");
        themeSelector.addItem("Corporate Light", "fl-theme-corporate");
        themeSelector.addItem("Deep Orange", "fl-theme-deep-orange");
        themeSelector.addItem("Rounded", "fl-theme-rounded");
        themeSelector.addItem("Underline", "fl-theme-underline");
        themeSelector.setStyleProperty("background", "#313244");
        themeSelector.setStyleProperty("border", "1px solid #89b4fa");
        themeSelector.setStyleProperty("color", "#89b4fa");
        themeSelector.setStyleProperty("border-radius", "4px");
        themeSelector.setStyleProperty("padding", "2px 8px");
        themeSelector.setStyleProperty("font-size", "12px");
        toolbar.add(themeSelector);

        root.add(toolbar);

        // FlexLayout panel
        final String initialModel = "{\"type\":\"row\",\"children\":["
            + "{\"type\":\"tabset\",\"weight\":60,\"children\":["
            + "{\"type\":\"tab\",\"name\":\"Welcome\",\"component\":\"welcome\"}"
            + "]},"
            + "{\"type\":\"tabset\",\"weight\":40,\"children\":["
            + "{\"type\":\"tab\",\"name\":\"Info\",\"component\":\"info\"}"
            + "]}"
            + "]}";

        final FlexLayoutPanel flexLayout = new FlexLayoutPanel(initialModel, null);

        // Toggle sidebar visibility buttons
        final PButton toggleLeft = Element.newPButton("\u258C Left");
        styleSidebarButton(toggleLeft);
        toggleLeft.addClickHandler(e -> {
            flexLayout.getAddon().toggleBorder("left-top");
            flexLayout.getAddon().toggleBorder("left-bottom");
        });
        toolbar.add(toggleLeft);

        final PButton toggleRight = Element.newPButton("Right \u2590");
        styleSidebarButton(toggleRight);
        toggleRight.addClickHandler(e -> {
            flexLayout.getAddon().toggleBorder("right-top");
            flexLayout.getAddon().toggleBorder("right-bottom");
        });
        toolbar.add(toggleRight);

        final PButton toggleBottom = Element.newPButton("\u2581 Bottom");
        styleSidebarButton(toggleBottom);
        toggleBottom.addClickHandler(e -> flexLayout.getAddon().toggleBorder("bottom"));
        toolbar.add(toggleBottom);

        // Theme selector handler
        themeSelector.addChangeHandler(event -> flexLayout.setTheme((String) themeSelector.getSelectedValue()));

        // Shared state for interactive widgets (simulates persistence)
        final java.util.Map<String, int[]> counterStates = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.Map<String, String> inputStates = new java.util.concurrent.ConcurrentHashMap<>();

        // Widget factory: receives (component, config) — config identifies the instance
        flexLayout.setWidgetFactory((component, config) -> {
            switch (component) {
                case "label":
                    final PLabel l = Element.newPLabel("I am a PLabel created by drag!");
                    l.setStyleProperty("padding", "12px");
                    l.setStyleProperty("color", "#a6e3a1");
                    return l;
                case "button":
                    final PButton b = Element.newPButton("I am a PButton!");
                    b.setStyleProperty("margin", "12px");
                    b.addClickHandler(e -> b.setText("Clicked! " + System.currentTimeMillis()));
                    return b;
                case "textbox":
                    final PTextBox t = Element.newPTextBox();
                    t.setPlaceholder("Type here...");
                    t.setStyleProperty("margin", "12px");
                    t.setStyleProperty("width", "200px");
                    return t;
                case "interactive":
                    // Use config as instance ID to preserve state across pop-out/pop-in
                    // When popped out via PWindow, config will be the tabId (set by handlePopOut)
                    final String instanceId = config != null ? config : "inst_" + System.nanoTime();
                    counterStates.putIfAbsent(instanceId, new int[]{0});
                    final int[] count = counterStates.get(instanceId);

                    final PFlowPanel panel = Element.newPFlowPanel();
                    panel.setStyleProperty("padding", "12px");
                    panel.setStyleProperty("display", "flex");
                    panel.setStyleProperty("flex-direction", "column");
                    panel.setStyleProperty("gap", "8px");
                    final PLabel counter = Element.newPLabel("Count: " + count[0]);
                    counter.setStyleProperty("color", "#89b4fa");
                    counter.setStyleProperty("font-size", "16px");
                    counter.addStyleName("interactive-counter");
                    panel.add(counter);
                    final PButton incBtn = Element.newPButton("Increment");
                    incBtn.addStyleName("interactive-inc-btn");
                    incBtn.setStyleProperty("width", "120px");
                    incBtn.addClickHandler(e -> {
                        count[0]++;
                        counter.setText("Count: " + count[0]);
                    });
                    panel.add(incBtn);
                    final PTextBox input = Element.newPTextBox();
                    input.setPlaceholder("Type and check after pop-in...");
                    input.addStyleName("interactive-input");
                    input.setStyleProperty("width", "250px");
                    // Restore previous text
                    final String prevText = inputStates.getOrDefault(instanceId, "");
                    if (!prevText.isEmpty()) input.setText(prevText);
                    // Save text on change
                    input.addKeyUpHandler(event -> inputStates.put(instanceId, input.getText()));
                    panel.add(input);
                    return panel;
                case "welcome":
                    final PLabel w = Element.newPLabel("Welcome to FlexLayout in PonySDK! Drag items from the toolbar.");
                    w.setStyleProperty("padding", "16px");
                    w.setStyleProperty("color", "#cdd6f4");
                    return w;
                case "info":
                    final PLabel i = Element.newPLabel("Resize panels with splitters. Drag tabs to reorganize.");
                    i.setStyleProperty("padding", "16px");
                    i.setStyleProperty("color", "#bac2de");
                    return i;
                default:
                    final PLabel d = Element.newPLabel("Unknown: " + component);
                    d.setStyleProperty("padding", "12px");
                    return d;
            }
        });

        // Register drag sources by CSS class
        flexLayout.registerDragSourceByClass("fl-drag-src-label", "Label", "label");
        flexLayout.registerDragSourceByClass("fl-drag-src-button", "Button", "button");
        flexLayout.registerDragSourceByClass("fl-drag-src-textbox", "TextBox", "textbox");
        flexLayout.registerDragSourceByClass("fl-drag-src-interactive", "Interactive", "interactive");

        // Programmatic add tab button
        addBtn.addClickHandler(e -> {
            final PLabel newLabel = Element.newPLabel("Tab added at " + System.currentTimeMillis());
            newLabel.setStyleProperty("padding", "12px");
            newLabel.setStyleProperty("color", "#f9e2af");
            flexLayout.addTab("Dynamic", newLabel);
        });

        // On tab close
        flexLayout.setOnTabClosed(tabId -> System.out.println("Tab closed: " + tabId));

        // ─── Sidebar demo: pre-populate all 5 zones ─────────────────
        // Activate all borders
        flexLayout.getAddon().toggleBorder("left-top");
        flexLayout.getAddon().toggleBorder("left-bottom");
        flexLayout.getAddon().toggleBorder("right-top");
        flexLayout.getAddon().toggleBorder("right-bottom");
        flexLayout.getAddon().toggleBorder("bottom");

        // Left-top: icon only
        final PLabel explorer = Element.newPLabel("Project files will appear here.");
        explorer.setStyleProperty("padding", "12px");
        explorer.setStyleProperty("color", "#a6e3a1");
        flexLayout.addBorderTab("left-top", "Explorer", explorer, "\uD83D\uDCC1");

        final PLabel search = Element.newPLabel("Search across your project.");
        search.setStyleProperty("padding", "12px");
        search.setStyleProperty("color", "#89b4fa");
        final String searchTabId = flexLayout.addBorderTab("left-top", "Search", search, "\uD83D\uDD0D");

        // Left-bottom: icon only
        final PLabel git = Element.newPLabel("Git branches and commits.");
        git.setStyleProperty("padding", "12px");
        git.setStyleProperty("color", "#f9e2af");
        flexLayout.addBorderTab("left-bottom", "Source Control", git, "\u2387");

        final PLabel extensions = Element.newPLabel("Manage extensions.");
        extensions.setStyleProperty("padding", "12px");
        extensions.setStyleProperty("color", "#cba6f7");
        final String extTabId = flexLayout.addBorderTab("left-bottom", "Extensions", extensions, "\u2B29");

        // Right-top: label only
        flexLayout.getAddon().setBorderTabStyle("right-top", "label");
        final PLabel props = Element.newPLabel("Widget properties and settings.");
        props.setStyleProperty("padding", "12px");
        props.setStyleProperty("color", "#fab387");
        flexLayout.addBorderTab("right-top", "Properties", props);

        final PLabel outline = Element.newPLabel("Document outline / structure.");
        outline.setStyleProperty("padding", "12px");
        outline.setStyleProperty("color", "#94e2d5");
        final String outlineTabId = flexLayout.addBorderTab("right-top", "Outline", outline);

        // Right-bottom: icon + label
        flexLayout.getAddon().setBorderTabStyle("right-bottom", "iconLabel");
        final PLabel notifications = Element.newPLabel("Recent notifications.");
        notifications.setStyleProperty("padding", "12px");
        notifications.setStyleProperty("color", "#f38ba8");
        final String notifTabId = flexLayout.addBorderTab("right-bottom", "Notifications", notifications, "\uD83D\uDD14");

        // Bottom: label (default)
        final PLabel terminal = Element.newPLabel("$ Terminal output here...");
        terminal.setStyleProperty("padding", "12px");
        terminal.setStyleProperty("color", "#a6e3a1");
        terminal.setStyleProperty("font-family", "monospace");
        flexLayout.addBorderTab("bottom", "Terminal", terminal);

        final PLabel problems = Element.newPLabel("0 errors, 2 warnings");
        problems.setStyleProperty("padding", "12px");
        problems.setStyleProperty("color", "#f9e2af");
        final String problemsTabId = flexLayout.addBorderTab("bottom", "Problems", problems);

        final PLabel output = Element.newPLabel("Build output logs.");
        output.setStyleProperty("padding", "12px");
        output.setStyleProperty("color", "#bac2de");
        flexLayout.addBorderTab("bottom", "Output", output);

        // Badges demo: simulate notifications on non-active tabs
        flexLayout.getAddon().setBadge(searchTabId, "", "#00e676");       // green glowing dot
        flexLayout.getAddon().setBadge(extTabId, "2", "#cba6f7");        // purple "2" bubble
        flexLayout.getAddon().setBadge(notifTabId, "5", "#ff6d00");      // orange "5" bubble
        flexLayout.getAddon().setBadge(outlineTabId, "", null);          // default red dot
        flexLayout.getAddon().setBadge(problemsTabId, "12", "#f38ba8");  // pink "12" bubble

        final PWidget layoutContainer = flexLayout.asWidget();
        layoutContainer.setStyleProperty("flex", "1");
        layoutContainer.setStyleProperty("min-height", "0");
        layoutContainer.setStyleProperty("position", "relative");
        root.add(layoutContainer);

        PWindow.getMain().add(root);
    }

    private static void styleDragSource(final PLabel label) {
        label.setStyleProperty("padding", "4px 10px");
        label.setStyleProperty("background", "#313244");
        label.setStyleProperty("border", "1px dashed #89b4fa");
        label.setStyleProperty("color", "#89b4fa");
        label.setStyleProperty("border-radius", "4px");
        label.setStyleProperty("font-size", "12px");
        label.setStyleProperty("cursor", "grab");
        label.setStyleProperty("white-space", "nowrap");
    }

    private static void styleSidebarButton(final PButton btn) {
        btn.setStyleProperty("background", "#313244");
        btn.setStyleProperty("border", "1px solid #cba6f7");
        btn.setStyleProperty("color", "#cba6f7");
        btn.setStyleProperty("border-radius", "4px");
        btn.setStyleProperty("padding", "3px 10px");
        btn.setStyleProperty("cursor", "pointer");
        btn.setStyleProperty("font-size", "12px");
    }
}
