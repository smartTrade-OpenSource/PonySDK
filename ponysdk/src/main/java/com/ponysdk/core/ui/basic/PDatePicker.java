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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DateConverter;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PShowRangeEvent;
import com.ponysdk.core.ui.basic.event.PShowRangeHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;

// FIXME Need to manipulate LocalDate instead of Date to avoid timezone issues
public class PDatePicker extends PWidget implements HasPValue<Date>, PValueChangeHandler<Date> {

    private static final Logger log = LoggerFactory.getLogger(PDatePicker.class);

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private List<PValueChangeHandler<Date>> handlers;
    private List<PShowRangeHandler<Date>> showRangeHandlers;

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
    public void addValueChangeHandler(final PValueChangeHandler<Date> handler) {
        if (handlers == null) handlers = new ArrayList<>();
        handlers.add(handler);
    }

    @Override
    public boolean removeValueChangeHandler(final PValueChangeHandler<Date> handler) {
        return handlers != null && handlers.remove(handler);
    }

    @Override
    public Collection<PValueChangeHandler<Date>> getValueChangeHandlers() {
        return handlers != null ? Collections.unmodifiableCollection(handlers) : Collections.emptyList();
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Date> event) {
        this.date = event.getData();
        if (handlers != null) {
            for (final PValueChangeHandler<Date> handler : handlers) {
                handler.onValueChange(event);
            }
        }
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (!isVisible()) return;
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE.toStringValue())) {
            final String rawDate = jsonObject.getString(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE.toStringValue());

            Date date = null;
            if (rawDate != null) {
                try {
                    date = dateFormat.parse(rawDate);

                    final String[] values = rawDate.split("-");
                    year = Integer.parseInt(values[0]);
                    month = Integer.parseInt(values[1]);
                    day = Integer.parseInt(values[2]);
                } catch (final ParseException e) {
                    log.error("Can't parse the date : " + rawDate, e);
                }
            } else {
                year = -1;
                month = -1;
                day = -1;
            }
            onValueChange(new PValueChangeEvent<>(this, date));
        } else if (jsonObject.containsKey(ClientToServerModel.HANDLER_SHOW_RANGE.toStringValue())) {
            if (showRangeHandlers != null) {
                final String start = jsonObject.getString(ClientToServerModel.START_DATE.toStringValue());
                final String end = jsonObject.getString(ClientToServerModel.END_DATE.toStringValue());
                try {
                    // FIXME Need to be removed (but need to upgrade the Pony version and break commpatibility)
                    // final PShowRangeEvent<String> event = new PShowRangeEvent<>(this, start, end);
                    final PShowRangeEvent<Date> event = new PShowRangeEvent<>(this, dateFormat.parse(start), dateFormat.parse(end));
                    for (final PShowRangeHandler<Date> handler : showRangeHandlers) {
                        handler.onShowRange(event);
                    }
                } catch (final ParseException e) {
                    log.error("Can't parse the dates " + start + " or " + end, e);
                }
            }
        } else {
            super.onClientData(jsonObject);
        }
    }

    public void addShowRangeHandler(final PShowRangeHandler<Date> handler) {
        if (showRangeHandlers == null) showRangeHandlers = new ArrayList<>();
        showRangeHandlers.add(handler);
    }

    public boolean removeShowRangeHandler(final PShowRangeHandler<Date> handler) {
        return showRangeHandlers != null && showRangeHandlers.remove(handler);
    }

    public Collection<PShowRangeHandler<Date>> getShowRangeHandlers() {
        return showRangeHandlers != null ? Collections.unmodifiableCollection(showRangeHandlers) : Collections.emptyList();
    }

    @Override
    public Date getValue() {
        return date;
    }

    @Override
    public void setValue(final Date date) {
        if (Objects.equals(this.date, date)) return;
        this.date = date;
        saveUpdate(ServerToClientModel.DATE, DateConverter.toTimestamp(date));
    }

    public void setCurrentMonth(final Date date) {
        saveUpdate(ServerToClientModel.TIME, DateConverter.toTimestamp(date));
    }

    /**
     * Sets a visible date to be enabled or disabled. This is only set until the
     * next time the DatePicker is refreshed.
     */
    public final void setTransientEnabledOnDates(final boolean enabled, final Collection<Date> dates) {
        final Long[] encodedDates = DateConverter.encode(dates);
        if (encodedDates != null && encodedDates.length > 0) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.DATE_ENABLED, encodedDates);
                writer.write(ServerToClientModel.ENABLED, enabled);
            });
        }
    }

    /**
     * Add a style name to the given dates.
     */
    public void addStyleToDates(final String styleName, final Collection<Date> dates) {
        final Long[] encodedDates = DateConverter.encode(dates);
        if (encodedDates != null && encodedDates.length > 0) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.ADD_DATE_STYLE, encodedDates);
                writer.write(ServerToClientModel.STYLE_NAME, styleName);
            });
        }
    }

    /**
     * Removes the styleName from the given dates (even if it is transient).
     */
    public void removeStyleFromDates(final String styleName, final Collection<Date> dates) {
        final Long[] encodedDates = DateConverter.encode(dates);
        if (encodedDates != null && encodedDates.length > 0) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.REMOVE_DATE_STYLE, encodedDates);
                writer.write(ServerToClientModel.STYLE_NAME, styleName);
            });
        }
    }

    public void setYearArrowsVisible(final boolean visible) {
        saveUpdate(ServerToClientModel.YEAR_ARROWS_VISIBLE, visible);
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    /**
     * @deprecated Useless
     */
    @Deprecated(forRemoval = true, since = "2.8.2")
    public void setMonth(final int month) {
        // To be removed
    }

}
