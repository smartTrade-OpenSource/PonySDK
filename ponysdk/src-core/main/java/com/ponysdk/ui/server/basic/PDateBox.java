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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.PKeyPressHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

/**
 * A text box that shows a {@link PDatePicker} when the user focuses on it. <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-DateBox</dt>
 * <dd>default style name</dd>
 * <dt>.dateBoxPopup</dt>
 * <dd>Applied to the popup around the PDatePicker</dd>
 * <dt>.dateBoxFormatError</dt>
 * <dd>Default style for when the date box has bad input.</dd>
 * </dl>
 */
public class PDateBox extends PFocusWidget implements HasPValue<Date>, PValueChangeHandler<Date>, PKeyPressHandler {

    private static final Logger log = LoggerFactory.getLogger(PDateBox.class);

    private final List<PValueChangeHandler<Date>> handlers = new ArrayList<PValueChangeHandler<Date>>();

    private Date date;

    private SimpleDateFormat dateFormat;

    public PDateBox() {
        this(null, new SimpleDateFormat());
    }

    public PDateBox(final SimpleDateFormat dateFormat) {
        this(null, dateFormat);
    }

    public PDateBox(final String text) {
        this(text, new SimpleDateFormat());
    }

    public PDateBox(final String text, final SimpleDateFormat dateFormat) {
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.DATE_VALUE_CHANGE_HANDLER);
        getPonySession().stackInstruction(addHandler);

        setDateFormat(dateFormat);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DATEBOX;
    }

    @Override
    public void onEventInstruction(final JSONObject e) throws JSONException {
        if (e.getString(HANDLER.KEY).equals(HANDLER.DATE_VALUE_CHANGE_HANDLER)) {
            final String data = e.getString(PROPERTY.VALUE);
            Date date = null;
            if (data != null) {
                try {
                    date = dateFormat.parse(data);
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

    public void setDateFormat(final SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;

        final Update update = new Update(getID());
        update.put(PROPERTY.DATE_FORMAT_PATTERN, dateFormat.toPattern());
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

    public String getDisplayedValue() {
        if (getValue() == null) return "";
        return getDateFormat().format(getValue());
    }

    @Override
    public void setValue(final Date date) {
        this.date = date;
        final Update update = new Update(getID());
        update.put(PROPERTY.VALUE, date != null ? dateFormat.format(date) : null);
        getPonySession().stackInstruction(update);
    }

    @Override
    public void onKeyPress(final int keyCode) {}

}
