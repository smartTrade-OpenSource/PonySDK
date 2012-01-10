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

package com.ponysdk.ui.server.addon;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAddOn;
import com.ponysdk.ui.server.basic.PDecoratedPopupPanel;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.addon.dialogbox.PCDialogBoxAddon;
import com.ponysdk.ui.terminal.instruction.Update;

public class PDialogBox extends PDecoratedPopupPanel implements PAddOn {

    private String text;

    private IsPWidget closeWidget;

    private boolean closable;

    private IsPWidget contentWidget;

    public PDialogBox() {
        this(false);
    }

    public PDialogBox(boolean closable) {
        setClosable(closable);
        setStyleName("pony-DialogBox");
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.ADDON;
    }

    public void setText(String text) {
        this.text = text;
        final Update update = new Update(ID);
        update.getMainProperty().setProperty(PropertyKey.POPUP_TEXT, text);
        getPonySession().stackInstruction(update);
    }

    public void setCloseWidget(IsPWidget closeWidget) {
        this.closeWidget = closeWidget;
        final Update update = new Update(ID);
        update.getMainProperty().setProperty(PropertyKey.DIALOG_BOX_CLOSE_WIDGET, closeWidget.asWidget().getID());
        getPonySession().stackInstruction(update);
    }

    public void setClosable(boolean closable) {
        this.closable = closable;
        final Update update = new Update(ID);
        update.getMainProperty().setProperty(PropertyKey.DIALOG_BOX_CLOSABLE, closable);
        getPonySession().stackInstruction(update);
    }

    public String getText() {
        return text;
    }

    public IsPWidget getCloseWidget() {
        return closeWidget;
    }

    public boolean isClosable() {
        return closable;
    }

    public IsPWidget getContentWidget() {
        return contentWidget;
    }

    @Override
    public String getSignature() {
        return PCDialogBoxAddon.SIGNATURE;
    }

}
