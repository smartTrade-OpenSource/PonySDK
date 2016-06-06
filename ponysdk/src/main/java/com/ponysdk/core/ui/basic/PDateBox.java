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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A text box that shows a {@link PDatePicker} when the user focuses on it.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-DateBox</dt>
 * <dd>default style name</dd>
 * <dt>.dateBoxPopup</dt>
 * <dd>Applied to the popup around the PDatePicker</dd>
 * <dt>.dateBoxFormatError</dt>
 * <dd>Default style for when the date box has bad input.</dd>
 * </dl>
 */
public class PDateBox extends PFocusWidget implements HasPValue<Date>, PValueChangeHandler<Date> {

    private static final Logger log = LoggerFactory.getLogger(PDateBox.class);

    private static final String EMPTY = "";

    private List<PValueChangeHandler<Date>> handlers;

    private final PDatePicker datePicker;
    private Date date;
    private SimpleDateFormat dateFormat;

    public PDateBox() {
        this(new SimpleDateFormat("MM/dd/yyyy"));
    }

    public PDateBox(final SimpleDateFormat dateFormat) {
        this(new PDatePicker(), dateFormat);
    }

    public PDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat) {
        this.datePicker = picker;
        this.dateFormat = dateFormat;
        saveAdd(datePicker.getID(), ID);
    }

    @Override
    protected boolean attach(final int windowID) {
        datePicker.attach(windowID);
        return super.attach(windowID);
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.PICKER, datePicker.getID());
        parser.parse(ServerToClientModel.DATE_FORMAT_PATTERN, dateFormat.toPattern());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DATEBOX;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())) {
            final String data = jsonObject.getString(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue());
            Date date = null;
            if (data != null && !data.isEmpty()) {
                try {
                    date = dateFormat.parse(data);
                } catch (final ParseException ex) {
                    if (log.isWarnEnabled()) log.warn("Cannot parse the date #{}", data);
                }
            }
            onValueChange(new PValueChangeEvent<>(this, date));
        } else {
            super.onClientData(jsonObject);
        }
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
        this.date = event.getValue();
        for (final PValueChangeHandler<Date> handler : getValueChangeHandlers()) {
            handler.onValueChange(event);
        }
    }

    public void setDateFormat(final SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.DATE_FORMAT_PATTERN, dateFormat.toPattern());
        });
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    @Override
    public Date getValue() {
        return date;
    }

    public String getDisplayedValue() {
        if (getValue() == null)
            return EMPTY;
        else
            return getDateFormat().format(getValue());
    }

    @Override
    public void setValue(final Date date) {
        this.date = date;
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.VALUE, date != null ? dateFormat.format(date) : EMPTY);
        });
        datePicker.setValue(date);
    }

    public void setDefaultMonth(final Date date) {
        saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.TIME, date.getTime());
        });
    }

    public PDatePicker getDatePicker() {
        return datePicker;
    }

}
