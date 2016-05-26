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

package com.ponysdk.ui.terminal.ui;

import java.util.Date;

import com.google.gwt.event.logical.shared.ShowRangeEvent;
import com.google.gwt.event.logical.shared.ShowRangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PTDatePicker extends PTWidget<DatePicker> {

    public static final String DATE_SEPARATOR = ",";

    private final DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd");

    @Override
    protected DatePicker createUIObject() {
        return new DatePicker();
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIBuilder uiService) {
        if (HandlerModel.HANDLER_DATE_VALUE_CHANGE_HANDLER.equals(handlerModel)) {
            final DatePicker picker = cast();
            picker.addValueChangeHandler(new ValueChangeHandler<Date>() {

                @Override
                public void onValueChange(final ValueChangeEvent<Date> event) {
                    triggerEvent(picker, uiService, event);
                }
            });
        } else if (HandlerModel.HANDLER_SHOW_RANGE.equals(handlerModel)) {
            final DatePicker picker = cast();
            picker.addShowRangeHandler(new ShowRangeHandler<Date>() {

                @Override
                public void onShowRange(final ShowRangeEvent<Date> event) {
                    final PTInstruction instruction = new PTInstruction(getObjectID());
                    // FIXME
                    instruction.put(ClientToServerModel.HANDLER_SHOW_RANGE);
                    instruction.put(ClientToServerModel.START_DATE, event.getStart().getTime());
                    instruction.put(ClientToServerModel.END_DATE, event.getEnd().getTime());
                    uiService.sendDataToServer(picker, instruction);
                }
            });
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    protected void triggerEvent(final DatePicker picker, final UIBuilder uiService,
            final ValueChangeEvent<Date> event) {
        long date = -1;
        int year = -1;
        int month = -1;
        int day = -1;

        if (event.getValue() != null) {
            date = event.getValue().getTime();
            final String[] values = format.format(event.getValue()).split("-");
            year = Integer.parseInt(values[0]);
            month = Integer.parseInt(values[1]);
            day = Integer.parseInt(values[2]);
        }

        final PTInstruction instruction = new PTInstruction(getObjectID());
        instruction.put(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE_HANDLER);
        instruction.put(ClientToServerModel.DATE, date);
        instruction.put(ClientToServerModel.YEAR, year);
        instruction.put(ClientToServerModel.MONTH, month);
        instruction.put(ClientToServerModel.DAY, day);
        uiService.sendDataToServer(picker, instruction);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final DatePicker picker = cast();
        if (ServerToClientModel.DATE.equals(binaryModel.getModel())) {
            picker.setValue(asDate(binaryModel.getLongValue()));
            return true;
        }
        if (ServerToClientModel.TIME.equals(binaryModel.getModel())) {
            picker.setCurrentMonth(asDate(binaryModel.getLongValue()));
            return true;
        }
        if (ServerToClientModel.DATE_ENABLED.equals(binaryModel.getModel())) {
            final String[] dates = binaryModel.getStringValue().split(DATE_SEPARATOR);
            // ServerToClientModel.ENABLED
            final boolean enabled = buffer.getBinaryModel().getBooleanValue();
            for (final String date : dates) {
                picker.setTransientEnabledOnDates(enabled, asDate(date));
            }
            return true;
        }
        if (ServerToClientModel.ADD_DATE_STYLE.equals(binaryModel.getModel())) {
            final String[] dates = binaryModel.getStringValue().split(DATE_SEPARATOR);
            // ServerToClientModel.STYLE_NAME
            final String style = buffer.getBinaryModel().getStringValue();
            for (final String date : dates) {
                picker.addStyleToDates(style, asDate(date));
            }
            return true;
        }
        if (ServerToClientModel.REMOVE_DATE_STYLE.equals(binaryModel.getModel())) {
            final String[] dates = binaryModel.getStringValue().split(DATE_SEPARATOR);
            // ServerToClientModel.STYLE_NAME
            final String style = buffer.getBinaryModel().getStringValue();
            for (final String date : dates) {
                picker.removeStyleFromDates(style, asDate(date));
            }
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    private static final Date asDate(final String timestamp) {
        return new Date(Long.parseLong(timestamp));
    }

    private static final Date asDate(final long timestamp) {
        return timestamp != -1 ? new Date(timestamp) : null;
    }

}
