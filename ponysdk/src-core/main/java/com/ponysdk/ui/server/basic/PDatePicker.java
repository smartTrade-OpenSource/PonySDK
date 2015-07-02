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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.ui.server.basic.event.PShowRangeEvent;
import com.ponysdk.ui.server.basic.event.PShowRangeHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

public class PDatePicker extends PWidget implements HasPValue<Date>, PValueChangeHandler<Date> {

    private final ListenerCollection<PValueChangeHandler<Date>> handlers = new ListenerCollection<PValueChangeHandler<Date>>();
    private final ListenerCollection<PShowRangeHandler<Date>> showRangeHandlers = new ListenerCollection<PShowRangeHandler<Date>>();

    private Date date;

    private int year = -1;
    private int month = -1;
    private int day = -1;

    public PDatePicker() {
        this(null);
    }

    public PDatePicker(final TimeZone timeZone) {
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.KEY_.DATE_VALUE_CHANGE_HANDLER);
        Txn.get().getTxnContext().save(addHandler);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DATEPICKER;
    }

    @Override
    public void onClientData(final JSONObject e) throws JSONException {
        if (e.getString(HANDLER.KEY).equals(HANDLER.KEY_.DATE_VALUE_CHANGE_HANDLER)) {
            final String data = e.getString(PROPERTY.VALUE);
            Date date = null;
            if (data != null && !data.isEmpty()) date = new Date(Long.parseLong(data));

            year = e.getInt(PROPERTY.YEAR);
            month = e.getInt(PROPERTY.MONTH);
            day = e.getInt(PROPERTY.DAY);
            onValueChange(new PValueChangeEvent<Date>(this, date));
        } else if (e.getString(HANDLER.KEY).equals(HANDLER.KEY_.SHOW_RANGE)) {
            final String start = e.getString(PROPERTY.START);
            final String end = e.getString(PROPERTY.END);
            final Date sd = new Date(Long.parseLong(start));
            final Date ed = new Date(Long.parseLong(end));

            final PShowRangeEvent<Date> showRangeEvent = new PShowRangeEvent<Date>(this, sd, ed);
            for (final PShowRangeHandler<Date> handler : showRangeHandlers) {
                handler.onShowRange(showRangeEvent);
            }
        } else {
            super.onClientData(e);
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

    public Collection<PShowRangeHandler<Date>> getShowRangeHandlers() {
        return Collections.unmodifiableCollection(showRangeHandlers);
    }

    public void addShowRangeHandler(final PShowRangeHandler<Date> handler) {
        if (showRangeHandlers.isEmpty()) {
            final AddHandler addHandler = new AddHandler(getID(), HANDLER.KEY_.SHOW_RANGE);
            Txn.get().getTxnContext().save(addHandler);
        }

        showRangeHandlers.add(handler);
    }

    public void removeShowRangeHandler(final PShowRangeHandler<Date> handler) {
        showRangeHandlers.remove(handler);
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Date> event) {
        this.date = event.getValue();
        for (final PValueChangeHandler<Date> handler : handlers) {
            handler.onValueChange(event);
        }
    }

    @Override
    public Date getValue() {
        return date;
    }

    @Override
    public void setValue(final Date date) {
        this.date = date;
        final Update update = new Update(getID());
        update.put(PROPERTY.VALUE, date != null ? Long.toString(date.getTime()) : "");
        Txn.get().getTxnContext().save(update);
    }

    public void setCurrentMonth(final Date date) {
        final Update update = new Update(getID());
        update.put(PROPERTY.MONTH, date != null ? Long.toString(date.getTime()) : "");
        Txn.get().getTxnContext().save(update);
    }

    /**
     * Sets a visible date to be enabled or disabled. This is only set until the next time the DatePicker is
     * refreshed.
     */
    public final void setTransientEnabledOnDates(final boolean enabled, final Collection<Date> dates) {
        final List<String> asString = dateToString(dates);
        final Update update = new Update(getID());
        update.put(PROPERTY.DATE_ENABLED, asString);
        update.put(PROPERTY.ENABLED, enabled);
        Txn.get().getTxnContext().save(update);
    }

    /**
     * Add a style name to the given dates.
     */
    public void addStyleToDates(final String styleName, final Collection<Date> dates) {
        final List<String> asString = dateToString(dates);
        final Update update = new Update(getID());
        update.put(PROPERTY.ADD_DATE_STYLE, asString);
        update.put(PROPERTY.STYLE_NAME, styleName);
        Txn.get().getTxnContext().save(update);
    }

    /**
     * Removes the styleName from the given dates (even if it is transient).
     */
    public void removeStyleFromDates(final String styleName, final Collection<Date> dates) {
        final List<String> asString = dateToString(dates);
        final Update update = new Update(getID());
        update.put(PROPERTY.REMOVE_DATE_STYLE, asString);
        update.put(PROPERTY.STYLE_NAME, styleName);
        Txn.get().getTxnContext().save(update);
    }

    private List<String> dateToString(final Collection<Date> dates) {
        final List<String> asString = new ArrayList<String>();
        for (final Date d : dates) {
            asString.add(Long.toString(d.getTime()));
        }
        return asString;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(final int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public int getDay() {
        return day;
    }

}
