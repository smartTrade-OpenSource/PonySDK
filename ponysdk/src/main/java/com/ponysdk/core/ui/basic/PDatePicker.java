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

package com.ponysdk.core.ui.basic;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.terminal.ui.PTDatePicker;
import com.ponysdk.core.ui.basic.event.PShowRangeEvent;
import com.ponysdk.core.ui.basic.event.PShowRangeHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

public class PDatePicker extends PWidget implements HasPValue<Date>, PValueChangeHandler<Date> {

    private final Set<PValueChangeHandler<Date>> handlers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<PShowRangeHandler<Date>> showRangeHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Date date;

    private int year = -1;
    private int month = -1;
    private int day = -1;

    protected PDatePicker() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DATEPICKER;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE.toStringValue())) {
            final long data = jsonObject.getJsonNumber(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE.toStringValue()).longValue();
            Date date = null;
            if (data != -1) date = new Date(data);
            year = jsonObject.getInt(ClientToServerModel.YEAR.toStringValue());
            month = jsonObject.getInt(ClientToServerModel.MONTH.toStringValue());
            day = jsonObject.getInt(ClientToServerModel.DAY.toStringValue());
            onValueChange(new PValueChangeEvent<>(this, date));
        } else if (jsonObject.containsKey(ClientToServerModel.HANDLER_SHOW_RANGE.toStringValue())) {
            final long start = jsonObject.getJsonNumber(ClientToServerModel.START_DATE.toStringValue()).longValue();
            final long end = jsonObject.getJsonNumber(ClientToServerModel.END_DATE.toStringValue()).longValue();
            final Date sd = new Date(start);
            final Date ed = new Date(end);

            // TODO nicolas Use date ???

            final PShowRangeEvent<Date> showRangeEvent = new PShowRangeEvent<>(this, sd, ed);
            for (final PShowRangeHandler<Date> handler : showRangeHandlers) {
                handler.onShowRange(showRangeEvent);
            }
        } else {
            super.onClientData(jsonObject);
        }

    }

    @Override
    public void addValueChangeHandler(final PValueChangeHandler<Date> handler) {
        handlers.add(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<Date> handler) {
        return handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<Date>> getValueChangeHandlers() {
        return Collections.unmodifiableCollection(handlers);
    }

    public Collection<PShowRangeHandler<Date>> getShowRangeHandlers() {
        return Collections.unmodifiableCollection(showRangeHandlers);
    }

    public void addShowRangeHandler(final PShowRangeHandler<Date> handler) {
        showRangeHandlers.add(handler);
    }

    public void removeShowRangeHandler(final PShowRangeHandler<Date> handler) {
        showRangeHandlers.remove(handler);
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Date> event) {
        this.date = event.getData();
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
        if (Objects.equals(this.date, date)) return;
        this.date = date;
        saveUpdate(writer -> writer.write(ServerToClientModel.DATE, date != null ? date.getTime() : -1));
    }

    public void setCurrentMonth(final Date date) {
        saveUpdate(writer -> writer.write(ServerToClientModel.TIME, date != null ? date.getTime() : -1));
    }

    /**
     * Sets a visible date to be enabled or disabled. This is only set until the
     * next time the DatePicker is refreshed.
     */
    public final void setTransientEnabledOnDates(final boolean enabled, final Collection<Date> dates) {
        final String encodedDates = dateToString(dates);
        if (encodedDates != null && !encodedDates.isEmpty()) {
            saveUpdate((writer) -> {
                writer.write(ServerToClientModel.DATE_ENABLED, encodedDates);
                writer.write(ServerToClientModel.ENABLED, enabled);
            });
        }
    }

    /**
     * Add a style name to the given dates.
     */
    public void addStyleToDates(final String styleName, final Collection<Date> dates) {
        final String encodedDates = dateToString(dates);
        if (encodedDates != null && !encodedDates.isEmpty()) {
            saveUpdate((writer) -> {
                writer.write(ServerToClientModel.ADD_DATE_STYLE, encodedDates);
                writer.write(ServerToClientModel.STYLE_NAME, styleName);
            });
        }
    }

    /**
     * Removes the styleName from the given dates (even if it is transient).
     */
    public void removeStyleFromDates(final String styleName, final Collection<Date> dates) {
        final String encodedDates = dateToString(dates);
        if (encodedDates != null && !encodedDates.isEmpty()) {
            saveUpdate((writer) -> {
                writer.write(ServerToClientModel.REMOVE_DATE_STYLE, encodedDates);
                writer.write(ServerToClientModel.STYLE_NAME, styleName);
            });
        }
    }

    public void setYearArrowsVisible(final boolean visible) {
        saveUpdate((writer) -> writer.write(ServerToClientModel.YEAR_ARROWS_VISIBLE, visible));
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

    private static final String dateToString(final Collection<Date> dates) {
        if (dates != null && !dates.isEmpty()) {
            final StringBuilder asString = new StringBuilder();
            final Iterator<Date> it = dates.iterator();
            while (it.hasNext()) {
                asString.append(String.valueOf(it.next().getTime()));
                if (it.hasNext()) asString.append(PTDatePicker.DATE_SEPARATOR);
            }
            return asString.toString();
        } else {
            return null;
        }
    }

}
