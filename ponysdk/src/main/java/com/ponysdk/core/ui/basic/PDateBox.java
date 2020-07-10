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
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickHandler;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PFocusHandler;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.writer.ModelWriter;

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
public class PDateBox extends PWidget implements Focusable, HasPValue<Date>, PValueChangeHandler<Date> {

    private static final Logger log = LoggerFactory.getLogger(PDateBox.class);

    private static final String EMPTY = "";

    private final PDatePicker datePicker;

    private SimpleDateFormat dateFormat;
    private List<PValueChangeHandler<Date>> handlers;
    private final boolean keepDayTimeNeeded;

    private String rawValue;
    private Date date;
    private boolean enabled = true;

    protected PDateBox() {
        this(new SimpleDateFormat("MM/dd/yyyy"));
    }

    protected PDateBox(final SimpleDateFormat dateFormat) {
        this(dateFormat, false);
    }

    protected PDateBox(final SimpleDateFormat dateFormat, final boolean keepDayTimeNeeded) {
        this(new PDatePicker(), dateFormat, keepDayTimeNeeded);
    }

    protected PDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat) {
        this(picker, dateFormat, false);

    }

    protected PDateBox(final PDatePicker picker, final SimpleDateFormat dateFormat, final boolean keepDayTimeNeeded) {
        if (UIContext.get().getConfiguration().isTabindexOnlyFormField()) tabindex = TabindexMode.FOCUSABLE.getTabIndex();
        this.datePicker = picker;
        this.dateFormat = dateFormat;
        this.keepDayTimeNeeded = keepDayTimeNeeded;
        saveAdd(datePicker.getID(), ID);
    }

    @Override
    protected boolean attach(final PWindow window, final PFrame frame) {
        datePicker.attach(window, frame);
        return super.attach(window, frame);
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.PICKER, datePicker.getID());
        writer.write(ServerToClientModel.DATE_FORMAT_PATTERN, dateFormat.toPattern());
        if (keepDayTimeNeeded) writer.write(ServerToClientModel.KEEP_DAY_TIME_NEEDED);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DATEBOX;
    }

    @Override
    public void onClientData(final JsonObject jsonObject) {
        if (!isVisible()) return;
        if (jsonObject.containsKey(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue())) {
            rawValue = jsonObject.getString(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue());
            Date date = null;
            if (rawValue != null && !rawValue.isEmpty()) {
                try {
                    date = dateFormat.parse(rawValue);
                } catch (final ParseException ex) {
                    if (log.isWarnEnabled()) log.warn("Cannot parse the date #{}", rawValue);
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
        this.date = event.getData();
        getValueChangeHandlers().forEach(handler -> handler.onValueChange(event));
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(final SimpleDateFormat dateFormat) {
        if (Objects.equals(this.dateFormat, dateFormat)) return;
        this.dateFormat = dateFormat;
        if (initialized) saveUpdate(ServerToClientModel.DATE_FORMAT_PATTERN, dateFormat.toPattern());
    }

    @Override
    public Date getValue() {
        return date;
    }

    @Override
    public void setValue(final Date date) {
        this.date = date;
        this.rawValue = date != null ? dateFormat.format(date) : EMPTY;
        saveUpdate(ServerToClientModel.VALUE, rawValue);
        datePicker.setValue(date);
    }

    public String getDisplayedValue() {
        if (getValue() == null) return EMPTY;
        else return getDateFormat().format(getValue());
    }

    public PDatePicker getDatePicker() {
        return datePicker;
    }

    public String getRawValue() {
        return rawValue;
    }

    /**
     * @since v2.7.16
     * @deprecated Use {@link #focus()} or {@link #blur()}
     */
    @Deprecated
    @Override
    public void setFocus(final boolean focused) {
        if (focused) focus();
        else blur();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        if (Objects.equals(this.enabled, enabled)) return;
        this.enabled = enabled;
        saveUpdate(ServerToClientModel.ENABLED, enabled);
    }

    public HandlerRegistration addMouseOverHandler(final PMouseOverEvent.Handler handler) {
        return addDomHandler(handler, PMouseOverEvent.TYPE);
    }

    public HandlerRegistration addFocusHandler(final PFocusHandler handler) {
        return addDomHandler(handler, PFocusEvent.TYPE);
    }

    public HandlerRegistration addBlurHandler(final PBlurHandler handler) {
        return addDomHandler(handler, PBlurEvent.TYPE);
    }

    public HandlerRegistration addClickHandler(final PClickHandler handler) {
        return addDomHandler(handler, PClickEvent.TYPE);
    }

    public HandlerRegistration addDoubleClickHandler(final PDoubleClickHandler handler) {
        return addDomHandler(handler, PDoubleClickEvent.TYPE);
    }

    @Override
    protected String dumpDOM() {
        return "<input format=\"" + dateFormat.toPattern() + "\">" + date + "</input>";
    }
}
