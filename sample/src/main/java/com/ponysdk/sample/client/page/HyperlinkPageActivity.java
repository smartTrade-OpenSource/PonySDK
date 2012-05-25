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

import com.ponysdk.impl.webapplication.page.place.PagePlace;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class HyperlinkPageActivity extends SamplePageActivity {

    public HyperlinkPageActivity() {
        super("Hyperlink", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();
        panel.setSpacing(10);

        panel.add(new PLabel("Choose a section:"));

        final PAnchor checkBoxAnchor = new PAnchor("CheckBox");
        checkBoxAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                goTo(new PagePlace("CheckBox"));
            }
        });
        panel.add(checkBoxAnchor);

        final PAnchor radioButtonAnchor = new PAnchor("RadioButton");
        radioButtonAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                goTo(new PagePlace("Radio Button"));
            }
        });
        panel.add(radioButtonAnchor);

        final PAnchor basicButtonAnchor = new PAnchor("BasicButton");
        basicButtonAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                goTo(new PagePlace("Basic Button"));
            }
        });
        panel.add(basicButtonAnchor);

        final PAnchor customButtonAnchor = new PAnchor("CustomButton");
        customButtonAnchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                goTo(new PagePlace("Custom Button"));
            }
        });
        panel.add(customButtonAnchor);

        final PAnchor fileUpload = new PAnchor("FileUpload");
        fileUpload.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                goTo(new PagePlace("File Upload"));
            }
        });
        panel.add(fileUpload);

        examplePanel.setWidget(panel);
    }
}
