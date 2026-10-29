/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.ponysdk.core.model.PAlignment;
import com.ponysdk.core.model.PFontSize;
import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PJustification;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PMenuItem;
import com.ponysdk.core.ui.basic.PGrid;
import com.ponysdk.core.ui.basic.PFunctionalLabel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PDatePicker;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PComposite;
import com.ponysdk.core.ui.basic.PHeaderPanel;
import com.ponysdk.core.ui.basic.PHost;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PDecoratedPopupPanel;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PLayoutPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PMultiWordSuggestOracle;
import com.ponysdk.core.ui.basic.PPopupPanel;
import com.ponysdk.core.ui.basic.PRichTextArea;
import com.ponysdk.core.ui.basic.PRootLayoutPanel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PSuggestBox;
import com.ponysdk.core.ui.basic.PTabPanel;
import com.ponysdk.core.ui.basic.PTextArea;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.basic.event.PContextMenuEvent;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEndHandler;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDragStartHandler;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PDropHandler;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PFocusHandler;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PKeyDownEvent;
import com.ponysdk.core.ui.basic.event.PMouseDownEvent;
import com.ponysdk.core.ui.basic.event.PMouseDownHandler;
import com.ponysdk.core.ui.basic.event.PMouseOutEvent;
import com.ponysdk.core.ui.basic.event.PMouseOutHandler;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpHandler;
import com.ponysdk.core.ui.basic.event.PMouseWhellEvent;
import com.ponysdk.core.ui.basic.PEventOptions;
import com.ponysdk.core.ui.basic.PScript.ExecutionCallback;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.sample.client.page.addon.AskAddOn;
import com.ponysdk.sample.client.page.addon.HighlightAddOn;

import jakarta.json.Json;

/**
 * Pages of the terminal's features that the showcase's pages do not show, opened at {@code #terminal-<name>} and
 * driven by sample/tests/terminal-pages.spec.js. Each page shows what reached the server in a status label.
 */
final class TerminalPages {

    static final String TOKEN = "terminal-";

    private static final Map<String, Supplier<IsPWidget>> PAGES = Map.ofEntries(
        Map.entry("window", TerminalPages::window),
        Map.entry("widget", TerminalPages::widget),
        Map.entry("textbox", TerminalPages::textBox),
        Map.entry("layout", TerminalPages::layout),
        Map.entry("rootlayout", TerminalPages::rootLayout),
        Map.entry("panels", TerminalPages::panels),
        Map.entry("tree", TerminalPages::tree),
        Map.entry("richtext", TerminalPages::richText),
        Map.entry("popup", TerminalPages::popup),
        Map.entry("image", TerminalPages::image),
        Map.entry("misc", TerminalPages::misc),
        Map.entry("collect", TerminalPages::collect),
        Map.entry("generic", TerminalPages::generic),
        Map.entry("webcomponent", TerminalPages::webComponent),
        Map.entry("datebox", TerminalPages::dateBox),
        Map.entry("menu", TerminalPages::menu),
        Map.entry("forms", TerminalPages::forms),
        Map.entry("host", TerminalPages::host));

    private TerminalPages() {
    }

    static void build(final String token) {
        final Supplier<IsPWidget> page = PAGES.get(token.substring(TOKEN.length()));
        PWindow.getMain().add(page != null ? page.get() : Element.newPLabel("No terminal page " + token));
    }

    /** A label that lists, in order, what the server received. */
    private static final class Status {

        private final PLabel label = Element.newPLabel("Status:");
        private final List<String> entries = new ArrayList<>();

        Status() {
            label.setAttribute("aria-label", "Status");
        }

        void add(final String entry) {
            entries.add(entry);
            label.setText("Status: " + String.join(", ", entries));
        }
    }

    private static PButton button(final PFlowPanel panel, final String text, final Runnable action) {
        final PButton button = Element.newPButton(text);
        button.addClickHandler(e -> action.run());
        panel.add(button);
        return button;
    }

