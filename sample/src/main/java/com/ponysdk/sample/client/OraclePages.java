/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.util.Map;
import java.util.function.Supplier;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;

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
        "listbox", OraclePages::listBox);

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
}
