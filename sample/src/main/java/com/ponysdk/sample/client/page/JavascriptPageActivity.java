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

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PScript.ExecutionCallback;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.model.PKeyCodes;

public class JavascriptPageActivity extends SamplePageActivity {

    private final List<String> commands = new ArrayList<>();
    private PFlowPanel history;

    private int commandIndex = 0;
    private PTextBox inputTextBox;
    private PScrollPanel scroll;

    public JavascriptPageActivity() {
        super("Javascript", "Extra");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        inputTextBox = Element.newPTextBox();
        scroll = Element.newPScrollPanel();
        history = Element.newPFlowPanel();
        scroll.setWidget(history);

        final PDockLayoutPanel dock = Element.newPDockLayoutPanel(PUnit.PX);
        dock.addSouth(inputTextBox, 30);
        dock.add(scroll);

        history.add(Element.newPLabel("> With PonySDK, you can also execute native javascript"));
        history.add(Element.newPLabel("> Try it out.. "));

        inputTextBox.addKeyUpHandler(new PKeyUpHandler() {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                if (PKeyCodes.ENTER.equals(keyUpEvent.getKeyCode())) {
                    executeJS(inputTextBox.getText());
                    commandIndex = 0;
                } else if (PKeyCodes.UP.equals(keyUpEvent.getKeyCode())) {
                    if (commandIndex < commands.size()) {
                        commandIndex++;
                        inputTextBox.setText(commands.get(commands.size() - commandIndex));
                    }
                } else if (PKeyCodes.DOWN.equals(keyUpEvent.getKeyCode())) {
                    if (commandIndex > 1) {
                        commandIndex--;
                        inputTextBox.setText(commands.get(commands.size() - commandIndex));
                    }
                }
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[]{PKeyCodes.ENTER, PKeyCodes.DOWN, PKeyCodes.UP};
            }
        });

        examplePanel.setWidget(dock);
    }

    protected void executeJS(final String js) {
        commands.add(js);
        history.add(Element.newPLabel("> " + js));
        inputTextBox.setText("");
        PScript.execute(PWindow.getMain(), js, new ExecutionCallback() {

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
        });
    }

}
