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
import com.ponysdk.core.ui.form.formfield.IntegerTextBoxFormField;
import com.ponysdk.core.ui.form.validator.IntegerFieldValidator;
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
            "Each control below is live. Interact with it and watch what the server receives, then open Code: it shows "
                + "the lines of this page's source that built it. No JavaScript, anywhere.");

        p.add(sectionTitle("Interactive: try it, then read the code"));

        // region textbox
        final PTextBox box = Element.newPTextBox();
        final PElement boxEcho = echoIdle("server: waiting for input… (type, then press Enter)");
        // fires when the value changes: Enter or leaving the field
        box.addValueChangeHandler(e -> echoSet(boxEcho, "server received → \"" + e.getData() + "\""));
        // endregion
        p.add(exampleCard("PTextBox",
            "A text field whose value is read by a Java handler the moment it changes (Enter or blur).",
            previewWithEcho(box, boxEcho), Source.code(InputsPage.class, "textbox"), costSpec("PTextBox")));

        // region button
        final PButton button = Element.newPButton("Click me");
        final int[] clicks = { 0 };
        final PElement buttonEcho = echoIdle("server: not clicked yet");
        // every click is a Java lambda, run on the server
        button.addClickHandler(e -> echoSet(buttonEcho, "server handled click #" + (++clicks[0]) + " (pure Java lambda)"));
        // endregion
        p.add(exampleCard("PButton",
            "Every click invokes a Java lambda on the JVM. Only a tiny binary delta crosses the wire.",
            previewWithEcho(button, buttonEcho), Source.code(InputsPage.class, "button"), costSpec("PButton")));

        // region checkbox
        final PCheckBox checkBox = Element.newPCheckBox("Enable feature");
        final PElement checkEcho = echoIdle("server: checked = false");
        // the state lives on the server
        checkBox.addValueChangeHandler(e -> echoSet(checkEcho, "server: checked = " + e.getData()));
        // endregion
        p.add(exampleCard("PCheckBox",
            "The checked state lives on the server. The handler fires the instant the box toggles.",
            previewWithEcho(checkBox, checkEcho), Source.code(InputsPage.class, "checkbox"), costSpec("PCheckBox")));

        // region radio
        final PFlowPanel radios = Element.newPFlowPanel();
        radios.addStyleName("pony-inline");
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("size");
        final PElement radioEcho = echoIdle("server: no selection");
        for (final String option : List.of("Small", "Medium", "Large")) {
            final PRadioButton radio = Element.newPRadioButton(option);
            group.addRadioButton(radio); // one selected at a time
            radio.addValueChangeHandler(e -> {
                if (Boolean.TRUE.equals(e.getData())) echoSet(radioEcho, "server: selected = " + option);
            });
            radios.add(radio);
        }
        // endregion
        p.add(exampleCard("PRadioButton (group)",
            "Grouped radios share a name; selecting one clears the others and the server learns the choice.",
            previewWithEcho(radios, radioEcho), Source.code(InputsPage.class, "radio"), costSpec("PRadioButton")));

        // ── The long tail: each row opens its code ───────────────────────────────
        p.add(sectionTitle("More input widgets"));
        // region textarea
        final PWidget textArea = Element.newPTextArea();
        // endregion
        p.add(labeled("TextArea", textArea, Source.code(InputsPage.class, "textarea")));
        // region password
        final PWidget password = Element.newPPasswordTextBox("Password");
        // endregion
        p.add(labeled("Password", password, Source.code(InputsPage.class, "password")));
        p.add(labeled("RichText editor", buildRichText(), Source.code(InputsPage.class, "richtext")));
        // region pushbutton
        final PPushButton push = Element.newPPushButton(Element.newPImage());
        push.setText("Push me");
        // endregion
        p.add(labeled("PushButton", push, Source.code(InputsPage.class, "pushbutton")));

        p.add(sectionTitle("Date & Time"));
        // region datebox
        final PDateBox dateBox = Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy"));
        // endregion
        p.add(labeled("DateBox dd/MM/yyyy", dateBox, Source.code(InputsPage.class, "datebox")));
        // region dateboxpicker
        final PDateBox pickerBox = Element.newPDateBox(Element.newPDatePicker(), new SimpleDateFormat("yyyy/MM/dd"));
        // endregion
        p.add(labeled("DateBox with picker", pickerBox, Source.code(InputsPage.class, "dateboxpicker")));
        p.add(labeled("DateBox with time", buildDateBoxWithLog(), Source.code(InputsPage.class, "dateboxtime")));
        // region datepicker
        final PDatePicker datePicker = Element.newPDatePicker();
        // endregion
        p.add(labeled("DatePicker", datePicker, Source.code(InputsPage.class, "datepicker")));
        p.add(labeled("DateBox (reset)", createDateBox(), Source.code(InputsPage.class, "dateboxreset")));

        p.add(sectionTitle("Selection"));
        p.add(labeled("ListBox (multiple)", createListBox(), Source.code(InputsPage.class, "listbox")));
        p.add(labeled("TwinListBox", createTwinListBox(), Source.code(InputsPage.class, "twinlistbox")));
        p.add(labeled("MenuBar", createMenu(), Source.code(InputsPage.class, "menubar")));

        p.add(sectionTitle("Form fields"));
        // region formfields
        final IntegerTextBoxFormField quantity = new IntegerTextBoxFormField();
        quantity.setValidator(new IntegerFieldValidator());
        quantity.addValueChangeHandler(event -> quantity.isValid());
        // endregion
        p.add(labeled("Integer FormField", quantity, Source.code(InputsPage.class, "formfields")));

        p.add(sectionTitle("File"));
        p.add(labeled("FileUpload", MiscPage.createPFileUpload(), Source.code(MiscPage.class, "fileupload")));

        return p;
    }

    static PWidget buildRichText() {
        // region richtext
        final PFlowPanel editor = Element.newPFlowPanel();
        final PRichTextArea area = Element.newPRichTextArea();
        // the toolbar links to the area: the area is created first, the toolbar is still shown above it
        editor.setStyleProperty("display", "flex");
        editor.setStyleProperty("flex-direction", "column-reverse");
        editor.add(area);
        editor.add(Element.newPRichTextToolbar(area));
        // endregion
        return editor;
    }

    static PDateBox buildDateBoxWithLog() {
        // region dateboxtime
        final PDateBox dateBox = Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"), true);
        dateBox.addValueChangeHandler(e -> log.debug("DateBox value: {}", e.getData()));
        // endregion
        return dateBox;
    }

    static final PListBox createListBox() {
        // region listbox
        final PListBox listBox = Element.newPListBox();
        listBox.setMultipleSelect(true); // Ctrl or Shift + click selects several
        listBox.addItem("A");
        listBox.addItem("B");
        listBox.insertItem("C", 1); // at index 1: A, C, B
        listBox.addItemsInGroup("Sport", "Baseball", "Basketball", "Football", "Hockey", "Water Polo");
        // endregion
        return listBox;
    }

    static final PTwinListBox<String> createTwinListBox() {
        // region twinlistbox
        final PTwinListBox<String> twin = new PTwinListBox<>("Available", "Selected");
        for (final String currency : List.of("EUR", "USD", "GBP", "JPY", "CHF")) twin.getLeftListBox().addItem(currency);
        // endregion
        return twin;
    }

    static final PMenuBar createMenu() {
        // region menubar
        final PMenuBar menuBar = Element.newPMenuBar(true);
        menuBar.addItem(Element.newPMenuItem("Menu 1", Element.newPMenuBar()));
        menuBar.addItem(Element.newPMenuItem("Menu 2", true, Element.newPMenuBar()));
        menuBar.addItem(Element.newPMenuItem("Menu 3", () -> log.debug("Menu click")));
        menuBar.addSeparator();
        // endregion
        return menuBar;
    }

    static final PFlowPanel createDateBox() {
        // region dateboxreset
        final PFlowPanel flowPanel = Element.newPFlowPanel();
        final PDatePicker datePicker = Element.newPDatePicker();
        final Date today = new Date();
        final List<Date> closed = List.of(today, new Date(today.getYear(), today.getMonth(), 26)); // not selectable
        datePicker.setTransientEnabledOnDates(false, closed);
        datePicker.addStyleToDates("pony-date-today", List.of(today));
        // the picker shows another month: its dates are set again
        datePicker.addShowRangeHandler(e -> {
            datePicker.setTransientEnabledOnDates(false, closed);
            datePicker.addStyleToDates("pony-date-today", List.of(today));
        });

        final PDateBox dateBox = Element.newPDateBox(datePicker, new SimpleDateFormat("dd/MM/yyyy"));
        final PButton reset = Element.newPButton("Reset");
        reset.addClickHandler(event -> dateBox.setValue(null));
        flowPanel.add(dateBox);
        flowPanel.add(reset);
        // endregion
        return flowPanel;
    }
}
