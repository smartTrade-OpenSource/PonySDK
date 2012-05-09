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

import com.google.gwt.dom.client.Style.Unit;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.PScript.ExecutionCallback;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;

public class JavascriptPageActivity extends SamplePageActivity {

    private final List<String> commands = new ArrayList<String>();
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

        inputTextBox = new PTextBox();
        scroll = new PScrollPanel();
        history = new PFlowPanel();
        scroll.setWidget(history);

        final PDockLayoutPanel dock = new PDockLayoutPanel(Unit.PX);
        dock.addSouth(inputTextBox, 30);
        dock.add(scroll);

        history.add(new PLabel("> With PonySDK, you can also execute native javascript"));
        history.add(new PLabel("> Try it out.. "));

        inputTextBox.addKeyUpHandler(new PKeyUpFilterHandler(PKeyCodes.ENTER, PKeyCodes.DOWN, PKeyCodes.UP) {

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
        });

        examplePanel.setWidget(dock);
    }

    protected void executeJS(final String js) {
        commands.add(js);
        history.add(new PLabel("> " + js));
        inputTextBox.setText("");
        PScript.get().execute(js, new ExecutionCallback() {

            @Override
            public void onSuccess(final String msg) {
                history.add(new PLabel(msg));
                scroll.scrollToBottom();
            }

            @Override
            public void onFailure(final String msg) {
                final PLabel lbl = new PLabel(msg);
                lbl.setStyleProperty("color", "red");
                history.add(lbl);
                scroll.scrollToBottom();
            }
        });
    }

}
