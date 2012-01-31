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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTFocusWidget extends PTWidget {

    private boolean showLoadingOnRequest = false;

    private boolean enabledOnRequest = false;

    private boolean enabled = true;

    @Override
    public void update(Update update, UIService uiService) {

        final Property mainProperty = update.getMainProperty();
        final PropertyKey mainPropertyKey = mainProperty.getKey();

        switch (mainPropertyKey) {
            case LOADING_ON_REQUEST:
                showLoadingOnRequest = mainProperty.getBooleanValue();
                break;
            case ENABLED_ON_REQUEST:
                enabledOnRequest = mainProperty.getBooleanValue();
                break;
            case END_OF_PROCESSING:
                if (showLoadingOnRequest) {
                    cast().removeStyleName("pony-Loading");
                }
                if (!enabledOnRequest) {
                    cast().setEnabled(enabled);
                }
                break;

            default:
                break;
        }

        for (final Property property : mainProperty.getChildProperties().values()) {
            final PropertyKey propertyKey = property.getKey();
            if (PropertyKey.ENABLED.equals(propertyKey)) {
                this.enabled = property.getBooleanValue();
                cast().setEnabled(enabled);
            } else if (PropertyKey.FOCUSED.equals(propertyKey)) {
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                    @Override
                    public void execute() {
                        cast().setFocus(property.getBooleanValue());
                    }
                });
            }
        }

        super.update(update, uiService);
    }

    @Override
    public com.google.gwt.user.client.ui.FocusWidget cast() {
        return (com.google.gwt.user.client.ui.FocusWidget) uiObject;
    }

    @Override
    protected void triggerOnClick(AddHandler addHandler, Widget widget, int domHandlerType, UIService uiService, ClickEvent event) {
        if (showLoadingOnRequest) {
            cast().setEnabled(false);
            cast().addStyleName("pony-Loading");

        }
        super.triggerOnClick(addHandler, widget, domHandlerType, uiService, event);
    }

}
