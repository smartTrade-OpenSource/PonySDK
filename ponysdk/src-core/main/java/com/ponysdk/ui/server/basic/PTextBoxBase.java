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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ponysdk.ui.server.basic.event.HasPValueChangeHandlers;
import com.ponysdk.ui.server.basic.event.PHasText;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTextBoxBase extends PFocusWidget implements PHasText, HasPValueChangeHandlers<String>, PValueChangeHandler<String> {

    private final List<PValueChangeHandler<String>> handlers = new ArrayList<PValueChangeHandler<String>>();

    private String text;

    public PTextBoxBase() {
        this(null);
    }

    public PTextBoxBase(String text) {
        super();
        setText(text);
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.STRING_VALUE_CHANGE_HANDLER);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    public void onEventInstruction(EventInstruction e) {
        if (HandlerType.STRING_VALUE_CHANGE_HANDLER.equals(e.getHandlerType())) {
            onValueChange(e.getMainProperty().getValue());
        } else {
            super.onEventInstruction(e);
        }
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.TEXTBOX;
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
    public void addValueChangeHandler(PValueChangeHandler<String> handler) {
        handlers.add(handler);
    }

    @Override
    public void removeValueChangeHandler(PValueChangeHandler<String> handler) {
        handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<String>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    @Override
    public void onValueChange(String value) {
        this.text = value;
        for (final PValueChangeHandler<String> handler : handlers) {
            handler.onValueChange(value);
        }
    }

    public void setSize(int maxCharacterLength) {
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.SIZE, maxCharacterLength + "");
        getPonySession().stackInstruction(update);
    }

}
