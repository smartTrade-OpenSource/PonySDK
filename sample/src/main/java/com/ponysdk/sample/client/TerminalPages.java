/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.ponysdk.core.model.PFontSize;
import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PJustification;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
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
        Map.entry("collect", TerminalPages::collect));

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
        final PWindow[] child = new PWindow[1];
        button(panel, "Open window", () -> {
            final PWindow window = Element.newPWindow("terminal-child", "width=400,height=300");
            window.addOpenHandler(e -> status.add("opened"));
            window.addCloseHandler(e -> status.add("closed"));
            window.add(Element.newPLabel("Child content"));
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

        panel.add(layout, simple, dock, split);
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

        panel.add(scroll, cells, table);
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
        first.add("Child A");
        final PTreeItem childB = first.add("Child B");
        tree.add("Second");
        tree.add("Third");
        first.setState(true);
        panel.add(tree);
        button(panel, "Rename", () -> first.setText("Renamed"));
        button(panel, "Collapse", () -> first.setState(false));
        button(panel, "Remove B", () -> first.remove(childB));
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
        held[0].add(counter, split, suggest, menu);
        panel.add(held[0]);
        final PLabel status = Element.newPLabel("Held");
        status.setAttribute("aria-label", "Status");
        panel.add(status);
        button(panel, "Collect", () -> {
            if (held[0] != null) panel.remove(held[0]);
            held[0] = null;
            System.gc();
            status.setText("Collected");
        });
        return panel;
    }
}
