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

package com.ponysdk.sample.client.page;

import java.time.Duration;

import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PTextArea;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

public class BasicTextBoxPageActivity extends SamplePageActivity {

    public BasicTextBoxPageActivity() {
        super("Basic Text", "Text Input");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = Element.newPVerticalPanel();

        final PTextBox textBox = Element.newPTextBox();
        final PTextBox textBoxReadOnly = Element.newPTextBox();
        textBoxReadOnly.setText("read only");
        textBoxReadOnly.setEnabled(false);
        final PTextBox passwordTextBox = Element.newPPasswordTextBox();
        final PTextBox passwordTextBoxReadOnly = Element.newPPasswordTextBox();
        passwordTextBoxReadOnly.setText("xxxxxxxxxxxx");
        passwordTextBoxReadOnly.setEnabled(false);
        final PTextArea textArea = Element.newPTextArea();

        panel.add(Element.newPLabel("Normal text box:"));
        panel.add(textBox);
        panel.add(textBoxReadOnly);

        final PTextBox placeHolder = Element.newPTextBox();
        panel.add(Element.newPLabel("Place holder : "));
        panel.add(placeHolder);

        final PButton button = Element.newPButton("Set");
        button.addClickHandler(event -> {
            textBox.setPlaceholder(placeHolder.getText());
            textBoxReadOnly.setPlaceholder(placeHolder.getText());
        });
        panel.add(button);

        final String pattern = "[a-zA-Z0-9]";
        final PTextBox filtered = Element.newPTextBox();
        filtered.setFilter(pattern);
        panel.add(Element.newPLabel("Filtered text box ( " + pattern + " ):"));
        panel.add(filtered);

        final PTextBox masked = Element.newPTextBox();
        final PTextBox maskedTextBox = Element.newPTextBox();
        final PTextBox replacement = Element.newPTextBox();
        final PCheckBox showMask = Element.newPCheckBox("Show mask");
        final PButton applyMaskButton = Element.newPButton("Apply mask");
        applyMaskButton.addClickHandler(event -> {
            if (masked.getText().isEmpty()) return;

            String replaceChar = " ";
            if (!replacement.getText().isEmpty()) replaceChar = replacement.getText().substring(0, 1);
            maskedTextBox.applyMask(masked.getText(), showMask.getValue(), replaceChar);
        });
        masked.setPlaceholder("({{000}}) {{000}}.{{0000}}");
        replacement.setWidth("10px");

        final PHorizontalPanel maskPanel = Element.newPHorizontalPanel();
        maskPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        maskPanel.add(masked);
        maskPanel.add(maskedTextBox);
        maskPanel.add(replacement);
        maskPanel.add(showMask);
        maskPanel.add(applyMaskButton);

        panel.add(Element.newPLabel("Password text box:"));
        panel.add(passwordTextBox);
        panel.add(passwordTextBoxReadOnly);
        panel.add(Element.newPLabel("Text area:"));
        panel.add(textArea);
        panel.add(maskPanel);
        panel.add(Element.newPLabel("AddOn test (javascript reverse)"));

        final PTextBox boxToReverse = Element.newPTextBox();
        PScheduler.schedule(() -> panel.add(boxToReverse), Duration.ofMillis(1500));

        examplePanel.setWidget(panel);
    }
}
