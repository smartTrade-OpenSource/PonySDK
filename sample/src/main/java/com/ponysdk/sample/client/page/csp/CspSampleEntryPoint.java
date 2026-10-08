/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.sample.client.page.csp;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFunctionalLabel;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PRichTextArea;
import com.ponysdk.core.ui.basic.PRichTextToolbar;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.core.ui.main.EntryPoint;

/**
 * Entry point used by the Content Security Policy end-to-end test. It renders one widget per
 * feature that is known to need script evaluation or inline code, each tagged with a style name
 * the test looks up. The {@code csp-ready} label is added last: its presence means the whole page
 * was built.
 */
public class CspSampleEntryPoint implements EntryPoint {

    @Override
    public void start(final UIContext uiContext) {
        final PWindow main = PWindow.getMain();

        // PScript.call of the named function cspSampleEcho (defined in sample.js) with a callback
        final PLabel scriptResult = Element.newPLabel("pending");
        scriptResult.addStyleName("csp-script-result");
        main.add(scriptResult);
        PScript.call(main, "cspSampleEcho", new Object[] { "ping" }, new PScript.ExecutionCallback() {

            @Override
            public void onSuccess(final String msg) {
                scriptResult.setText(msg);
            }

            @Override
            public void onFailure(final String msg) {
                scriptResult.setText("failure:" + msg);
            }
        }, null);

        // PObject.callTerminalFunction of sampleCheck (defined in sample.js) on a checkbox input element
        final PElement input = Element.newInput();
        input.setAttribute("type", "checkbox");
        input.callTerminalFunction("sampleCheck");
        final PFlowPanel checkbox = Element.newPFlowPanel();
        checkbox.addStyleName("csp-checkbox");
        checkbox.add(input);
        main.add(checkbox);

        // TextFunction referencing the named function sampleJoin (defined in sample.js)
        final TextFunction textFunction = TextFunction.named(args -> args[0] + "-" + args[1], "sampleJoin");
        final PFunctionalLabel functionalLabel = Element.newPFunctionalLabel(textFunction);
        functionalLabel.addStyleName("csp-functional-label");
        main.add(functionalLabel);
        functionalLabel.setArgs("A", "B");

        // Clipped image
        final PImage image = Element.newPImage("images/pony.png", 0, 0, 16, 16);
        image.addStyleName("csp-clipped-image");
        main.add(image);

        // File upload
        final PFileUpload fileUpload = Element.newPFileUpload();
        fileUpload.addStyleName("csp-file-upload");
        main.add(fileUpload);

        // Rich text area and its toolbar
        final PRichTextArea richTextArea = Element.newPRichTextArea();
        richTextArea.addStyleName("csp-rich-text");
        main.add(richTextArea);
        final PRichTextToolbar richTextToolbar = Element.newPRichTextToolbar(richTextArea);
        main.add(richTextToolbar);

        // Disclosure panel and tree, with their default images
        final PDisclosurePanel disclosure = Element.newPDisclosurePanel("Disclosure");
        disclosure.addStyleName("csp-disclosure");
        main.add(disclosure);

        final PTree tree = Element.newPTree();
        tree.addStyleName("csp-tree");
        final PTreeItem root = tree.add("Root");
        root.add("Child");
        root.setState(true);
        main.add(tree);

        // History
        uiContext.getHistory().newItem("csp", false);

        // Added last: its presence means the whole page was built
        final PLabel ready = Element.newPLabel("csp-ready");
        ready.addStyleName("csp-ready");
        main.add(ready);
    }

}
