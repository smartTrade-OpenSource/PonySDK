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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PCheckBox extends PButtonBase implements HasPValue<Boolean>, PValueChangeHandler<Boolean> {

    private final List<PValueChangeHandler<Boolean>> handlers = new ArrayList<PValueChangeHandler<Boolean>>();

    private boolean value;

    private String text;

    private String html;

    public PCheckBox() {
        this(null);
    }

    public PCheckBox(final String text) {
        setText(text);
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.BOOLEAN_VALUE_CHANGE_HANDLER);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.CHECKBOX;
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<Boolean> handler) {
        handlers.add(handler);
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<Boolean> handler) {
        handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<Boolean>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    @Override
    public String getHTML() {
        return html;
    }

    @Override
    public void setHTML(final String html) {
        this.html = html;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.HTML, html);
        getPonySession().stackInstruction(update);
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(final String text) {
        this.text = text;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.TEXT, text);
        getPonySession().stackInstruction(update);
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public void setValue(final Boolean value) {
        this.value = value;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.VALUE, value);
        getPonySession().stackInstruction(update);
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Boolean> event) {
        this.value = event.getValue();
        for (final PValueChangeHandler<Boolean> handler : handlers) {
            handler.onValueChange(event);
        }
    }

    @Override
    public void onEventInstruction(final EventInstruction e) {
        if (HandlerType.BOOLEAN_VALUE_CHANGE_HANDLER.equals(e.getHandlerType())) {
            onValueChange(new PValueChangeEvent<Boolean>(this, e.getMainProperty().getBooleanValue()));
        } else {
            super.onEventInstruction(e);
        }
    }
}
