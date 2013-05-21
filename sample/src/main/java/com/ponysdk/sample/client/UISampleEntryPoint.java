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

package com.ponysdk.sample.client;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.spring.client.SpringEntryPoint;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PNumberTextBox;
import com.ponysdk.ui.server.basic.PRootLayoutPanel;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class UISampleEntryPoint extends SpringEntryPoint implements EntryPoint, UserLoggedOutHandler, InitializingActivity {

    public static final String USER = "user";

    // private static Logger log = LoggerFactory.getLogger(UISampleEntryPoint.class);

    @Override
    public void start(final UIContext uiContext) {
        if (uiContext.getApplicationAttribute(USER) == null) uiContext.getHistory().newItem("", false);

        script();

        start();
    }

    private void script() {
        PScript.get().execute("less.watch();");
        final StringBuilder builder = new StringBuilder();
        builder.append("less.watch();");

        PScript.get().execute(builder.toString());
    }

    @Override
    public void restart(final UIContext session) {
        if (session.getApplicationAttribute(USER) == null) session.getHistory().newItem("", false);

        script();

        start();
    }

    private void start() {
        final PFlowPanel flow = new PFlowPanel();

        final PNumberTextBox nb2 = buildNB(new PNumberTextBox.Options().withMin(1d).withMax(100d));
        final PNumberTextBox nb3 = buildNB(new PNumberTextBox.Options().withMin(0d).withMax(10d).withStep(0.01d).withNumberFormat(2));
        flow.add(buildNB(new PNumberTextBox.Options()));
        flow.add(nb2);
        flow.add(nb3);

        final PButton b = new PButton("set value");
        b.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                nb2.setValue("5");
                nb3.setValue("5");
            }
        });
        flow.add(b);

        PRootLayoutPanel.get().add(flow);
    }

    private PNumberTextBox buildNB(final PNumberTextBox.Options options) {
        final PNumberTextBox nb = new PNumberTextBox(options);
        nb.addValueChangeHandler(new PValueChangeHandler<String>() {

            @Override
            public void onValueChange(final PValueChangeEvent<String> event) {
                System.out.println("New value: " + event.getValue());
            }
        });

        nb.setHeight("35px");

        return nb;
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }

    @Override
    public void afterContextInitialized() {
        eventBus.addHandler(UserLoggedOutEvent.TYPE, this);
    }

}
