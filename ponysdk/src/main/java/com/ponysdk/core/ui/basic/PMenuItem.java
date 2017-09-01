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

package com.ponysdk.core.ui.basic;

import java.util.Objects;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PHasHTML;
import com.ponysdk.core.writer.ModelWriter;

/**
 * An entry in a {@link PMenuBar}. Menu items can either fire a {@link Runnable} when they are
 * clicked, or open a cascading sub-menu. Each menu item is
 * assigned a unique DOM id in order to support ARIA.
 */
public class PMenuItem extends PMenuSubElement implements PHasHTML {

    private String text;

    private String html;

    private PMenuBar subMenu;

    private Runnable cmd;

    private boolean enabled;

    protected PMenuItem(final String text, final boolean asHTML, final Runnable cmd) {
        this(text, asHTML);
        setCommand(cmd);
    }

    protected PMenuItem(final String text, final boolean asHTML, final PMenuBar subMenu) {
        this(text, asHTML);
        setSubMenu(subMenu);
    }

    protected PMenuItem(final String text, final Runnable cmd) {
        this(text, false);
        setCommand(cmd);
    }

    protected PMenuItem(final String text) {
        this(text, false);
    }

    protected PMenuItem(final String text, final PMenuBar subMenu) {
        this(text, false);
        setSubMenu(subMenu);
    }

    protected PMenuItem(final String text, final boolean asHTML) {
        if (asHTML) this.html = text;
        else this.text = text;
    }

    @Override
    protected void init0() {
        super.init0();
        if (subMenu != null) subMenu.attach(window, frame);
    }

    @Override
    protected void enrichOnInit(final ModelWriter writer) {
        super.enrichOnInit(writer);
        if (html != null) writer.write(ServerToClientModel.HTML, html);
        else writer.write(ServerToClientModel.TEXT, text);
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
        if (Objects.equals(this.text, text)) return;
        this.text = text;
        this.html = null;
        saveUpdate(ServerToClientModel.TEXT, text);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        if (Objects.equals(this.html, html)) return;
        this.html = html;
        this.text = null;
        saveUpdate(ServerToClientModel.HTML, html);
    }

    public void setCommand(final Runnable cmd) {
        this.cmd = cmd;
        saveAddHandler(HandlerModel.HANDLER_COMMAND);
    }

    @Override
    public void onClientData(final JsonObject event) {
        if (!isVisible()) return;
        if (event.containsKey(ClientToServerModel.HANDLER_COMMAND.toStringValue())) {
            cmd.run();
        } else {
            super.onClientData(event);
        }
    }

    public PMenuBar getSubMenu() {
        return subMenu;
    }

    private void setSubMenu(final PMenuBar subMenu) {
        this.subMenu = subMenu;
        subMenu.saveAdd(subMenu.getID(), ID);
        subMenu.attach(window, frame);
    }

    public Runnable getCmd() {
        return cmd;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        if (Objects.equals(this.enabled, enabled)) return;
        this.enabled = enabled;
        saveUpdate(ServerToClientModel.ENABLED, enabled);
    }

}
