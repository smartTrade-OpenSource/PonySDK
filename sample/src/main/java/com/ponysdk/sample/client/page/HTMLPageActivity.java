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

import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class HTMLPageActivity extends SamplePageActivity {

    public HTMLPageActivity() {
        super("HTML", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel verticalPanel = new PVerticalPanel();
        verticalPanel.setSpacing(10);

        final PHTML htmlBold = new PHTML("<b>Pony Bold</b> and <font color='red'>Pony Red</font> using HTML");
        final PHTML htmlClickable = new PHTML("<span style='cursor: pointer;border: 1px solid black;color:white;background-color:gray;margin:5px;padding:10px'>click me!</span> using HTML and CSS");

        htmlClickable.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                PNotificationManager.showHumanizedNotification("HTML clicked!");
            }
        });

        final PCheckBox checkBox = new PCheckBox();
        checkBox.setHTML("<font color='blue'>Pony-SDK</font>");

        verticalPanel.add(htmlBold);
        verticalPanel.setCellWidth(htmlBold, "400px");
        verticalPanel.setCellHeight(htmlBold, "50px");

        verticalPanel.add(htmlClickable);
        verticalPanel.setCellWidth(htmlClickable, "400px");
        verticalPanel.setCellHeight(htmlClickable, "50px");

        verticalPanel.add(checkBox);
        verticalPanel.setCellWidth(checkBox, "400px");
        verticalPanel.setCellHeight(checkBox, "50px");

        examplePanel.setWidget(verticalPanel);
    }
}
