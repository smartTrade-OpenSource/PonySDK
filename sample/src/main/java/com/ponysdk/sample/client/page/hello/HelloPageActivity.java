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

package com.ponysdk.sample.client.page.hello;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.client.place.HelloPlace;
import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class HelloPageActivity extends PageActivity {

    public HelloPageActivity() {
        super("Hello Page", "Category");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onShowPage(Place place) {
        if (place instanceof HelloPlace) {} else if (place instanceof HelloPagePopupPlace) {
            final HelloPagePopupPlace popupPanel = (HelloPagePopupPlace) place;
            PNotificationManager.notify(popupPanel.getContent());
        }
    }

    @Override
    protected void onFirstShowPage() {
        final PVerticalPanel verticalPanel = new PVerticalPanel();
        for (int i = 0; i < 10; i++) {
            verticalPanel.add(new PLabel("Test"));
        }
        final PButton openStreamButon = new PButton("open Stream");
        openStreamButon.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                final StreamResource streamResource = new StreamResource();
                streamResource.open(new StreamHandler() {

                    @Override
                    public void onStream(HttpServletRequest req, HttpServletResponse response) {
                        response.setContentType("text/text");
                        response.setHeader("Content-Disposition", "attachment; filename=" + "toto.txt");
                        PrintWriter printer;
                        try {
                            printer = response.getWriter();
                            printer.print("It rocks");
                            printer.flush();
                            printer.close();
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        verticalPanel.add(openStreamButon);
        pageView.getBody().setWidget(verticalPanel);
    }

    public HelloPagePopupPlace newPopupPlace(String content) {
        final HelloPagePopupPlace pagePlace = new HelloPagePopupPlace(this, content);
        return pagePlace;
    }
}
