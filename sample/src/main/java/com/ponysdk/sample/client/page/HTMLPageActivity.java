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
import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class HTMLPageActivity extends PageActivity {

    public HTMLPageActivity() {
        super("HTML", "Basics UI Components");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onShowPage(final Place place) {
        final PVerticalPanel verticalPanel = new PVerticalPanel();

        final PHTML htmlBold = new PHTML("<b>Pony Bold</b>");
        final PHTML htmlRed = new PHTML("<font color='red'>Pony Redb</font>");
        final PHTML htmlClickable = new PHTML("<span style='border: 3px solid black;color:white;background-color:gray;margin:5px;padding:10px;display:block'>click me!</span>");

        htmlClickable.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                PNotificationManager.showTrayNotification("HTML clicked!");
            }
        });

        verticalPanel.add(htmlBold);
        verticalPanel.add(htmlRed);
        verticalPanel.add(htmlClickable);
        verticalPanel.add(new PCheckBox("Pony-SDK"));

        pageView.getBody().setWidget(verticalPanel);
    }

    @Override
    protected void onFirstShowPage() {

    }
}
