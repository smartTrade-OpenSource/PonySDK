/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.PAlignment;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PHeaderPanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PTabLayoutPanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PLayoutPanel;
import com.ponysdk.core.ui.basic.PMultiWordSuggestOracle;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PSuggestBox;
import com.ponysdk.core.ui.basic.PPopupPanel;
import com.ponysdk.core.ui.basic.PMenuItem;
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PComposite;
import com.ponysdk.core.ui.basic.PDecoratorPanel;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFocusPanel;
import com.ponysdk.core.ui.basic.PFunctionalLabel;
import com.ponysdk.core.ui.basic.PGrid;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPasswordTextBox;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PRadioButtonGroup;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PTabPanel;
import com.ponysdk.core.ui.basic.PTextArea;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PKeyDownEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFrame;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWebComponent;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

/**
 * The pages of the behaviour oracle (docs/decisions/0002-javascript-terminal.md, sample/tests/oracle.spec.js), opened
 * at {@code #oracle-<name>}. A page builds one widget with the handlers its scenario exercises, and nothing else: the
 * object ids, so the frames the terminal sends, depend only on what is built here.
 */
final class OraclePages {

    static final String TOKEN = "oracle-";

    private static final Map<String, Supplier<IsPWidget>> PAGES = Map.ofEntries(
        Map.entry("button", OraclePages::button),
        Map.entry("button-busy", OraclePages::busyButton),
        Map.entry("textbox", OraclePages::textBox),
        Map.entry("checkbox", OraclePages::checkBox),
        Map.entry("listbox", OraclePages::listBox),
        Map.entry("element", OraclePages::element),
        Map.entry("webcomponent", OraclePages::webComponent),
        Map.entry("frame", OraclePages::frame),
        Map.entry("reconnect", OraclePages::reconnect),
        Map.entry("texts", OraclePages::texts),
        Map.entry("radio", OraclePages::radio),
        Map.entry("fields", OraclePages::fields),
        Map.entry("listbox-multi", OraclePages::multiListBox),
        Map.entry("scroll", OraclePages::scroll),
        Map.entry("panels", OraclePages::panels),
        Map.entry("composite", OraclePages::composite),
        Map.entry("upload", OraclePages::upload),
        Map.entry("flextable", OraclePages::flexTable),
        Map.entry("grid", OraclePages::grid),
        Map.entry("disclosure", OraclePages::disclosure),
        Map.entry("tabpanel", OraclePages::tabPanel),
        Map.entry("popup", OraclePages::popup),
        Map.entry("dialog", OraclePages::dialog),
        Map.entry("menu", OraclePages::menu),
        Map.entry("tree", OraclePages::tree),
        Map.entry("suggest", OraclePages::suggest),
        Map.entry("layoutpanel", OraclePages::layoutPanel),
        Map.entry("dock", OraclePages::dock),
        Map.entry("split", OraclePages::split),
        Map.entry("tablayout", OraclePages::tabLayout),
        Map.entry("stack", OraclePages::stack),
        Map.entry("headerpanel", OraclePages::headerPanel));

    private OraclePages() {
    }

    static void build(final String token) {
        final Supplier<IsPWidget> page = PAGES.get(token.substring(TOKEN.length()));
        PWindow.getMain().add(page != null ? page.get() : Element.newPLabel("No oracle page " + token));
    }

    /** Stays enabled while its clicks are processed: every click and double click reaches the server. */
    private static IsPWidget button() {
        final PButton button = Element.newPButton("Oracle button");
        button.setEnabledOnRequest(true);
        button.addClickHandler(e -> {
        });
        button.addDoubleClickHandler(e -> {
        });
        return button;
    }

