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

import java.util.Calendar;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class CookiePageActivity extends PageActivity {

    public CookiePageActivity() {
        super("Cookie", "Basics UI Components");
    }

    @Override
    protected void onInitialization() {
    }

    @Override
    protected void onShowPage(Place place) {
    }

    @Override
    protected void onLeavingPage() {
    }

    @Override
    protected void onFirstShowPage() {
        final PVerticalPanel layout = new PVerticalPanel();
        pageView.getBody().setWidget(layout);

        final PButton button = new PButton("set cookie");
        final PButton remove = new PButton("remove cookie");
        final PTextBox name = new PTextBox("cookie name");
        final PTextBox value = new PTextBox("cookie value");
        final PLabel added = new PLabel();
        remove.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                PonySession.getCurrent().getCookies().removeCookie(name.getText());
            }
        });
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                final Calendar instance = Calendar.getInstance();
                instance.add(Calendar.DAY_OF_MONTH, 1);
                PonySession.getCurrent().getCookies().setCookie(name.getText(), value.getText(), instance.getTime());
                added.setText("added cookie # " + name.getText() + " having value : " + value.getText());
            }
        });
        layout.add(button);
        layout.add(remove);
        layout.add(name);
        layout.add(value);
        layout.add(added);
    }
}
