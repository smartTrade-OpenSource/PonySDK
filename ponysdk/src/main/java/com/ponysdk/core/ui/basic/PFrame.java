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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class PFrame extends PWidget {

    private final Map<String, PRootPanel> panelByZone = new HashMap<>(8);

    private String url;
    private boolean ready;

    protected PFrame(final String url) {
        this.url = url;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        if (url != null && !url.isEmpty()) writer.write(ServerToClientModel.URL, url);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FRAME;
    }

    public String getURL() {
        return url;
    }

    public void add(final PWidget child) {
        add(null, child);
    }

    public void add(final String zoneID, final PWidget child) {
        ensureRootPanel(zoneID).add(child);
    }

    private PRootPanel ensureRootPanel(final String zoneID) {
        PRootPanel rootPanel = panelByZone.get(zoneID);
        if (rootPanel == null) {
            rootPanel = new PRootPanel(zoneID);

            if (ready && isInitialized()) rootPanel.attach(getWindow(), this);

            panelByZone.put(zoneID, rootPanel);
        }
        return rootPanel;
    }

    @Override
    public void onClientData(final JsonObject event) {
        if (!isVisible()) return;
        if (event.containsKey(ClientToServerModel.HANDLER_OPEN.toStringValue())) {
            url = event.getString(ClientToServerModel.HANDLER_OPEN.toStringValue());
            ready = true;
            panelByZone.forEach((key, value) -> value.attach(this.getWindow(), this));
        } else {
            super.onClientData(event);
        }
    }

    public String dumpDOM() {
        String DOM = "<frame pid=\"" + ID + "\">";
        for (PRootPanel panel : panelByZone.values()) {
            DOM += panel.dumpDOM();
        }
        DOM += "</frame>";
        return DOM;
    }

}
