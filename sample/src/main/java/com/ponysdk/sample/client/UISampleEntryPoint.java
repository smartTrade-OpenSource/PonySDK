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

import javax.json.JsonObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.statistic.TerminalDataReceiver;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.page.addon.LabelPAddOn;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PRootPanel;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler {

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput(new TerminalDataReceiver() {

            @Override
            public void onDataReceived(final PObject object, final JsonObject instruction) {
                System.err.println(object + "" + instruction);
            }
        });

        // PRootPanel.get().clear(true);

        final PLabel label = new PLabel("Coucou");
        // PRootPanel.get().add(label);

        final LabelPAddOn addon = new LabelPAddOn();
        addon.log(null);
        // PRootPanel.get().add(new LabelPAddOn());

        final PElement elt = new PElement("div");
        PRootPanel.get().add(elt);
        final LabelPAddOn addon2 = new LabelPAddOn(elt);
        addon2.log("Coucou");

        final PFlowPanel panel = new PFlowPanel();
        panel.add(new PElement("button"));
        panel.add(new PElement("button"));
        panel.add(new PElement("button"));
        panel.add(new PElement("button"));

        PRootPanel.get().add(panel);

        // uiContext.getHistory().newItem("", false);
    }

    @Override
    public void restart(final UIContext uiContext) {
        start(uiContext);
        // session.getHistory().newItem("", false);
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }

}