    /**
     * The default: a click disables the button until the server has processed it. The handler takes 300 ms, so that
     * the clicks of a double click made meanwhile are dropped by the terminal, whatever the speed of the round trip.
     */
    private static IsPWidget busyButton() {
        final PButton button = Element.newPButton("Oracle button");
        button.addClickHandler(e -> {
            try {
                Thread.sleep(300);
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        button.addDoubleClickHandler(e -> {
        });
        return button;
    }

    private static IsPWidget textBox() {
        final PTextBox textBox = Element.newPTextBox();
        textBox.addFocusHandler(e -> {
        });
        textBox.addKeyUpHandler(e -> {
        });
        textBox.addBlurHandler(e -> {
        });
        textBox.addValueChangeHandler(e -> {
        });
        return textBox;
    }

    private static IsPWidget checkBox() {
        final PCheckBox checkBox = Element.newPCheckBox("Oracle checkbox");
        checkBox.addValueChangeHandler(e -> {
        });
        return checkBox;
    }

    private static IsPWidget listBox() {
        final PListBox listBox = Element.newPListBox();
        listBox.addItem("Alpha");
        listBox.addItem("Beta");
        listBox.addItem("Gamma");
        listBox.addChangeHandler(e -> {
        });
        return listBox;
    }

    /**
     * The objects of a terminal without widgets: an element with a click and a visibility handler, a script whose
     * result comes back, a cookie read back through a script, a history item.
     */
    private static IsPWidget element() {
        final UIContext context = UIContext.get();
        final PElement section = Element.newPElement("section");
        section.setAttribute("aria-label", "Oracle element");
        section.addVisibilityHandler(e -> {
        });
        final PElement run = Element.newPElement("button");
        run.setInnerText("Run");
        run.addDomHandler((PClickHandler) e -> PScript.execute(PWindow.getMain(), "6 * 7", new PScript.ExecutionCallback() {

            @Override
            public void onSuccess(final String result) {
                context.getCookies().setCookie("oracle", "baked-" + result);
                PScript.execute(PWindow.getMain(), "document.cookie", new PScript.ExecutionCallback() {

                    @Override
                    public void onSuccess(final String cookies) {
                        run.setInnerText(cookies);
                        context.getHistory().newItem("oracle-element-done", true);
                    }

                    @Override
                    public void onFailure(final String message) {
                    }
                });
            }

            @Override
            public void onFailure(final String message) {
            }
        }), PClickEvent.TYPE);
        section.add(run);
        return section;
    }

    /** my-counter (script/webcomponents.js): an attribute, a property, its events, and a method called back. */
    private static IsPWidget webComponent() {
        final PWebComponent counter = new PWebComponent("my-counter");
        counter.attr("label", "Oracle");
        counter.property("count").set(5);
        counter.onEvent("count-changed", event -> {
            if (event.getString(ClientToServerModel.WC_EVENT_DETAIL.toStringValue(), "").contains("\"count\":7")) counter.call("reset");
        });
        return counter;
    }

    /** A frame running a terminal of its own (the application page), fed by this one. */
    private static IsPWidget frame() {
        final PFrame frame = Element.newPFrame("");
        frame.setHeight("120px");
        final PElement inside = Element.newPElement("button");
        inside.setInnerText("Inside");
        inside.addDomHandler((PClickHandler) e -> inside.setInnerText("Clicked"), PClickEvent.TYPE);
        frame.add(inside);
        return frame;
    }

    /** A counter kept by the server: after a reconnection, it goes on where it was if the context was resumed. */
    private static IsPWidget reconnect() {
        final PElement counter = Element.newPElement("button");
        counter.setInnerText("Clicks: 0");
        final int[] clicks = {0};
        counter.addDomHandler((PClickHandler) e -> counter.setInnerText("Clicks: " + ++clicks[0]), PClickEvent.TYPE);
        return counter;
    }

    // ---- P2: the simple widgets ----

    /** Text widgets and an image, each with a click handler. */
    private static IsPWidget texts() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("Oracle label");
        label.addClickHandler(e -> {
        });
        final PHTML html = Element.newPHTML("<b>Oracle html</b>");
        html.addClickHandler(e -> {
        });
        final PAnchor anchor = Element.newPAnchor("Oracle anchor");
        anchor.setEnabledOnRequest(true);
        anchor.addClickHandler(e -> {
        });
        final PImage image = Element.newPImage("images/pony.png");
        image.setAttribute("alt", "Oracle image");
        image.addClickHandler(e -> {
        });
        final PFunctionalLabel functional = Element.newPFunctionalLabel(
            new TextFunction(args -> args[0] + " ponies", "return args[0] + ' ponies';"), 3);
        panel.add(label, html, anchor, image, functional);
        return panel;
    }

    /** A group of three radio buttons, and a button that selects the first one from the server. */
    private static IsPWidget radio() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("oracle");
        final PRadioButton a = Element.newPRadioButton("Radio A");
        final PRadioButton b = Element.newPRadioButton("Radio B");
        final PRadioButton c = Element.newPRadioButton("Radio C");
        group.addRadioButton(a);
        group.addRadioButton(b);
        group.addRadioButton(c);
        group.addValueChangeHandler(e -> {
        });
        final PButton select = Element.newPButton("Select A");
        select.addClickHandler(e -> a.setState(PCheckBoxState.CHECKED));
        panel.add(a, b, c, select);
        return panel;
    }

    /** A text area and a password field that send their value, a text box listening to Enter only and to key down. */
    private static IsPWidget fields() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PTextArea area = Element.newPTextArea();
        area.setAttribute("aria-label", "Oracle area");
        area.addValueChangeHandler(e -> {
        });
        final PPasswordTextBox password = Element.newPPasswordTextBox();
        password.setAttribute("aria-label", "Oracle password");
        password.addValueChangeHandler(e -> {
        });
        final PTextBox box = Element.newPTextBox();
        box.setAttribute("aria-label", "Oracle enter");
        box.addKeyUpHandler(new PKeyUpHandler() {

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] {PKeyCodes.ENTER};
            }

            @Override
            public void onKeyUp(final PKeyUpEvent event) {
            }
        });
        box.addKeyDownHandler(e -> {
        });
        panel.add(area, password, box);
        return panel;
    }

    private static IsPWidget multiListBox() {
        final PListBox listBox = Element.newPListBox();
        listBox.setMultipleSelect(true);
        for (final String item : new String[] {"Alpha", "Beta", "Gamma", "Delta"}) listBox.addItem(item);
        listBox.addChangeHandler(e -> {
        });
        return listBox;
    }

    /** A scroll panel smaller than its content, scrolled by the page. */
    private static IsPWidget scroll() {
        final PScrollPanel panel = Element.newPScrollPanel();
        panel.setAttribute("aria-label", "Oracle scroll");
        panel.setWidth("200px");
        panel.setHeight("100px");
        final PElement content = Element.newPElement("div");
        content.setStyleProperty("width", "1000px");
        content.setStyleProperty("height", "1000px");
        panel.setWidget(content);
        panel.addScrollHandler(e -> {
        });
        return panel;
    }

    /** Cell panels, a focus panel, and a simple panel whose widget the server replaces. */
    private static IsPWidget panels() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PHorizontalPanel row = Element.newPHorizontalPanel();
        final PButton h1 = Element.newPButton("H1");
        final PButton h0 = Element.newPButton("H0");
        row.add(h1);
        row.insert(h0, 0);
        h0.addClickHandler(e -> {
        });
        final PVerticalPanel column = Element.newPVerticalPanel();
        final PButton v1 = Element.newPButton("V1");
        final PButton v2 = Element.newPButton("V2");
        column.add(v1);
        column.add(v2);
        v2.addClickHandler(e -> {
        });
        final PFocusPanel focus = Element.newPFocusPanel();
        focus.setAttribute("aria-label", "Oracle focus panel");
        focus.setWidget(Element.newPLabel("Focus me"));
        focus.addFocusHandler(e -> {
        });
        focus.addKeyDownHandler(e -> {
        });
        final PSimplePanel simple = Element.newPSimplePanel();
        simple.setWidget(Element.newPLabel("Before"));
        final PButton swap = Element.newPButton("Swap");
        swap.addClickHandler(e -> {
            final PLabel after = Element.newPLabel("After");
            after.addClickHandler(click -> {
            });
            simple.setWidget(after);
        });
        panel.add(row, column, focus, simple, swap);
        return panel;
    }

    /** A composite with a click handler around a panel holding a button with its own. */
    private static IsPWidget composite() {
        final PFlowPanel inside = Element.newPFlowPanel();
        final PButton button = Element.newPButton("Inside composite");
        button.setEnabledOnRequest(true);
        button.addClickHandler(e -> {
        });
        inside.add(button);
        final PComposite<PFlowPanel> composite = new PComposite<>() {

            {
                initWidget(inside);
            }

            @Override
            public String dumpDOM() {
                return inside.dumpDOM();
            }
        };
        composite.addDomHandler((PClickHandler) e -> {
        }, PClickEvent.TYPE);
        return composite;
    }

    /** A file upload submitted on change; the server reads the file and labels the upload with it. */
    private static IsPWidget upload() {
        final PFileUpload upload = Element.newPFileUpload();
        upload.setLabel("Oracle file");
        final String[] content = {""};
        upload.addStreamHandler((request, response, context) -> {
            try {
                final JakartaServletFileUpload<?, ?> parser = new JakartaServletFileUpload<>(DiskFileItemFactory.builder().get());
                for (final FileItem<?> item : parser.parseRequest(request)) {
                    if (!item.isFormField()) content[0] = new String(item.get(), StandardCharsets.UTF_8);
                }
            } catch (final Exception e) {
                content[0] = e.toString();
            }
        });
        upload.addSubmitCompleteHandler(() -> upload.setLabel("Uploaded: " + content[0]));
        upload.addChangeHandler(e -> upload.submit());
        return upload;
    }

    // ---- P3: tables and simple containers ----

    private static PButton clickable(final String text) {
        final PButton button = Element.newPButton(text);
        button.setEnabledOnRequest(true);
        button.addClickHandler(e -> {
        });
        return button;
    }

    /**
     * Widgets in cells; once the table is on the terminal, a row inserted, a column span, row and cell styles; a button
     * removes the first row. (Before attachment, the server replays INSERT_ROW out of order with the widgets.)
     */
    private static IsPWidget flexTable() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PFlexTable table = Element.newPFlexTable();
        table.setWidget(0, 0, clickable("R0C0"));
        table.setWidget(0, 1, clickable("R0C1"));
        table.setWidget(1, 0, clickable("R1C0"));
        table.addInitializeListener(event -> {
            table.insertRow(1);
            table.setWidget(1, 0, clickable("New"));
            table.getCellFormatter().setColSpan(1, 0, 2);
            table.getRowFormatter().addStyleName(0, "first");
            table.getCellFormatter().addStyleName(2, 0, "last");
            table.setCellPadding(4);
        });
        final PButton remove = Element.newPButton("Remove row 0");
        remove.addClickHandler(e -> table.removeRow(0));
        panel.add(table, remove);
        return panel;
    }

    /** A 2 x 2 grid in a decorator panel; a button inserts a row. */
    private static IsPWidget grid() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PGrid grid = Element.newPGrid(2, 2);
        grid.setWidget(0, 0, clickable("G00"));
        grid.setWidget(1, 1, clickable("G11"));
        final PDecoratorPanel decorator = Element.newPDecoratorPanel();
        decorator.setWidget(grid);
        final PButton insert = Element.newPButton("Insert row");
        insert.addClickHandler(e -> {
            grid.insertRow(1);
            grid.setWidget(1, 0, clickable("G10"));
        });
        panel.add(decorator, insert);
        return panel;
    }

    /** Opened and closed by the user, then opened by the server. */
    private static IsPWidget disclosure() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PDisclosurePanel disclosure = Element.newPDisclosurePanel("Oracle details");
        disclosure.setContent(Element.newPLabel("Hidden content"));
        disclosure.addOpenHandler(e -> {
        });
        disclosure.addCloseHandler(e -> {
        });
        final PButton open = Element.newPButton("Open from server");
        open.addClickHandler(e -> disclosure.setOpen(true));
        panel.add(disclosure, open);
        return panel;
    }

    /** Text tabs and a widget tab, selected by the user then by the server. */
    private static IsPWidget tabPanel() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PTabPanel tabs = Element.newPTabPanel();
        tabs.addBeforeSelectionHandler(e -> {
        });
        tabs.addSelectionHandler(e -> {
        });
        tabs.add(Element.newPLabel("First panel"), "Tab 1");
        tabs.add(Element.newPLabel("Second panel"), "Tab 2");
        // once the panel is on the terminal: a tab widget given before is never created (server side)
        tabs.addInitializeListener(event -> tabs.add(Element.newPLabel("Third panel"), Element.newPLabel("Tab 3")));
        final PButton first = Element.newPButton("Select first");
        first.addClickHandler(e -> tabs.selectTab(0));
        panel.add(tabs, first);
        return panel;
    }

    // ---- P4: popups, menus, tree, suggestions ----

    /** An auto-hide popup shown at the center, hidden by a press outside; one placed by the server from its size. */
    private static IsPWidget popup() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PButton show = Element.newPButton("Show popup");
        show.addClickHandler(e -> {
            final PPopupPanel popup = Element.newPPopupPanel(true);
            final PButton inside = clickable("Inside popup");
            popup.setWidget(inside);
            popup.addCloseHandler(close -> {
            });
            PWindow.getMain().add(popup);
            popup.center();
        });
        final PButton placed = Element.newPButton("Show placed");
        placed.addClickHandler(e -> {
            final PPopupPanel popup = Element.newPPopupPanel(false);
            popup.setWidget(Element.newPLabel("Placed popup"));
            popup.addCloseHandler(close -> {
            });
            PWindow.getMain().add(popup);
            popup.setPopupPositionAndShow((width, height, windowWidth, windowHeight) -> popup.setPopupPosition(10, 200));
            final PButton hide = Element.newPButton("Hide placed");
            hide.addClickHandler(h -> popup.hide());
            panel.add(hide);
        });
        panel.add(show, placed);
        return panel;
    }

    /** A dialog box with a glass, closed by a button; then a modal one. */
    private static IsPWidget dialog() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PButton open = Element.newPButton("Open dialog");
        open.addClickHandler(e -> dialogBox("Oracle dialog", false));
        final PButton modal = Element.newPButton("Open modal");
        modal.addClickHandler(e -> dialogBox("Oracle modal", true));
        panel.add(open, modal);
        return panel;
    }

    private static PDialogBox dialogBox(final String caption, final boolean modal) {
        final PDialogBox dialog = Element.newPDialogBox(false);
        dialog.setCaption(caption);
        dialog.setGlassEnabled(!modal);
        dialog.setModal(modal);
        final PButton close = Element.newPButton("Close " + caption);
        close.addClickHandler(e -> dialog.hide());
        dialog.setWidget(close);
        dialog.addCloseHandler(e -> {
        });
        PWindow.getMain().add(dialog); // as the showcase does: attached to its window, then centred
        dialog.center();
        return dialog;
    }

    /** A bar with nested submenus, a separator, a disabled item and a command on the bar. */
    private static IsPWidget menu() {
        final PMenuBar bar = Element.newPMenuBar();
        final PMenuBar file = Element.newPMenuBar(true);
        file.addItem("New", () -> {
        });
        file.addSeparator();
        file.addItem("Open", () -> {
        });
        final PMenuBar recent = Element.newPMenuBar(true);
        recent.addItem("Doc 1", () -> {
        });
        file.addItem("Recent", recent);
        final PMenuItem disabled = file.addItem("Disabled", () -> {
        });
        disabled.setEnabled(false);
        final PMenuBar edit = Element.newPMenuBar(true);
        edit.addItem("Copy", () -> {
        });
        bar.addItem("File", file);
        bar.addItem("Edit", edit);
        bar.addItem("Help", () -> {
        });
        return bar;
    }

    /** Items opened, selected (twice), closed by the user; one selected by the server; a widget item. */
    private static IsPWidget tree() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PTree tree = Element.newPTree();
        final PTreeItem fruits = tree.add("Fruits");
        fruits.add("Apple");
        fruits.add("Banana");
        final PTreeItem vegetables = tree.add("Vegetables");
        final PTreeItem carrot = vegetables.add("Carrot");
        vegetables.add(Element.newPTreeItem(Element.newPLabel("Widget leaf")));
        tree.addSelectionHandler(e -> {
        });
        final PButton select = Element.newPButton("Select Carrot");
        select.addClickHandler(e -> tree.setSelectedItem(carrot));
        panel.add(tree, select);
        return panel;
    }

    /** A suggest box on a multi-word oracle. */
    private static IsPWidget suggest() {
        final PMultiWordSuggestOracle oracle = new PMultiWordSuggestOracle();
        oracle.addAll(List.of("Apple", "Apricot", "Banana", "Blueberry", "Cherry", "Red apple"));
        final PSuggestBox box = Element.newPSuggestBox(oracle);
        box.getTextBox().setAttribute("aria-label", "Oracle suggest");
        // shown with the replacement the server writes back to the box: once it shows, the box holds it
        final PLabel selected = Element.newPLabel("Selected: none");
        box.addSelectionHandler(e -> selected.setText("Selected: " + e.getSelectedItem().getReplacementString()));
        box.addValueChangeHandler(e -> {
        });
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(box, selected);
        return panel;
    }

    // ---- P5: layout panels ----

    private static <T extends com.ponysdk.core.ui.basic.PWidget> T sized(final T widget, final String label, final String width,
                                                                       final String height) {
        widget.setAttribute("aria-label", label);
        widget.setWidth(width);
        widget.setHeight(height);
        return widget;
    }

    /** Children placed by constraints and alignment; one moved by the server. */
    private static IsPWidget layoutPanel() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLayoutPanel layout = sized(Element.newPLayoutPanel(), "Oracle layout", "400px", "200px");
        final PButton a = clickable("A");
        final PButton b = clickable("B");
        final PLabel c = Element.newPLabel("C");
        layout.add(a);
        layout.add(b);
        layout.add(c);
        layout.setWidgetLeftWidth(a, 10, 100, PUnit.PX);
        layout.setWidgetTopHeight(a, 10, 30, PUnit.PX);
        layout.setWidgetRightWidth(b, 10, 80, PUnit.PX);
        layout.setWidgetTopHeight(b, 50, 30, PUnit.PX);
        layout.setWidgetBottomHeight(c, 0, 20, PUnit.PX);
        layout.setWidgetHorizontalPosition(c, PAlignment.BEGIN);
        final PButton move = Element.newPButton("Move A");
        move.addClickHandler(e -> {
            layout.setWidgetLeftWidth(a, 200, 100, PUnit.PX);
            layout.animate(Duration.ZERO);
        });
        panel.add(layout, move);
        return panel;
    }

    /** North, west and center; the server grows the north. */
    private static IsPWidget dock() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PDockLayoutPanel dock = sized(Element.newPDockLayoutPanel(PUnit.PX), "Oracle dock", "400px", "300px");
        final PLabel north = Element.newPLabel("North");
        dock.addNorth(north, 50);
        dock.addWest(Element.newPLabel("West"), 80);
        dock.add(clickable("Center"));
        final PButton grow = Element.newPButton("Grow north");
        grow.addClickHandler(e -> dock.setWidgetSize(north, 100));
        panel.add(dock, grow);
        return panel;
    }

    /** A west pane resized by its splitter; the server is told the sizes. */
    private static IsPWidget split() {
        final PSplitLayoutPanel split = sized(Element.newPSplitLayoutPanel(), "Oracle split", "400px", "300px");
        final PLabel west = Element.newPLabel("West pane");
        split.addWest(west, 100);
        split.add(clickable("Center pane"));
        split.setWidgetMinSize(west, 50);
        split.addLayoutResizeHandler(e -> {
        });
        return split;
    }

    /** Tabs selected by the user and by the server; removing the selected one selects the first. */
    private static IsPWidget tabLayout() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PTabLayoutPanel tabs = sized(Element.newPTabLayoutPanel(), "Oracle tab layout", "400px", "200px");
        tabs.addBeforeSelectionHandler(e -> {
        });
        tabs.addSelectionHandler(e -> {
        });
        final PLabel first = Element.newPLabel("TL1 panel");
        tabs.add(first, "TL 1");
        tabs.add(Element.newPLabel("TL2 panel"), "TL 2");
        final PLabel third = Element.newPLabel("TL3 panel");
        tabs.add(third, "TL 3");
        final PButton select = Element.newPButton("Select TL 1");
        select.addClickHandler(e -> tabs.selectTab(0));
        final PButton remove = Element.newPButton("Remove TL 3");
        remove.addClickHandler(e -> tabs.remove(third));
        panel.add(tabs, select, remove);
        return panel;
    }

    /** Headers opened by the user and by the server; removing the open one opens the first. */
    private static IsPWidget stack() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PStackLayoutPanel stack = sized(Element.newPStackLayoutPanel(PUnit.PX), "Oracle stack", "300px", "300px");
        stack.addBeforeSelectionHandler(e -> {
        });
        stack.addSelectionHandler(e -> {
        });
        stack.add(Element.newPLabel("S1 content"), "S1", false, 30);
        stack.add(Element.newPLabel("S2 content"), "S2", false, 30);
        final PLabel third = Element.newPLabel("S3 content");
        stack.add(third, "S3", false, 30);
        final PButton show = Element.newPButton("Show S3");
        show.addClickHandler(e -> stack.showWidget(third));
        final PButton remove = Element.newPButton("Remove S3");
        remove.addClickHandler(e -> stack.remove(third));
        panel.add(stack, show, remove);
        return panel;
    }

    /** A header and a footer around a content that fills the rest. */
    private static IsPWidget headerPanel() {
        final PHeaderPanel header = sized(Element.newPHeaderPanel(), "Oracle header panel", "300px", "200px");
        final PLabel head = Element.newPLabel("Head");
        head.setHeight("30px");
        final PLabel foot = Element.newPLabel("Foot");
        foot.setHeight("20px");
        header.setHeaderWidget(head);
        header.setContentWidget(clickable("Body button"));
        header.setFooterWidget(foot);
        return header;
    }
}
