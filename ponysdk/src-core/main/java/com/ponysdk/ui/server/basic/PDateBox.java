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
 */

package com.ponysdk.ui.server.basic;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.server.basic.event.PHasValue;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

public class PDateBox extends PFocusWidget implements PHasValue<Date>, PValueChangeHandler<Date>, PKeyPressHandler {

    private static final Logger log = LoggerFactory.getLogger(PDateBox.class);

    private final List<PValueChangeHandler<Date>> handlers = new ArrayList<PValueChangeHandler<Date>>();

    private Date date;

    private DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    private String pattern;

    public PDateBox() {
        this(null);
    }

    public PDateBox(final String text) {
        final AddHandler addHandler = new AddHandler(getID(), HandlerType.DATE_VALUE_CHANGE_HANDLER);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.DATEBOX;
    }

    @Override
    public void onEventInstruction(final EventInstruction e) {
        if (HandlerType.DATE_VALUE_CHANGE_HANDLER.equals(e.getHandlerType())) {
            final String data = e.getMainProperty().getValue();
            Date date = null;
            if (data != null) {
                try {
                    date = getDateFormat().parse(data);
                } catch (final ParseException ex) {
                    log.error("Cannot parse the date", ex);
                }
            }
            onValueChange(new PValueChangeEvent<Date>(this, date));
        } else {
            super.onEventInstruction(e);
        }
    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<Date> handler) {
        handlers.add(handler);
    }

    @Override
    public void removeValueChangeHandler(final PValueChangeHandler<Date> handler) {
        handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<Date>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Date> event) {
        this.date = event.getValue();
        for (final PValueChangeHandler<Date> handler : handlers) {
            handler.onValueChange(event);
        }

    }

    public void setFormat(final int style) {
        this.dateFormat = DateFormat.getDateInstance(style);

        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.DATE_FORMAT, style);
        getPonySession().stackInstruction(update);
    }

    public void setFormat(final String pattern) {
        this.pattern = pattern;
        this.dateFormat = new SimpleDateFormat(pattern);
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.DATE_FORMAT_PATTERN, pattern);
        getPonySession().stackInstruction(update);
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public void setTimeZone(final TimeZone timeZone) {
        dateFormat.setTimeZone(timeZone);
    }

    @Override
    public Date getValue() {
        return date;
    }

    @Override
    public void setValue(final Date date) {
        this.date = date;
        final Update update = new Update(getID());
        update.getMainProperty().setProperty(PropertyKey.VALUE, date != null ? dateFormat.format(date) : null);
        getPonySession().stackInstruction(update);
    }

    @Override
    public void onKeyPress(final int keyCode) {}

    public String getPattern() {
        return pattern;
    }

}
