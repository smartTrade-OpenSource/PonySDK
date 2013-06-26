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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTFocusWidget<W extends FocusWidget> extends PTWidget<W> {

    private boolean showLoadingOnRequest = false;

    private boolean enabledOnRequest = false;

    private boolean enabled = true;

    @Override
    public void update(final PTInstruction update, final UIService uiService) {

        if (update.containsKey(PROPERTY.LOADING_ON_REQUEST)) {
            showLoadingOnRequest = update.getBoolean(PROPERTY.LOADING_ON_REQUEST);
        } else if (update.containsKey(PROPERTY.ENABLED_ON_REQUEST)) {
            enabledOnRequest = update.getBoolean(PROPERTY.ENABLED_ON_REQUEST);
        } else if (update.containsKey(PROPERTY.END_OF_PROCESSING)) {
            if (showLoadingOnRequest) uiObject.removeStyleName("pony-Loading");
            if (!enabledOnRequest) uiObject.setEnabled(enabled);
        } else if (update.containsKey(PROPERTY.ENABLED)) {
            this.enabled = update.getBoolean(PROPERTY.ENABLED);
            uiObject.setEnabled(enabled);
        } else if (update.containsKey(PROPERTY.TABINDEX)) {
            uiObject.setTabIndex(update.getInt(PROPERTY.TABINDEX));
        } else if (update.containsKey(PROPERTY.FOCUSED)) {
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                @Override
                public void execute() {
                    uiObject.setFocus(update.getBoolean(PROPERTY.FOCUSED));
                }
            });
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    protected void triggerMouseEvent(final PTInstruction addHandler, final Widget widget, final int domHandlerType, final UIService uiService, final MouseEvent<?> event) {
        if (!enabledOnRequest) uiObject.setEnabled(false);
        if (showLoadingOnRequest) uiObject.addStyleName("pony-Loading");
        super.triggerMouseEvent(addHandler, widget, domHandlerType, uiService, event);
    }

}
