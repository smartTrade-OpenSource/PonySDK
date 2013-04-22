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

import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPasswordTextBox;
import com.ponysdk.ui.server.basic.PTextArea;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

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

        panel.add(new PLabel("Password text box:"));
        panel.add(passwordTextBox);
        panel.add(passwordTextBoxReadOnly);
        panel.add(new PLabel("Text area:"));
        panel.add(textArea);

        examplePanel.setWidget(panel);
    }
}
