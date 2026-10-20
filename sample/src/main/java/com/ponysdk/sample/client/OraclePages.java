/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PComposite;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFocusPanel;
import com.ponysdk.core.ui.basic.PFunctionalLabel;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPasswordTextBox;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PRadioButtonGroup;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
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
        Map.entry("upload", OraclePages::upload));

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
}
