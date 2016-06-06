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

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PRichTextArea;
import com.ponysdk.core.ui.basic.PRichTextToolbar;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

public class RichTextAreaPageActivity extends SamplePageActivity {

    public RichTextAreaPageActivity() {
        super("RichText Area", "Text Input");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PScrollPanel scroll = new PScrollPanel();
        final PRichTextArea richTextArea = new PRichTextArea();
        final PRichTextToolbar richTextToolbar = new PRichTextToolbar(richTextArea);

        richTextArea.addValueChangeHandler(new PValueChangeHandler<String>() {

            @Override
            public void onValueChange(final PValueChangeEvent<String> event) {
                System.err.println(richTextArea.getHTML());
            }
        });

        final PFlowPanel flow = new PFlowPanel();
        flow.add(new PLabel("Edit rich content"));
        flow.add(richTextToolbar);
        flow.add(richTextArea);
        flow.add(buildCustomToolbar(richTextArea));
        scroll.setWidget(flow);
        examplePanel.setWidget(scroll);

        richTextArea.setWidth("100%");
    }

    private PWidget buildCustomToolbar(final PRichTextArea richTextArea) {

        final PTextBox color = new PTextBox();
        color.setPlaceholder("Color");
        final PButton update = new PButton("Set back color");
        update.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final String c = color.getValue();
                richTextArea.getFormatter().setBackColor(c);
            }
        });

        final PFlowPanel toolbar = new PFlowPanel();
        toolbar.add(color);
        toolbar.add(update);
        toolbar.setStyleProperty("padding-top", "15px");
        return toolbar;
    }
}
