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

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PScript.ExecutionCallback;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;

public class JavascriptPageActivity extends SamplePageActivity {

    private PFlowPanel history;
    private PTextBox functionNameTextBox;
    private PTextBox argumentTextBox;
    private PScrollPanel scroll;

    public JavascriptPageActivity() {
        super("Javascript", "Extra");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        functionNameTextBox = Element.newPTextBox();
        functionNameTextBox.setPlaceholder("Function name (e.g. sampleLog)");
        argumentTextBox = Element.newPTextBox();
        argumentTextBox.setPlaceholder("Argument (a single string)");
        final PButton executeButton = Element.newPButton("Execute");
        executeButton.addClickHandler(event -> callFunction(functionNameTextBox.getText(), argumentTextBox.getText()));

        final PFlowPanel inputs = Element.newPFlowPanel();
        inputs.add(functionNameTextBox);
        inputs.add(argumentTextBox);
        inputs.add(executeButton);

        scroll = Element.newPScrollPanel();
        history = Element.newPFlowPanel();
        scroll.setWidget(history);

        final PDockLayoutPanel dock = Element.newPDockLayoutPanel(PUnit.PX);
        dock.addSouth(inputs, 100);
        dock.add(scroll);

        history.add(Element.newPLabel("> With PonySDK, you can call a named javascript function defined in the application scripts"));
        history.add(Element.newPLabel("> Try it out.. (e.g. sampleLog, cspSampleEcho or alert)"));

        examplePanel.setWidget(dock);
    }

    protected void callFunction(final String functionName, final String argument) {
        history.add(Element.newPLabel("> " + functionName + "(" + argument + ")"));
        PScript.call(PWindow.getMain(), functionName, new Object[] { argument }, new ExecutionCallback() {

            @Override
            public void onSuccess(final String msg) {
                history.add(Element.newPLabel(msg));
                scroll.scrollToBottom();
            }

            @Override
            public void onFailure(final String msg) {
                final PLabel lbl = Element.newPLabel(msg);
                lbl.setStyleProperty("color", "red");
                history.add(lbl);
                scroll.scrollToBottom();
            }
        }, null);
    }

}
