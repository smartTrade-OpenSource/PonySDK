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

import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.form.renderer.OracleListBoxRenderer;

public class OraclePageActivity extends PageActivity {

    public OraclePageActivity() {
        super("Oracle", "Basics UI Components");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(Place place) {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {

        final PVerticalPanel layout = new PVerticalPanel();
        pageView.getBody().setWidget(layout);
        final OracleListBoxRenderer listBox = new OracleListBoxRenderer(3, "List");

        listBox.addItem("AABBBAAA", "AAAAAAAA");
        listBox.addItem("AAAAAAAA1", "AAAAAAAA1");
        listBox.addItem("AABBBAAA2", "AAAAAAAA2");
        listBox.addItem("AAAAAAAA3", "AAAAAAAA3");
        listBox.addItem("AABBBAAA4", "AAAAAAAA4");
        listBox.addItem("BBBBBBAA", "BBBBBBBBB");
        listBox.addItem("CCCCCCCCC", "CCCCCCCCC");
        listBox.addItem("DDDDDDDDD", "DDDDDDDDD");
        listBox.addItem("EEEEEEEEE", "EEEEEEEEE");
        listBox.addItem("FFFFFFFFF", "FFFFFFFFF");
        final PButton child = new PButton("add item");
        final PTextBox textBox = new PTextBox();
        child.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                final String text = textBox.getText();
                if (text != null) listBox.addItem(text, text);
            }
        });
        layout.add(child);
        layout.add(textBox);

        layout.add(listBox.render(null).asWidget());
    }
}
