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
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PMenuItem extends PWidget implements PHasHTML {

    private String text;

    private String html;

    private PMenuBar subMenu;

    private PCommand cmd;

    private boolean enabled;

    public PMenuItem(String text, boolean asHTML, PCommand cmd) {
        this(text, asHTML);
        setCommand(cmd);
    }

    public PMenuItem(String text, boolean asHTML, PMenuBar subMenu) {
        this(text, asHTML);
        setSubMenu(subMenu);
    }

    public PMenuItem(String text, PCommand cmd) {
        this(text, false);
        setCommand(cmd);
    }

    public PMenuItem(String text) {
        this(text, false);
    }

    public PMenuItem(String text, PMenuBar subMenu) {
        this(text, false);
        setSubMenu(subMenu);
    }

    public PMenuItem(String text, boolean asHTML) {
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
    public void setText(String text) {
        this.text = text;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.TEXT, text);
        getPonySession().stackInstruction(update);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(String html) {
        this.html = html;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.HTML, html);
        getPonySession().stackInstruction(update);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.ENABLED, enabled);
        getPonySession().stackInstruction(update);
    }

    private void setSubMenu(PMenuBar subMenu) {
        this.subMenu = subMenu;
        final Add add = new Add(subMenu.getID(), getID());
        getPonySession().stackInstruction(add);
    }

    public void setCommand(PCommand cmd) {
        this.cmd = cmd;
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.COMMAND);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    public void onEventInstruction(EventInstruction event) {
        if (HandlerType.COMMAND.equals(event.getHandlerType())) {
            cmd.execute();
        } else {
            super.onEventInstruction(event);
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
