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

import com.ponysdk.ui.server.basic.event.PHasHTML;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * An entry in a {@link PMenuBar}. Menu items can either fire a {@link PCommand} when they are clicked, or
 * open a cascading sub-menu. Each menu item is assigned a unique DOM id in order to support ARIA. See
 * {com.google.gwt.user.client.ui.Accessibility} for more information.
 */
public class PMenuItem extends PWidget implements PHasHTML {

    private String text;

    private String html;

    private PMenuBar subMenu;

    private PCommand cmd;

    private boolean enabled;

    public PMenuItem(final String text, final boolean asHTML, final PCommand cmd) {
        this(text, asHTML);
        setCommand(cmd);
    }

    public PMenuItem(final String text, final boolean asHTML, final PMenuBar subMenu) {
        this(text, asHTML);
        setSubMenu(subMenu);
    }

    public PMenuItem(final String text, final PCommand cmd) {
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
        if (asHTML) setHTML(text);
        else setText(text);
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
        saveUpdate(Model.TEXT, text);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        this.html = html;
        saveUpdate(Model.HTML, html);
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        saveUpdate(Model.ENABLED, enabled);
    }

    private void setSubMenu(final PMenuBar subMenu) {
        this.subMenu = subMenu;
        saveAdd(subMenu.getID(), ID);
    }

    public void setCommand(final PCommand cmd) {
        this.cmd = cmd;
        saveAddHandler(Model.HANDLER_COMMAND);
    }

    @Override
    public void onClientData(final JSONObject event) throws JSONException {
        String handlerKey = null;
        if (event.has(HANDLER.KEY)) {
            handlerKey = event.getString(HANDLER.KEY);
        }
        if (HANDLER.KEY_.COMMAND.equals(handlerKey)) {
            cmd.execute();
        } else {
            super.onClientData(event);
        }
    }

    public PMenuBar getSubMenu() {
        return subMenu;
    }

    public PCommand getCmd() {
        return cmd;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
