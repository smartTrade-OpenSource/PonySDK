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

package com.ponysdk.core.terminal.ui;

import java.util.Date;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.PTDateBox.MyDateBox;

public class PTDateBox extends PTWidget<MyDateBox> {

    private PTDatePicker datePicker;
    private DefaultFormat format;
    private Date defaultDate;
    private boolean keepDayTimeNeeded;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        datePicker = (PTDatePicker) uiService.getPTObject(buffer.readBinaryModel().getIntValue());
        final DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(buffer.readBinaryModel().getStringValue());
        format = new DefaultFormat(dateTimeFormat);

        final BinaryModel dateModel = buffer.readBinaryModel();
        if (dateModel.getModel() == ServerToClientModel.VALUE) {
            final String dateText = dateModel.getStringValue();
            defaultDate = dateText != null && !dateText.isEmpty() ? dateTimeFormat.parse(dateText) : null;
        } else {
            buffer.rewind(dateModel);
        }

        final BinaryModel keepTimeModel = buffer.readBinaryModel();
        if (keepTimeModel.getModel() == ServerToClientModel.KEEP_DAY_TIME_NEEDED) this.keepDayTimeNeeded = true;
        else buffer.rewind(keepTimeModel);

        super.create(buffer, objectId, uiService);
        if (PonySDK.get().isTabindexOnlyFormField()) uiObject.setTabIndex(-1);
    }

    @Override
    protected MyDateBox createUIObject() {
        return new MyDateBox(datePicker.uiObject, defaultDate, format);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.VALUE == model) {
            final String dateText = binaryModel.getStringValue();
            uiObject.setValue(format.parse(uiObject, dateText != null ? dateText : "", false));
            return true;
        } else if (ServerToClientModel.DATE_FORMAT_PATTERN == model) {
            format = new DefaultFormat(DateTimeFormat.getFormat(binaryModel.getStringValue()));
            uiObject.setFormat(format);
            return true;
        } else if (ServerToClientModel.ENABLED == model) {
            uiObject.setEnabled(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    class MyDateBox extends DateBox {

        private static final int ONE_MINUTE_IN_MILLIS = 60 * 1000;
        private static final int ONE_DAY_IN_MILLIS = 24 * 60 * ONE_MINUTE_IN_MILLIS;
        private static final String DATE_PICKER_DAY_IS_TODAY_STYLENAME = "datePickerDayIsToday";

        private final DateTimeFormat formatter = DateTimeFormat.getFormat("yyyyMMdd");
        private Date todayDate;
        private String formattedTodayDate;

        private Date lastDate;

        private MyDateBox(final DatePicker picker, final Date date, final Format format) {
            super(picker, date, format);
            getTextBox().addValueChangeHandler(this::onTextBoxChanged);
            getDatePicker().addValueChangeHandler(this::onDatePickerChanged);
        }

        @Override
        public void setValue(final Date date, final boolean fireEvents) {
            super.setValue(date, fireEvents);
            if (keepDayTimeNeeded) lastDate = date;
        }

        @Override
        public void showDatePicker() {
            final Date newTodayDate = new Date();
            if (todayDate != null) {
                if (formattedTodayDate == null) formattedTodayDate = formatter.format(todayDate);
                final String newFormattedTodayDate = formatter.format(newTodayDate);
                if (!formattedTodayDate.equals(newFormattedTodayDate)) {
                    getDatePicker().removeStyleFromDates(DATE_PICKER_DAY_IS_TODAY_STYLENAME, todayDate);
                    getDatePicker().addStyleToDates(DATE_PICKER_DAY_IS_TODAY_STYLENAME, newTodayDate);
                    todayDate = newTodayDate;
                    formattedTodayDate = newFormattedTodayDate;
                }
            } else {
                getDatePicker().addStyleToDates(DATE_PICKER_DAY_IS_TODAY_STYLENAME, newTodayDate);
                todayDate = newTodayDate;
            }
            super.showDatePicker();
        }

        private void onTextBoxChanged(final ValueChangeEvent<String> event) {
            if (keepDayTimeNeeded) lastDate = format.parse(this, event.getValue(), true);
            fireDateChanged();
        }

        private void onDatePickerChanged(final ValueChangeEvent<Date> event) {
            if (keepDayTimeNeeded) {
                Date pickerDate = event.getValue();
                if (lastDate != null) {
                    final int dayTime = (int) (lastDate.getTime() % ONE_DAY_IN_MILLIS
                            - lastDate.getTimezoneOffset() * ONE_MINUTE_IN_MILLIS);
                    final long dateInMillis = pickerDate.getTime() + dayTime;
                    pickerDate = new Date(dateInMillis);
                    getTextBox().setValue(format.format(this, pickerDate), false);
                }
            }
            fireDateChanged();
        }

        private void fireDateChanged() {
            final PTInstruction instruction = new PTInstruction(PTDateBox.this.getObjectID());
            instruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, getTextBox().getValue());
            uiBuilder.sendDataToServer(this, instruction);
        }

    }
}
