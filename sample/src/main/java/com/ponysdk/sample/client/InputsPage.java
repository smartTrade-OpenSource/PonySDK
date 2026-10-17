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

import static com.ponysdk.sample.client.Ui.*;
import static com.ponysdk.sample.client.Costs.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PDatePicker;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PRadioButtonGroup;
import com.ponysdk.core.ui.basic.PPushButton;
import com.ponysdk.core.ui.basic.PRichTextArea;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.form2.impl.formfield.ColorInputFormField;
import com.ponysdk.core.ui.form2.impl.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.rich.PTwinListBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Inputs: every form control, live, with the Java behind it and its cost. */
final class InputsPage {

    private static final Logger log = LoggerFactory.getLogger(InputsPage.class);

    private InputsPage() {
    }

    // ── Tab: Inputs ──────────────────────────────────────────────────────────

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "Inputs",
            "Each control below is live. Interact with it and watch what the server receives — then flip to Code to see the exact Java behind it. No JavaScript, anywhere.");

        p.add(sectionTitle("Interactive — try it, then read the code"));

        // 1) TextBox — the value is read by a server-side handler
        final PTextBox tb = Element.newPTextBox();
        final PElement tbEcho = echoIdle("server: waiting for input… (type, then press Enter)");
        tb.addValueChangeHandler(e -> echoSet(tbEcho, "server received → \"" + e.getData() + "\""));
        p.add(exampleCard("PTextBox",
            "A text field whose value is read by a Java handler the moment it changes (Enter or blur).",
            previewWithEcho(tb, tbEcho), snippet_exTextBox(), costSpec("PTextBox")));

        // 2) Button — every click is a server-side lambda
        final PButton btn = Element.newPButton("Click me");
        final int[] clicks = { 0 };
        final PElement btnEcho = echoIdle("server: not clicked yet");
        btn.addClickHandler(e -> echoSet(btnEcho, "server handled click #" + (++clicks[0]) + " (pure Java lambda)"));
        p.add(exampleCard("PButton",
            "Every click invokes a Java lambda on the JVM. Only a tiny binary delta crosses the wire.",
            previewWithEcho(btn, btnEcho), snippet_exButton(), costSpec("PButton")));

        // 3) CheckBox — state lives on the server
        final PCheckBox cb = Element.newPCheckBox("Enable feature");
        final PElement cbEcho = echoIdle("server: checked = false");
        cb.addValueChangeHandler(e -> echoSet(cbEcho, "server: checked = " + e.getData()));
        p.add(exampleCard("PCheckBox",
            "The checked state lives on the server. The handler fires the instant the box toggles.",
            previewWithEcho(cb, cbEcho), snippet_exCheckBox(), costSpec("PCheckBox")));

        // 4) RadioButton group — mutually-exclusive selection
        final PFlowPanel radios = Element.newPFlowPanel();
        radios.setStyleProperty("display", "flex");
        radios.setStyleProperty("gap", "16px");
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("size");
        final PElement rbEcho = echoIdle("server: no selection");
        for (final String opt : List.of("Small", "Medium", "Large")) {
            final PRadioButton rb = Element.newPRadioButton(opt);
            group.addRadioButton(rb);
            rb.addValueChangeHandler(e -> {
                if (Boolean.TRUE.equals(e.getData())) echoSet(rbEcho, "server: selected = " + opt);
            });
            radios.add(rb);
        }
        p.add(exampleCard("PRadioButton (group)",
            "Grouped radios share a name; selecting one clears the others and the server learns the choice.",
            previewWithEcho(radios, rbEcho), snippet_exRadio(), costSpec("PRadioButton")));

        // ── The long tail, kept compact for completeness ───────────────────────
        p.add(sectionTitle("More input widgets"));
        p.add(labeled("TextArea", Element.newPTextArea()));
        p.add(labeled("Password", Element.newPPasswordTextBox("Password")));
        p.add(labeled("RichText editor", buildRichText()));
        final PPushButton push = Element.newPPushButton(Element.newPImage());
        push.setText("Push me");
        p.add(labeled("PushButton", push));

        p.add(sectionTitle("Date & Time"));
        p.add(labeled("DateBox dd/MM/yyyy", Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy"))));
        p.add(labeled("DateBox with picker", Element.newPDateBox(Element.newPDatePicker(), new SimpleDateFormat("yyyy/MM/dd"))));
        p.add(labeled("DateBox with time", buildDateBoxWithLog()));
        p.add(labeled("DatePicker", Element.newPDatePicker()));
        p.add(labeled("DateBox (reset)", createDateBox()));

        p.add(sectionTitle("Selection"));
        p.add(labeled("ListBox (multi)", createListBox()));
        p.add(labeled("TwinListBox", new PTwinListBox<>()));
        p.add(labeled("MenuBar", createMenu()));

        p.add(sectionTitle("Form fields"));
        p.add(labeled("String FormField", new StringTextBoxFormField("String")));
        p.add(labeled("Color FormField", new ColorInputFormField("Color")));

        p.add(sectionTitle("File"));
        p.add(labeled("FileUpload", MiscPage.createPFileUpload()));

        return p;
    }

    static String snippet_exTextBox() {
        return ln(ty("PTextBox") + " box = " + ty("Element") + "." + mt("newPTextBox") + "();") +
            ln(cm("fires when the value changes (Enter / blur)")) +
            ln("box." + mt("addValueChangeHandler") + "(e ->") +
            ln("    echo." + mt("setText") + "(" + st("server received → ") + " + e." + mt("getData") + "()));") +
            ln(ty("PWindow") + "." + mt("getMain") + "()." + mt("add") + "(box);");
    }

    static String snippet_exButton() {
        return ln(ty("PButton") + " btn = " + ty("Element") + "." + mt("newPButton") + "(" + st("Click me") + ");") +
            ln(ty("int") + "[] clicks = {" + nu("0") + "};") +
            ln(cm("every click is a Java lambda — runs on the JVM")) +
            ln("btn." + mt("addClickHandler") + "(e ->") +
            ln("    echo." + mt("setText") + "(" + st("handled click #") + " + (++clicks[" + nu("0") + "])));");
    }

    static String snippet_exCheckBox() {
        return ln(ty("PCheckBox") + " cb = " + ty("Element") + "." + mt("newPCheckBox") + "(" + st("Enable feature") + ");") +
            ln(cm("state lives on the server, not in the browser")) +
            ln("cb." + mt("addValueChangeHandler") + "(e ->") +
            ln("    echo." + mt("setText") + "(" + st("checked = ") + " + e." + mt("getData") + "()));");
    }

    static String snippet_exRadio() {
        return ln(ty("PRadioButtonGroup") + " group = " + ty("Element") + "." + mt("newPRadioButtonGroup") + "(" + st("size") + ");") +
            ln(kw("for") + " (" + ty("String") + " opt : " + ty("List") + "." + mt("of") + "(" + st("Small") + ", " + st("Medium") + ", " + st("Large") + ")) {") +
            ln("  " + ty("PRadioButton") + " rb = " + ty("Element") + "." + mt("newPRadioButton") + "(opt);") +
            ln("  group." + mt("addRadioButton") + "(rb);  " + cm("mutual exclusion")) +
            ln("  rb." + mt("addValueChangeHandler") + "(e -> {") +
            ln("    " + kw("if") + " (" + ty("Boolean") + ".TRUE." + mt("equals") + "(e." + mt("getData") + "()))") +
            ln("      echo." + mt("setText") + "(" + st("selected = ") + " + opt);") +
            ln("  });") +
            ln("  row." + mt("add") + "(rb);") +
            ln("}");
    }

    static PWidget buildRichText() {
        final PFlowPanel p = Element.newPFlowPanel();
        final PRichTextArea rta = Element.newPRichTextArea();
        // the toolbar links to the area: the area is created first, the toolbar is still shown above it
        p.setStyleProperty("display", "flex");
        p.setStyleProperty("flex-direction", "column-reverse");
        p.add(rta);
        p.add(Element.newPRichTextToolbar(rta));
        return p;
    }

    static PDateBox buildDateBoxWithLog() {
        final PDateBox db = Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"), true);
        db.addValueChangeHandler(e -> log.debug("DateBox value: {}", e.getData()));
        return db;
    }

    static final PListBox createListBox() {
        final PListBox pListBox = Element.newPListBox(true);
        pListBox.addItem("A");
        pListBox.addItem("B");
        pListBox.insertItem("C", 1);
        pListBox.addItemsInGroup("sport", "Baseball", "Basketball", "Football", "Hockey", "Water Polo");
        return pListBox;
    }

    static final PMenuBar createMenu() {
        final PMenuBar pMenuBar = Element.newPMenuBar(true);
        pMenuBar.addItem(Element.newPMenuItem("Menu 1", Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 2", true, Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 3", () -> log.debug("Menu click")));
        pMenuBar.addSeparator();
        return pMenuBar;
    }

    static final PFlowPanel createDateBox() {
        final PFlowPanel flowPanel = Element.newPFlowPanel();

        final PDatePicker datePicker = Element.newPDatePicker();
        final Date a = new Date();

        datePicker.setTransientEnabledOnDates(false, List.of(new Date(), new Date(a.getYear(), a.getMonth(), 26)));
        datePicker.addStyleToDates("toto", List.of(new Date()));

        final PDateBox dateBox = Element.newPDateBox(datePicker, new SimpleDateFormat("dd/MM/yyyy"));
        //dateBox.setValue(new Date(0));
        flowPanel.add(dateBox);
        datePicker.addShowRangeHandler(e -> {
            datePicker.setTransientEnabledOnDates(false, List.of(new Date(), new Date(a.getYear(), a.getMonth(), 26)));
            datePicker.addStyleToDates("toto", List.of(new Date()));
        });

        final PButton button = Element.newPButton("reset");
        button.addClickHandler(event -> dateBox.setValue(null));
        flowPanel.add(button);
        return flowPanel;
    }
}
