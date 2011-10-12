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
 */package com.ponysdk.ui.server.basic;

import java.util.List;

import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.ui.server.basic.event.HasPClickHandlers;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PHasText;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Update;

public class PLabel extends PWidget implements PHasText, HasPClickHandlers {

    private String text;

    public PLabel() {
    }

    public PLabel(String text) {
        setText(text);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.LABEL;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
        final Update update = new Update(getID());
        update.setMainPropertyValue(PropertyKey.TEXT, text);
        getPonySession().stackInstruction(update);
    }

    @Override
    public HandlerRegistration addClickHandler(PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    @Override
    public List<PClickHandler> getClickHandlers() {
        return getHandlerList(PClickEvent.TYPE, this);
    }

}
