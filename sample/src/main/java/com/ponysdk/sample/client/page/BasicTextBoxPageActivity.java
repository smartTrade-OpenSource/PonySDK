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

import com.ponysdk.sample.client.page.addon.ReverseTextInput;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPasswordTextBox;
import com.ponysdk.ui.server.basic.PTerminalScheduledCommand;
import com.ponysdk.ui.server.basic.PTextArea;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class BasicTextBoxPageActivity extends SamplePageActivity {

    public BasicTextBoxPageActivity() {
        super("Basic Text", "Text Input");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();

        final PTextBox textBox = new PTextBox();
        final PTextBox textBoxReadOnly = new PTextBox();
        textBoxReadOnly.setText("read only");
        textBoxReadOnly.setEnabled(false);
        final PTextBox passwordTextBox = new PPasswordTextBox();
        final PTextBox passwordTextBoxReadOnly = new PPasswordTextBox();
        passwordTextBoxReadOnly.setText("xxxxxxxxxxxx");
        passwordTextBoxReadOnly.setEnabled(false);
        final PTextArea textArea = new PTextArea();

        panel.add(new PLabel("Normal text box:"));
        panel.add(textBox);
        panel.add(textBoxReadOnly);

        final PTextBox placeHolder = new PTextBox();
        panel.add(new PLabel("Place holder : "));
        panel.add(placeHolder);

        final PButton button = new PButton("Set");
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                textBox.setPlaceholder(placeHolder.getText());
                textBoxReadOnly.setPlaceholder(placeHolder.getText());
            }
        });
        panel.add(button);

        final PTextBox masked = new PTextBox();
        final PTextBox maskedTextBox = new PTextBox();
        final PTextBox replacement = new PTextBox();
        final PCheckBox showMask = new PCheckBox("Show mask");
        final PButton applyMaskButton = new PButton("Apply mask");
        applyMaskButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                if (masked.getText().isEmpty()) return;

                String replaceChar = " ";
                if (!replacement.getText().isEmpty()) replaceChar = replacement.getText().substring(0, 1);
                maskedTextBox.applyMask(masked.getText(), showMask.getValue(), replaceChar);
            }
        });
        masked.setPlaceholder("({{000}}) {{000}}.{{0000}}");
        replacement.setWidth("10px");

        final PHorizontalPanel maskPanel = new PHorizontalPanel();
        maskPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        maskPanel.add(masked);
        maskPanel.add(maskedTextBox);
        maskPanel.add(replacement);
        maskPanel.add(showMask);
        maskPanel.add(applyMaskButton);

        panel.add(new PLabel("Password text box:"));
        panel.add(passwordTextBox);
        panel.add(passwordTextBoxReadOnly);
        panel.add(new PLabel("Text area:"));
        panel.add(textArea);
        panel.add(maskPanel);

        panel.add(new PLabel("AddOn test (javascript reverse)"));
        final PTextBox boxToReverse = new PTextBox();
        new ReverseTextInput(boxToReverse);
        final PTerminalScheduledCommand deffered = new PTerminalScheduledCommand() {

            @Override
            protected void run() {
                panel.add(boxToReverse);
            }
        };
        deffered.schedule(1500);

        examplePanel.setWidget(panel);
    }
}