    /** A window opened by the server, then moved, sized, titled, printed, focused and closed by it. */
    private static IsPWidget window() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        PWindow.getMain().setTitle("Terminal window page");
        panel.add(Element.newPLabel("Terminal window page")); // twice: in the dictionary, from the main window's blocks
        final PWindow[] child = new PWindow[1];
        button(panel, "Open window", () -> {
            final PWindow window = Element.newPWindow("terminal-child", "width=400,height=300");
            window.addOpenHandler(e -> status.add("opened"));
            window.addCloseHandler(e -> status.add("closed"));
            window.add(Element.newPLabel("Child content"));
            window.add(Element.newPLabel("Terminal window page")); // a string the main window's dictionary has
            window.open();
            child[0] = window;
        });
        button(panel, "Title", () -> child[0].setTitle("Child title"));
        button(panel, "Resize to", () -> child[0].resizeTo(420, 320));
        button(panel, "Resize by", () -> child[0].resizeBy(20, 20));
        button(panel, "Move", () -> {
            child[0].moveTo(10, 10);
            child[0].moveBy(5.5f, 5.5f);
        });
        button(panel, "Focus", () -> child[0].focus());
        button(panel, "Print", () -> child[0].print());
        button(panel, "Close", () -> child[0].close());
        button(panel, "Navigate", () -> child[0].getLocation().replace("about:blank"));
        button(panel, "Open blank", () -> Element.newPWindow(false, "about:blank", "terminal-blank", "width=300,height=200").open());
        button(panel, "Reload page", () -> PWindow.getMain().reload());
        button(panel, "Frame", () -> {
            final com.ponysdk.core.ui.basic.PFrame frame = Element.newPFrame("html/frame-demo.html?demo");
            frame.setAttribute("title", "Query frame");
            panel.add(frame);
        });
        return panel;
    }

    /**
     * A target with the mouse, wheel and context menu handlers, a drag source and a drop zone, the title, styles,
     * prevented and stopped events, a native binding and an AJAX handler.
     */
    private static IsPWidget widget() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);

        final PLabel target = Element.newPLabel("Target");
        target.setAttribute("aria-label", "Target");
        target.addDomHandler((PMouseOverEvent.Handler) e -> status.add("over"), PMouseOverEvent.TYPE);
        target.addDomHandler((PMouseOutHandler) e -> status.add("out"), PMouseOutEvent.TYPE);
        target.addDomHandler((PMouseDownHandler) e -> status.add("down " + e.getNativeButton()), PMouseDownEvent.TYPE);
        target.addDomHandler((PMouseUpHandler) e -> status.add("up"), PMouseUpEvent.TYPE);
        target.addDomHandler((PMouseWhellEvent.Handler) e -> status.add("wheel " + e.getDeltaY()), PMouseWhellEvent.TYPE);
        target.addDomHandler((PContextMenuEvent.Handler) e -> status.add("menu"), PContextMenuEvent.TYPE);
        target.preventEvent(PEventType.ONCONTEXTMENU);
        target.stopEvent(PEventType.ONMOUSEUP);
        panel.add(target);

        final PLabel source = Element.newPLabel("Drag source");
        source.addDomHandler((PDragStartHandler) e -> status.add("drag start"), PDragStartEvent.TYPE);
        source.addDomHandler((PDragEndHandler) e -> status.add("drag end"), PDragEndEvent.TYPE);
        final PLabel zone = Element.newPLabel("Drop zone");
        zone.setHeight("40px");
        zone.addDomHandler((PDragEnterHandler) e -> status.add("drag enter"), PDragEnterEvent.TYPE);
        zone.addDomHandler((PDragLeaveEvent.Handler) e -> status.add("drag leave"), PDragLeaveEvent.TYPE);
        zone.addDomHandler((PDragOverEvent.Handler) e -> status.add("drag over"), PDragOverEvent.TYPE); // not sent
        zone.addDomHandler((PDropHandler) e -> status.add("drop " + ((PLabel) e.getDragSource()).getText()), PDropEvent.TYPE);
        panel.add(source, zone);

        final PTextBox keys = Element.newPTextBox();
        keys.setAttribute("aria-label", "Enter only");
        keys.addKeyDownHandler(new PKeyDownEvent.Handler() {

            @Override
            public void onKeyDown(final PKeyDownEvent event) {
                status.add("key " + event.getKeyCode());
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] { PKeyCodes.ENTER };
            }
        });
        keys.addDomHandler((PFocusHandler) e -> status.add("focus"), PFocusEvent.TYPE);
        keys.addDomHandler((PBlurHandler) e -> status.add("blur"), PBlurEvent.TYPE);
        panel.add(keys);

        final PLabel styled = Element.newPLabel("Styled");
        styled.addStyleName("first");
        styled.addStyleName("second");
        panel.add(styled);
        final PLabel bound = Element.newPLabel("Native: none");
        bound.bindTerminalFunction("terminalBinding");
        panel.add(bound);
        final PLabel ajax = Element.newPLabel("Ajax");
        ajax.forceDomId();
        ajax.setAttribute("aria-label", "Ajax");
        ajax.setAjaxHandler((request, response) -> {
            response.setContentType("text/plain");
            response.getOutputStream().write(("answer to " + request.getHeader("question")).getBytes(StandardCharsets.UTF_8));
        });
        ajax.setTerminalHandler(e -> status.add("native " + e.getData().getString("word", "")));
        panel.add(ajax);
        final PLabel watched = Element.newPLabel("Watched");
        final com.ponysdk.core.ui.basic.event.PVisibilityEvent.PVisibilityHandler visibility = e -> status.add("visible");
        watched.addVisibilityHandler(visibility);
        panel.add(watched);
        final HandlerRegistration[] clicks = new HandlerRegistration[1];
        clicks[0] = watched.addEventListener("click", PEventOptions.fields("type"), e -> status.add("listened"));

        button(panel, "Title", () -> target.setTitle("A tip"));
        button(panel, "Untitle", () -> target.setTitle(""));
        button(panel, "Unstyle", () -> styled.removeStyleName("first"));
        button(panel, "Full size", styled::setSizeFull);
        button(panel, "Focus box", keys::focus);
        button(panel, "Blur box", keys::blur);
        button(panel, "Focus quietly", keys::focusPreventScroll);
        button(panel, "Native", () -> bound.sendToNative(Json.createObjectBuilder().add("text", "hello").build()));
        button(panel, "Unwatch", () -> {
            watched.removeVisibilityHandler(visibility);
            clicks[0].removeHandler();
        });
        final PLabel unbound = Element.newPLabel("Unbound");
        final PElement plain = Element.newDiv();
        plain.setInnerText("Plain element");
        panel.add(unbound, plain);
        button(panel, "Bind nowhere", () -> unbound.bindTerminalFunction("terminalNowhere"));
        button(panel, "Native unbound", () -> unbound.sendToNative(Json.createObjectBuilder().add("text", "lost").build()));
        button(panel, "Restyle element", () -> plain.setStyleName("restyled"));
        button(panel, "Listen with detail", () -> clicks[0] = watched.addEventListener("click",
            PEventOptions.fields("type", "detail"), e -> status.add("detail " + e.getFields().getInt("detail", -1))));
        final PLabel locked = Element.newPLabel("Locked");
        final int[] lockedClicks = { 0 };
        locked.addEventListener("click", PEventOptions.NONE.lock(), e -> status.add("locked " + ++lockedClicks[0]));
        panel.add(locked);
        return panel;
    }

    /** A paste handler, a filter, a selection, sizes and lengths. */
    private static IsPWidget textBox() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        final PTextBox box = Element.newPTextBox();
        box.setAttribute("aria-label", "Pasted box");
        final PPasteHandlerImpl paste = new PPasteHandlerImpl(status, box);
        box.addPasteHandler(paste);
        box.setVisibleLength(12);
        box.setMaxLength(8);
        final PTextBox digits = Element.newPTextBox();
        digits.setAttribute("aria-label", "Digits");
        digits.setFilter("[0-9]");
        digits.setFilter("[0-9.]");
        final PTextArea area = Element.newPTextArea();
        area.setAttribute("aria-label", "Area");
        area.setVisibleLines(4);
        area.setCharacterWidth(30);
        panel.add(box, digits, area);
        button(panel, "Select", () -> {
            box.setText("abcdef");
            box.select(1, 3);
        });
        button(panel, "Stop paste", () -> box.removePasteHandler(paste));
        button(panel, "Paste again", () -> box.addPasteHandler(paste));
        return panel;
    }

    private static final class PPasteHandlerImpl implements com.ponysdk.core.ui.basic.event.PPasteEvent.PPasteHandler {

        private final Status status;
        private final PTextBox box;

        PPasteHandlerImpl(final Status status, final PTextBox box) {
            this.status = status;
            this.box = box;
        }

        @Override
        public void onPaste(final com.ponysdk.core.ui.basic.event.PPasteEvent event) {
            status.add("paste " + event.getData());
        }
    }

    /** A layout panel, a simple layout panel, a dock with an east and a line end, a split with a toggled splitter. */
    private static IsPWidget layout() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);

        final PLayoutPanel layout = Element.newPLayoutPanel();
        layout.setAttribute("aria-label", "Layout");
        layout.setWidth("300px");
        layout.setHeight("60px");
        final PLabel kept = Element.newPLabel("Kept");
        final PLabel layered = Element.newPLabel("Layered");
        layout.add(kept);
        layout.add(layered);
        layout.setWidgetLeftWidth(layered, 150, 100, PUnit.PX);

        final PSimpleLayoutPanel simple = Element.newPSimpleLayoutPanel();
        simple.setAttribute("aria-label", "Simple");
        simple.setWidth("200px");
        simple.setHeight("30px");
        simple.setWidget(Element.newPLabel("First"));
        simple.setWidget(Element.newPLabel("Second"));

        final PDockLayoutPanel dock = Element.newPDockLayoutPanel(PUnit.PX);
        dock.setAttribute("aria-label", "Dock");
        dock.setWidth("300px");
        dock.setHeight("60px");
        final PLabel east = Element.newPLabel("East");
        dock.addEast(east, 60);
        dock.addLineEnd(Element.newPLabel("End"), 40);
        dock.add(Element.newPLabel("Middle"));

        final PSplitLayoutPanel split = Element.newPSplitLayoutPanel();
        split.setAttribute("aria-label", "Split");
        split.setWidth("50%");
        split.setHeight("80px");
        final PLabel west = Element.newPLabel("Split west");
        split.addWest(west, 100);
        split.add(Element.newPLabel("Split center"));
        split.setWidgetSnapClosedSize(west, 30);
        split.setWidgetToggleDisplayAllowed(west, true);
        final com.ponysdk.core.ui.basic.event.PLayoutResizeEvent.Handler resized = e -> status.add("resized "
                + e.getLayoutResizeData().stream().map(d -> String.valueOf((int) d.getSize())).reduce((a, b) -> a + "/" + b).orElse(""));
        split.addLayoutResizeHandler(resized);

        final PLayoutPanel aligned = Element.newPLayoutPanel();
        aligned.setAttribute("aria-label", "Aligned");
        aligned.setWidth("200px");
        aligned.setHeight("60px");
        final PLabel top = Element.newPLabel("Top");
        final PLabel bottom = Element.newPLabel("Bottom");
        aligned.add(top);
        aligned.add(bottom);

        final PSplitLayoutPanel vertical = Element.newPSplitLayoutPanel();
        vertical.setAttribute("aria-label", "Rows");
        vertical.setWidth("300px");
        vertical.setHeight("200px");
        vertical.addNorth(Element.newPLabel("North"), 50);
        vertical.addSouth(Element.newPLabel("South"), 50);
        vertical.addLineStart(Element.newPLabel("Start"), 60);
        vertical.add(Element.newPLabel("Vertical center"));

        final PHeaderPanel header = Element.newPHeaderPanel();
        header.setAttribute("aria-label", "Header panel");
        header.setHeaderWidget(Element.newPLabel("Header"));
        header.setContentWidget(Element.newPLabel("Content"));
        header.setFooterWidget(Element.newPLabel("Footer"));

        panel.add(layout, simple, dock, split, aligned, vertical, header);
        button(panel, "Animate", () -> {
            aligned.setWidgetTopHeight(top, 20, 20, PUnit.PX);
            aligned.animate(Duration.ofMillis(200));
        });
        button(panel, "Align", () -> {
            aligned.setWidgetVerticalPosition(top, PAlignment.END);
            aligned.setWidgetVerticalPosition(bottom, PAlignment.BEGIN);
        });
        button(panel, "Remove split west", () -> split.remove(west));
        button(panel, "Resize header", header::resize);
        button(panel, "Hide layered", () -> layout.setWidgetHidden(layered, true));
        button(panel, "Remove layered", () -> layout.remove(layered));
        button(panel, "Hide east", () -> dock.setWidgetHidden(east, true));
        button(panel, "Remove east", () -> dock.remove(east));
        button(panel, "Stop resize reports", () -> split.removeLayoutResizeHandler(resized));
        return panel;
    }

    /** A root layout panel: the page's whole window. */
    private static IsPWidget rootLayout() {
        final PRootLayoutPanel root = PRootLayoutPanel.get(PWindow.getMain());
        root.add(Element.newPLabel("In the root layout"));
        return Element.newPLabel("Beside the root layout");
    }

    /** A scroll panel scrolled by the server, a cell panel's cells, a flex table's rows, cells and columns. */
    private static IsPWidget panels() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PScrollPanel scroll = Element.newPScrollPanel();
        scroll.setAttribute("aria-label", "Scrolled");
        scroll.setWidth("100px");
        scroll.setHeight("50px");
        final PLabel big = Element.newPLabel("Big");
        big.setWidth("400px");
        big.setHeight("400px");
        scroll.setWidget(big);

        final PHorizontalPanel cells = Element.newPHorizontalPanel();
        cells.setAttribute("aria-label", "Cells");
        final PLabel one = Element.newPLabel("One");
        final PLabel three = Element.newPLabel("Three");
        cells.add(one, three);
        cells.insert(Element.newPLabel("Two"), 1);
        cells.setCellHorizontalAlignment(one, PHorizontalAlignment.ALIGN_RIGHT);
        cells.setCellVerticalAlignment(one, PVerticalAlignment.ALIGN_BOTTOM);
        cells.setCellWidth(three, "80px");
        cells.setSpacing(4);
        cells.setBorderWidth(2);

        final PFlexTable table = Element.newPFlexTable();
        table.setAttribute("aria-label", "Table");
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 2; column++) table.setWidget(row, column, Element.newPLabel(row + "." + column));
        }
        table.setBorderWidth(1);
        table.setCellSpacing(3);
        table.getRowFormatter().setStyleName(0, "head");
        table.getRowFormatter().addStyleName(1, "odd");
        table.getRowFormatter().addStyleName(1, "extra");
        table.getRowFormatter().removeStyleName(1, "extra");
        table.getRowFormatter().hideRow(2);
        table.getCellFormatter().setStyleName(0, 0, "corner");
        table.getCellFormatter().addStyleName(0, 1, "a");
        table.getCellFormatter().addStyleName(0, 1, "b");
        table.getCellFormatter().removeStyleName(0, 1, "a");
        table.getCellFormatter().setVerticalAlignment(1, 0, PVerticalAlignment.ALIGN_TOP);
        table.getCellFormatter().setRowSpan(0, 0, 2);
        table.getColumnFormatter().setStyleName(1, "second");
        table.getColumnFormatter().addStyleName(0, "first");
        table.getColumnFormatter().addStyleName(0, "wide");
        table.getColumnFormatter().removeStyleName(0, "wide");
        table.getColumnFormatter().setWidth(0, "60px");

        final PLabel wrapped = Element.newPLabel("Wrapped by a composite");
        final PComposite<PLabel> composite = new PComposite<>() {
            {
                initWidget(wrapped);
            }

            @Override
            public String dumpDOM() {
                return wrapped.dumpDOM();
            }
        };
        final PVerticalPanel column = Element.newPVerticalPanel();
        column.setAttribute("aria-label", "Column");
        panel.add(scroll, cells, table, composite, column);
        button(panel, "Style composite", () -> composite.addStyleName("composed"));
        button(panel, "Add centered", () -> {
            column.setHorizontalAlignment(PHorizontalAlignment.ALIGN_CENTER);
            column.add(Element.newPLabel("Centered"));
        });
        button(panel, "No borders", () -> {
            cells.setBorderWidth(0);
            table.setBorderWidth(0);
        });
        button(panel, "Row 5", () -> table.insertRow(5));
        button(panel, "Clear table", table::clear);
        button(panel, "Scroll to 30", () -> {
            scroll.setHorizontalScrollPosition(30);
            scroll.setVerticalScrollPosition(30);
        });
        button(panel, "Scroll to bottom", scroll::scrollToBottom);
        button(panel, "Scroll to right", scroll::scrollToRight);
        button(panel, "Scroll to top", () -> {
            scroll.scrollToTop();
            scroll.scrollToLeft();
        });
        button(panel, "Show row", () -> table.getRowFormatter().showRow(2));
        return panel;
    }

    /** Items renamed, closed, removed and cleared by the server. */
    private static IsPWidget tree() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PTree tree = Element.newPTree();
        tree.setAnimationEnabled(true);
        final PTreeItem first = tree.add("First");
        final PTreeItem childA = first.add("Child A");
        final PTreeItem childB = first.add("Child B");
        tree.add("Second");
        tree.add("Third");
        first.setState(true);
        panel.add(tree);
        button(panel, "Rename", () -> first.setText("Renamed"));
        button(panel, "Collapse", () -> first.setState(false));
        button(panel, "Remove B", () -> first.remove(childB));
        button(panel, "Insert first", () -> first.add(0, Element.newPTreeItem("Child zero")));
        button(panel, "Remove children", () -> {
            first.remove(childA);
            first.remove(first.get(0));
        });
        final PTextBox field = Element.newPTextBox();
        field.setAttribute("aria-label", "Item field");
        button(panel, "Field item", () -> tree.add(Element.newPTreeItem(field)));
        button(panel, "Move field", () -> tree.add(Element.newPTreeItem(field)));
        button(panel, "Add opened", () -> {
            final PTreeItem opened = Element.newPTreeItem("Opened");
            opened.add("Inner");
            opened.setState(true);
            tree.add(opened);
        });
        button(panel, "Unselect", () -> tree.setSelectedItem(null));
        button(panel, "Clear", tree::clear);
        return panel;
    }

    /** The formatter's commands, sent by the server. */
    private static IsPWidget richText() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PRichTextArea area = Element.newPRichTextArea();
        area.setAttribute("aria-label", "Rich text");
        area.setHTML("Some text");
        panel.add(area, Element.newPRichTextToolbar(area));
        final PRichTextArea.Formatter formatter = area.getFormatter();
        button(panel, "Server select all", formatter::selectAll);
        button(panel, "Server bold", formatter::toggleBold);
        button(panel, "Server large", () -> formatter.setFontSize(PFontSize.X_LARGE));
        button(panel, "Server center", () -> formatter.setJustification(PJustification.CENTER));
        button(panel, "Server red", () -> formatter.setForeColor("red"));
        button(panel, "Plain text", () -> area.setText("Plain"));
        button(panel, "Disable", () -> area.setEnabled(false));
        button(panel, "Enable", () -> area.setEnabled(true));
        return panel;
    }

    /** A decorated popup that hides on a press outside, with a glass; a dialog that cannot be dragged. */
    private static IsPWidget popup() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        button(panel, "Show decorated", () -> {
            final PDecoratedPopupPanel popup = Element.newPDecoratedPopupPanel(true);
            popup.setAnimationEnabled(true);
            popup.setGlassEnabled(true);
            popup.setGlassStyleName("terminal-glass");
            popup.setWidget(Element.newPLabel("Decorated"));
            popup.addCloseHandler(e -> status.add("decorated closed"));
            PWindow.getMain().add(popup);
            popup.setPopupPosition(400, 100);
            popup.show();
        });
        button(panel, "Show fixed dialog", () -> {
            final PDialogBox dialog = Element.newPDialogBox(true);
            dialog.setCaption("Fixed dialog");
            dialog.setDraggable(false);
            dialog.setWidget(Element.newPLabel("Not draggable"));
            dialog.addCloseHandler(e -> status.add("dialog closed"));
            PWindow.getMain().add(dialog);
            dialog.setPopupPosition(400, 300);
            dialog.show();
        });
        button(panel, "Show modal", () -> {
            final PPopupPanel popup = Element.newPPopupPanel(true);
            popup.setModal(true);
            popup.setWidget(Element.newPLabel("Modal popup"));
            popup.addCloseHandler(e -> status.add("modal closed"));
            PWindow.getMain().add(popup);
            popup.setPopupPosition(100, 100);
            popup.show();
        });
        button(panel, "Show draggable popup", () -> {
            final PPopupPanel popup = Element.newPPopupPanel(false);
            popup.setDraggable(true);
            popup.setWidget(Element.newPLabel("Draggable popup"));
            PWindow.getMain().add(popup);
            popup.setPopupPosition(100, 300);
            popup.show();
        });
        return panel;
    }

    /** A clipped image, an image whose URL changes, an image the server streams, a file it sends. */
    private static IsPWidget image() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PImage clipped = Element.newPImage("images/pony.png", 4, 6, 20, 30);
        clipped.setAttribute("alt", "Clipped");
        final PImage changed = Element.newPImage("images/pony.png");
        changed.setAttribute("alt", "Changed");
        final PImage streamed = Element.newPImage();
        streamed.setAttribute("alt", "Streamed");
        streamed.setStream((request, response, context) -> {
            try {
                response.setContentType("image/svg+xml");
                response.getOutputStream().write("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'/>"
                    .getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
        panel.add(clipped, changed, streamed);
        button(panel, "Change URL", () -> changed.setUrl("images/pony.png?changed"));
        button(panel, "Download", () -> UIContext.get().stackStreamRequest((request, response, context) -> {
            try {
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=\"terminal.csv\"");
                response.getOutputStream().write("a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }));
        return panel;
    }

    /** Smaller features of many widgets, each behind a button. */
    private static IsPWidget misc() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        final UIContext context = UIContext.get();

        final PLabel linked = Element.newPLabel("Linked");
        linked.setAttributeLinkedToValue("data-value");
        final PHTML wrapped = Element.newPHTML("<i>Wrapped</i>", true);
        final PListBox list = Element.newPListBox();
        list.setAttribute("aria-label", "List");
        list.addItem("Alpha");
        list.addItem("Beta");
        final PCheckBox check = Element.newPCheckBox("Check");
        final PDisclosurePanel disclosure = Element.newPDisclosurePanel("Details");
        disclosure.setContent(Element.newPLabel("Disclosed"));
        disclosure.setAnimationEnabled(true);
        disclosure.setOpen(true);
        final PStackLayoutPanel stack = Element.newPStackLayoutPanel(PUnit.PX);
        stack.setAttribute("aria-label", "Stack");
        stack.setWidth("200px");
        stack.setHeight("150px");
        final PLabel stackFirst = Element.newPLabel("Stack one");
        stack.add(stackFirst, "One", false, 25);
        final PLabel stackSecond = Element.newPLabel("Stack two");
        stack.add(stackSecond, "Two", false, 25);
        stack.showWidget(stackSecond);
        final PTabPanel tabs = Element.newPTabPanel();
        tabs.setAttribute("aria-label", "Tabs");
        final PLabel tabFirst = Element.newPLabel("Tab one");
        tabs.add(tabFirst, "One");
        tabs.add(Element.newPLabel("Tab two"), "Two");
        tabs.selectTab(1);
        final PFileUpload upload = Element.newPFileUpload();
        upload.setLabel("File");
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.addAll(List.of("Red apple", "Red cherry", "Green apple"));
        final PSuggestBox suggest = Element.newPSuggestBox(oracle);
        suggest.getTextBox().setAttribute("aria-label", "Suggest");
        final PMenuBar menu = Element.newPMenuBar();
        menu.setAnimationEnabled(true);
        menu.addItem("Menu item", () -> status.add("menu"));
        final PButton slow = Element.newPButton("Slow");
        slow.showLoadingOnRequest(true);
        slow.addClickHandler(e -> status.add("slow"));
        panel.add(linked, wrapped, list, check, disclosure, stack, tabs, upload, suggest, menu, slow);

        final ExecutionCallback callback = new ExecutionCallback() {

            @Override
            public void onSuccess(final String result) {
                status.add("script " + result);
            }

            @Override
            public void onFailure(final String message) {
                status.add("script failed");
            }
        };
        button(panel, "Later", () -> PScript.call(PWindow.getMain(), "oracleProduct", new Object[] { 3, 4 }, callback,
            Duration.ofMillis(50)));
        button(panel, "Fail", () -> PScript.call(PWindow.getMain(), "terminalMissing", null, callback, null));
        button(panel, "Bake", () -> {
            context.getCookies().setCookie("terminal", "baked", "/");
            PScript.call(PWindow.getMain(), "oracleCookies", null, callback, null);
        });
        button(panel, "Eat", () -> {
            context.getCookies().removeCookie("terminal", PCookies.CookieOptions.path("/"));
            PScript.call(PWindow.getMain(), "oracleCookies", null, callback, null);
        });
        button(panel, "Bake options", () -> context.getCookies().setCookie("options", "v",
            PCookies.CookieOptions.expires(Instant.parse("2100-01-01T00:00:00Z")).withDomain("pony.example").withPath("/")
                .secure().sameSite(PCookies.SameSite.LAX)));
        button(panel, "Eat options", () -> context.getCookies().removeCookie("options",
            PCookies.CookieOptions.domain("pony.example").withPath("/")));
        button(panel, "Read odd", () -> status.add("odd " + context.getCookies().getCookie("odd")));
        final PAnchor anchor = Element.newPAnchor("Anchor", "#terminal-misc");
        panel.add(anchor);
        button(panel, "Unlink", () -> anchor.setHref(null));
        button(panel, "Change", () -> {
            linked.setText("Changed");
            wrapped.setWordWrap(false);
            list.setItemText(1, "Bravo");
            list.setVisibleItemCount(3);
            check.setHTML("<b>Bold check</b>");
            check.setInputAttribute("data-input", "yes");
            disclosure.setOpen(false);
            stack.remove(stackFirst);
            tabs.remove(tabFirst);
            upload.setEnabled(false);
            upload.reset();
            oracle.clear();
            oracle.add("Blue");
            suggest.setLimit(1);
        });
        button(panel, "Empty", () -> {
            linked.setText("");
            menu.clearItems();
        });
        button(panel, "Zone", () -> PWindow.getMain().getPRootPanel("loading").add(Element.newPLabel("In the zone")));
        button(panel, "No zone", () -> PWindow.getMain().getPRootPanel("nowhere").add(Element.newPLabel("Nowhere")));
        button(panel, "End session", context::close);
        return panel;
    }

    /**
     * Widgets removed from the page, then collected by the server: only then does the terminal release them. The
     * button asks the JVM to collect at once, which a page would never do.
     */
    private static IsPWidget collect() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PFlowPanel[] held = { Element.newPFlowPanel() };
        final PPopupPanel popup = Element.newPPopupPanel(false);
        popup.setWidget(Element.newPLabel("Collected popup"));
        held[0].add(popup);
        popup.setPopupPosition(300, 50);
        popup.show();
        final PWebComponent counter = new PWebComponent("my-counter");
        counter.addEventListener("count-changed", detail -> {
        });
        final PSplitLayoutPanel split = Element.newPSplitLayoutPanel();
        split.addWest(Element.newPLabel("W"), 50);
        split.addLayoutResizeHandler(e -> {
        });
        final PSuggestBox suggest = Element.newPSuggestBox(new PMultiWordSuggestOracle());
        final PMenuBar menu = Element.newPMenuBar();
        menu.addItem("Collected menu", () -> {
        });
        final PDateBox date = Element.newPDateBox();
        final PLabel watched = Element.newPLabel("Collected watcher");
        watched.addVisibilityHandler(e -> {
        });
        watched.addEventListener("click", PEventOptions.fields("type"), e -> {
        });
        held[0].add(counter, split, suggest, menu, date, watched);
        panel.add(held[0]);
        final PLabel status = Element.newPLabel("Held");
        status.setAttribute("aria-label", "Status");
        panel.add(status);
        final WeakReference<PWebComponent> collected = new WeakReference<>(counter);
        final int[] clicks = { 0 };
        button(panel, "Collect", () -> {
            if (held[0] != null) panel.remove(held[0]);
            held[0] = null;
            System.gc();
            status.setText((collected.get() == null ? "Collected " : "Still held ") + ++clicks[0]);
        });
        return panel;
    }

    /**
     * A widget's own properties and handlers on widgets of every type: a style name, a DOM handler, a visibility
     * handler added then removed, a function of the page called on its element; children inserted and removed.
     */
    private static IsPWidget generic() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);

        final PFileUpload upload = Element.newPFileUpload();
        upload.setAttribute("aria-label", "Upload");
        upload.setTabindex(3);
        upload.addDomHandler((PFocusHandler) e -> status.add("upload focus"), PFocusEvent.TYPE);
        final PCheckBox check = Element.newPCheckBox("Generic check");
        check.addFocusHandler(e -> status.add("check focus"));
        check.addBlurHandler(e -> status.add("check blur"));
        final PMenuBar menu = Element.newPMenuBar();
        final PMenuItem item = Element.newPMenuItem("Generic item", () -> status.add("command"));
        menu.addItem(item);
        item.addDomHandler((PMouseOverEvent.Handler) e -> status.add("item over"), PMouseOverEvent.TYPE);
        final PSuggestBox suggest = Element.newPSuggestBox(new PMultiWordSuggestOracle());
        suggest.getTextBox().setAttribute("aria-label", "Generic suggest");
        suggest.addDomHandler((PMouseOverEvent.Handler) e -> status.add("suggest over"), PMouseOverEvent.TYPE);
        final PTree tree = Element.newPTree();
        final PTreeItem treeItem = tree.add("Generic tree item");
        final PFunctionalLabel functional = Element.newPFunctionalLabel(TextFunction.named(args -> args[0] + " ponies", "ponies"), 2);
        final PStackLayoutPanel stack = Element.newPStackLayoutPanel(PUnit.PX);
        stack.setAttribute("aria-label", "Generic stack");
        stack.setWidth("200px");
        stack.setHeight("80px");
        stack.add(Element.newPLabel("Stacked"), "Stack", false, 25);
        stack.setAnimationDuration(Duration.ofMillis(10));
        stack.addDomHandler((PMouseOverEvent.Handler) e -> status.add("stack over"), PMouseOverEvent.TYPE);
        final PTabPanel tabs = Element.newPTabPanel();
        tabs.setAttribute("aria-label", "Generic tabs");
        tabs.add(Element.newPLabel("Tabbed"), "Tab");
        tabs.addDomHandler((PMouseOverEvent.Handler) e -> status.add("tabs over"), PMouseOverEvent.TYPE);
        final PSplitLayoutPanel split = Element.newPSplitLayoutPanel();
        split.setAttribute("aria-label", "Generic split");
        split.setWidth("200px");
        split.setHeight("40px");
        split.addWest(Element.newPLabel("West"), 50);
        final PScrollPanel scroll = Element.newPScrollPanel();
        scroll.setAttribute("aria-label", "Generic scroll");
        scroll.setWidth("100px");
        scroll.setHeight("40px");
        final PLabel wide = Element.newPLabel("Wide content to scroll");
        wide.setWidth("400px");
        scroll.setWidget(wide);
        scroll.addScrollHandler(e -> status.add("scrolled"));
        final PTextBox keys = Element.newPTextBox();
        keys.setAttribute("aria-label", "Pressed");
        keys.addKeyPressHandler(e -> status.add("press " + e.getKeyCode()));
        final PLabel marked = Element.newPLabel("Marked");
        final PVerticalPanel cells = Element.newPVerticalPanel();
        cells.setAttribute("aria-label", "Generic cells");
        final PLabel last = Element.newPLabel("Last cell");
        cells.add(Element.newPLabel("First cell"), last);
        final PElement list = Element.newUl();
        list.setAttribute("aria-label", "Generic list");
        final PElement lastEntry = Element.newLi();
        lastEntry.setInnerText("Last entry");
        list.add(lastEntry);
        final PGrid grid = Element.newPGrid();
        grid.setAttribute("aria-label", "Generic grid");
        final PPopupPanel popup = Element.newPPopupPanel(false);
        popup.setWidget(Element.newPLabel("Generic popup"));
        popup.addDomHandler((PMouseOverEvent.Handler) e -> status.add("popup over"), PMouseOverEvent.TYPE);

        final List<PWidget> styled = List.of(upload, menu, item, suggest, functional, stack, tabs, split, scroll,
            cells, list, grid, popup);
        for (final PWidget widget : styled) widget.addStyleName("generic");
        final PVisibilityEvent.PVisibilityHandler visibility = e -> {
        };
        final List<PWidget> watched = List.of(item, keys, split, scroll);
        for (final PWidget widget : watched) widget.addVisibilityHandler(visibility);
        panel.add(upload, check, menu, suggest, tree, functional, stack, tabs, split, scroll, keys, marked, cells, list, grid);

        button(panel, "Unwatch", () -> {
            for (final PWidget widget : watched) widget.removeVisibilityHandler(visibility);
            status.add("unwatched");
        });
        button(panel, "Animate", () -> {
            stack.animate(Duration.ofMillis(10));
            status.add("animated");
        });
        button(panel, "Mark", () -> {
            marked.callTerminalFunction("terminalMark", "yes", 2);
            treeItem.callTerminalFunction("terminalMark");
        });
        button(panel, "Insert", () -> {
            cells.insert(Element.newPLabel("Inserted cell"), 1);
            final PElement entry = Element.newLi();
            entry.setInnerText("Inserted entry");
            list.insert(entry, 0);
        });
        button(panel, "Remove", () -> {
            cells.remove(last);
            list.remove(lastEntry);
        });
        button(panel, "Show popup", () -> {
            panel.add(popup);
            popup.setPopupPosition(300, 50);
            popup.show();
        });
        return panel;
    }

    /** A web component's children at an index and at the end, a property set as text and removed, a method, events. */
    private static IsPWidget webComponent() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        final PWebComponent component = new PWebComponent("pony-test-element");
        component.setAttribute("aria-label", "Component");
        component.add(Element.newPLabel("Second"));
        component.property("note").set("plain text");
        final HandlerRegistration[] listened = new HandlerRegistration[1];
        listened[0] = component.addEventListener("ping", detail -> status.add("ping " + detail));
        panel.add(component);
        button(panel, "Insert first", () -> component.insert(Element.newPLabel("First"), 0));
        button(panel, "Append", () -> component.add(Element.newPLabel("Third")));
        button(panel, "Remove note", () -> component.property("note").remove());
        button(panel, "Call", () -> component.call("setAttribute", "data-called", "yes"));
        button(panel, "Unlisten", () -> {
            listened[0].removeHandler();
            status.add("unlistened");
        });
        return panel;
    }

    /** A date box near the bottom of the window, whose format the server changes, keeping the time of its day; styled days. */
    private static IsPWidget dateBox() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        final PDatePicker picker = Element.newPDatePicker();
        final PDateBox box = Element.newPDateBox(picker, new SimpleDateFormat("yyyy-MM-dd HH:mm"), true);
        box.setAttribute("aria-label", "Date");
        box.addValueChangeHandler(e -> status.add("value " + box.getDisplayedValue()));
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2024, Calendar.MAY, 15, 9, 30);
        box.setValue(calendar.getTime());
        final List<Date> styled = List.of(calendar.getTime());
        picker.addStyleToDates("marked", styled);
        final PFlowPanel spacer = Element.newPFlowPanel();
        spacer.setHeight("600px");
        panel.add(spacer, box);
        button(panel, "Unmark", () -> picker.removeStyleFromDates("marked", styled));
        button(panel, "Day first", () -> box.setDateFormat(new SimpleDateFormat("dd/MM/yyyy HH:mm")));
        button(panel, "Clear date", () -> box.setValue(null));
        button(panel, "Remove box", () -> panel.remove(box));
        return panel;
    }

    /** A bar with a submenu that has its own, an item without a command, a bar removed while a submenu shows. */
    private static IsPWidget menu() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        final PMenuBar bar = Element.newPMenuBar();
        final PMenuBar file = Element.newPMenuBar(true);
        file.addItem("New", () -> status.add("new"));
        final PMenuBar recent = Element.newPMenuBar(true);
        recent.addItem("Recent one", () -> status.add("recent"));
        file.addItem("Recent", recent);
        file.addItem("Plain");
        bar.addItem("File", file);
        bar.addItem("Edit", () -> status.add("edit"));
        bar.addItem("Help", () -> status.add("help"));
        panel.add(bar);
        button(panel, "Remove menu", () -> panel.remove(bar));
        return panel;
    }

    /**
     * In a {@code <pony-app>} element: an event of the host listened to, then no longer; an event with a number as its
     * detail; a window titled before it is ready; the application reloaded, a submenu and a date picker showing.
     */
    private static IsPWidget host() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        final PHost host = PHost.get();
        if (host == null) {
            status.add("not embedded");
            return panel;
        }
        final PMenuBar menu = Element.newPMenuBar();
        final PMenuBar submenu = Element.newPMenuBar(true);
        submenu.addItem("Host sub item", () -> status.add("sub"));
        menu.addItem("Host menu", submenu);
        final PDateBox date = Element.newPDateBox();
        date.setAttribute("aria-label", "Host date");
        panel.add(menu, date);
        final HandlerRegistration ping = host.addEventListener("ping", detail -> status.add("ping " + detail));
        button(panel, "Unlisten ping", () -> {
            ping.removeHandler();
            status.add("unlistened");
        });
        button(panel, "Send number", () -> host.dispatchEvent("number", Json.createValue(42)));
        button(panel, "Call on host", () -> host.callTerminalFunction("onHost")); // no function: the terminal skips it
        final PLabel asked = Element.newPLabel("Asked");
        panel.add(asked);
        button(panel, "Ask", () -> new AskAddOn(asked, "life"));
        button(panel, "Reload", () -> PWindow.getMain().reload());
        button(panel, "Open titled", () -> {
            final PWindow window = Element.newPWindow("host-child", "width=300,height=200");
            window.add(Element.newPLabel("Host child content"));
            window.open();
            window.setTitle("Titled at once"); // before the window is ready
        });
        return panel;
    }

    /**
     * A tab whose tab is a field, a list of several choices, a file input, a masked field set by the server, addons on
     * widgets that show already.
     */
    private static IsPWidget forms() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final Status status = new Status();
        panel.add(status.label);
        final PTabPanel tabs = Element.newPTabPanel();
        tabs.setAttribute("aria-label", "Form tabs");
        tabs.add(Element.newPLabel("First panel"), "First");
        tabs.add(Element.newPLabel("Second panel"), "Second");
        final PTextBox tabField = Element.newPTextBox();
        tabField.setAttribute("aria-label", "Tab field");
        tabs.add(Element.newPLabel("Third panel"), tabField);
        final PListBox list = Element.newPListBox();
        list.setAttribute("aria-label", "Choices");
        list.setMultipleSelect(true);
        list.addItem("One");
        list.addItem("Two");
        list.addChangeHandler(e -> status.add("choices " + list.getSelectedIndexes()));
        final PFileUpload upload = Element.newPFileUpload();
        upload.setAttribute("aria-label", "Form file");
        upload.addChangeHandler(e -> status.add("file " + upload.getFileName()));
        final PTextBox masked = Element.newPTextBox();
        masked.setAttribute("aria-label", "Masked");
        masked.applyMask("{{00}}/{{00}}", true, "_");
        final PLabel highlighted = Element.newPLabel("Highlighted");
        final PLabel unhighlighted = Element.newPLabel("Unhighlighted");
        panel.add(tabs, list, upload, masked, highlighted, unhighlighted);
        // addons on widgets that show already
        button(panel, "Highlight", () -> new HighlightAddOn(highlighted, "rgb(1, 2, 3)"));
        button(panel, "Highlight in no color", () -> new HighlightAddOn(unhighlighted, ""));
        button(panel, "Deselect", () -> list.setSelectedIndex(-1));
        button(panel, "Set masked", () -> masked.setText("12x4"));
        return panel;
    }

}
