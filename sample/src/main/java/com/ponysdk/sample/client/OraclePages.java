/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.util.Map;
import java.util.function.Supplier;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
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

    private static final Map<String, Supplier<IsPWidget>> PAGES = Map.of(
        "button", OraclePages::button,
        "button-busy", OraclePages::busyButton,
        "textbox", OraclePages::textBox,
        "checkbox", OraclePages::checkBox,
        "listbox", OraclePages::listBox,
        "element", OraclePages::element,
        "webcomponent", OraclePages::webComponent,
        "frame", OraclePages::frame,
        "reconnect", OraclePages::reconnect);

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
}
