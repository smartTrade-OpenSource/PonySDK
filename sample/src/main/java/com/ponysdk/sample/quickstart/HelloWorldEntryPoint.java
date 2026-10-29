/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.sample.quickstart;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

/** The quick start's screen (README.md, docs/guide/quick-start.md): run by HelloWorldMain. */
public class HelloWorldEntryPoint implements EntryPoint {

    @Override
    public void start(final UIContext uiContext) {
        final PLabel label = Element.newPLabel("Hello PonySDK!");
        final PTextBox name = Element.newPTextBox();
        final PButton button = Element.newPButton("Say hello");

        // Runs on the server: the click comes over the WebSocket, the new text goes back
        button.addClickHandler(event -> label.setText("Hello, " + (name.getText().isBlank() ? "World" : name.getText()) + "!"));

        final PFlowPanel root = Element.newPFlowPanel();
        root.add(label);
        root.add(name);
        root.add(button);
        PWindow.getMain().add(root);
    }
}
