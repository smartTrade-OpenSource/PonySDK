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

package com.ponysdk.ui.server.basic;

import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

/**
 * An entry in a {@link PMenuBar}. Menu items can either fire a {@link PCommand}
 * when they are clicked, or open a cascading sub-menu. Each menu item is
 * assigned a unique DOM id in order to support ARIA. See
 * {com.google.gwt.user.client.ui.Accessibility} for more information.
 */
public class PMenuItem extends PMenuSubElement implements PHasHTML {

    private String text;

    private String html;

    private PMenuBar subMenu;

    private Runnable cmd;

    private boolean enabled;

    public PMenuItem(final String text, final boolean asHTML, final Runnable cmd) {
        this(text, asHTML);
        setCommand(cmd);
    }

    public PMenuItem(final String text, final boolean asHTML, final PMenuBar subMenu) {
        this(text, asHTML);
        setSubMenu(subMenu);
    }

    public PMenuItem(final String text, final Runnable cmd) {
        this(text, false);
        setCommand(cmd);
    }

    public PMenuItem(final String text) {
        this(text, false);
    }

    public PMenuItem(final String text, final PMenuBar subMenu) {
        this(text, false);
        setSubMenu(subMenu);
    }

    public PMenuItem(final String text, final boolean asHTML) {
        if (asHTML) this.html = text;
        else this.text = text;
    }

    @Override
    protected boolean attach(final int windowID) {
        final boolean result = super.attach(windowID);
        subMenu.attach(windowID);
        return result;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        if (html != null) parser.parse(ServerToClientModel.HTML, html);
        else parser.parse(ServerToClientModel.TEXT, text);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.MENU_ITEM;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(final String text) {
        this.text = text;
        saveUpdate(ServerToClientModel.TEXT, text);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        if (Objects.equals(this.html, html))
            return;
        this.html = html;
        saveUpdate(ServerToClientModel.HTML, this.html.replace("\"", "\\\""));
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        saveUpdate(ServerToClientModel.ENABLED, enabled);
    }

    private void setSubMenu(final PMenuBar subMenu) {
        this.subMenu = subMenu;
        subMenu.saveAdd(subMenu.getID(), ID);
        subMenu.attach(windowID);
    }

    public void setCommand(final Runnable cmd) {
        this.cmd = cmd;
        saveAddHandler(HandlerModel.HANDLER_COMMAND);
    }

    @Override
    public void onClientData(final JsonObject event) {
        final String handlerKeyKey = ClientToServerModel.HANDLER_KEY.toStringValue();
        if (event.containsKey(handlerKeyKey)) {
            final String handlerKey = event.getString(handlerKeyKey);
            if (ClientToServerModel.HANDLER_KEY_COMMAND.toStringValue().equals(handlerKey)) cmd.run();
            else super.onClientData(event);
        } else {
            super.onClientData(event);
        }
    }

    public PMenuBar getSubMenu() {
        return subMenu;
    }

    public Runnable getCmd() {
        return cmd;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
