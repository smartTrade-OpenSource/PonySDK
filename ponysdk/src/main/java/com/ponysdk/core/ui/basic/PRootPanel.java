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

import jakarta.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.writer.ModelWriter;

/**
 * The panel to which all other widgets must ultimately be added. RootPanels are never created directly. Rather, they
 * are accessed via {@link PWindow#getPRootPanel(String)} .
 * <p>
 * Most applications will add widgets to the default root panel in their
 * {@link com.ponysdk.core.ui.main.EntryPoint#start(UIContext)} methods.
 * </p>
 */
public class PRootPanel extends PAbsolutePanel {

    private final String id;

    PRootPanel(final String id) {
        this.id = id;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        if (id != null) writer.write(ServerToClientModel.ROOT_ID, id);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ROOT_PANEL;
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (destroy) return;
        if (instruction.containsKey(ClientToServerModel.HANDLER_DOCUMENT_VISIBILITY.toStringValue())) {
            if (id == null) getWindow().onClientData(instruction);
        } else {
            super.onClientData(instruction);
        }
    }

}
