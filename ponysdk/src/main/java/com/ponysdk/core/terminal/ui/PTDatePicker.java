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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DateConverter;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTDatePicker extends PTWidget<DatePicker> {

    private DateTimeFormat format;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        format = DateTimeFormat.getFormat("yyyy-MM-dd");
        super.create(buffer, objectId, uiService);
        addHandlers(uiService);
    }

    @Override
    protected DatePicker createUIObject() {
        return new DatePicker();
    }

    private void addHandlers(final UIBuilder uiService) {
        uiObject.addValueChangeHandler(event -> triggerEvent(uiObject, uiService, event));
        uiObject.addShowRangeHandler(event -> {
            final PTInstruction instruction = new PTInstruction(getObjectID());
            instruction.put(ClientToServerModel.HANDLER_SHOW_RANGE);
            instruction.put(ClientToServerModel.START_DATE, format.format(event.getStart()));
            instruction.put(ClientToServerModel.END_DATE, format.format(event.getEnd()));
            uiService.sendDataToServer(uiObject, instruction);
        });
    }

    protected void triggerEvent(final DatePicker picker, final UIBuilder uiService, final ValueChangeEvent<Date> event) {
        final PTInstruction instruction = new PTInstruction(getObjectID());
        instruction.put(ClientToServerModel.HANDLER_DATE_VALUE_CHANGE,
            event.getValue() != null ? format.format(event.getValue()) : null);
        uiService.sendDataToServer(picker, instruction);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.DATE == model) {
            uiObject.setValue(DateConverter.fromTimestamp(binaryModel.getLongValue()));
            return true;
        } else if (ServerToClientModel.TIME == model) {
            uiObject.setCurrentMonth(DateConverter.fromTimestamp(binaryModel.getLongValue()));
            return true;
        } else if (ServerToClientModel.DATE_ENABLED == model) {
            final JSONArray dates = binaryModel.getArrayValue();
            // ServerToClientModel.ENABLED
            final boolean enabled = buffer.readBinaryModel().getBooleanValue();
            for (int i = 0; i < dates.size(); i++) {
                final JSONValue rawObject = dates.get(i);
                final Date date = DateConverter.decode((long) rawObject.isNumber().doubleValue());
                if (date.after(uiObject.getFirstDate()) && date.before(uiObject.getLastDate())) {
                    uiObject.setTransientEnabledOnDates(enabled, date);
                }
            }
            return true;
        } else if (ServerToClientModel.ADD_DATE_STYLE == model) {
            final JSONArray dates = binaryModel.getArrayValue();
            // ServerToClientModel.STYLE_NAME
            final String style = buffer.readBinaryModel().getStringValue();
            for (int i = 0; i < dates.size(); i++) {
                final JSONValue rawObject = dates.get(i);
                uiObject.addStyleToDates(style, DateConverter.decode((long) rawObject.isNumber().doubleValue()));
            }
            return true;
        } else if (ServerToClientModel.REMOVE_DATE_STYLE == model) {
            final JSONArray dates = binaryModel.getArrayValue();
            // ServerToClientModel.STYLE_NAME
            final String style = buffer.readBinaryModel().getStringValue();
            for (int i = 0; i < dates.size(); i++) {
                final JSONValue rawObject = dates.get(i);
                uiObject.removeStyleFromDates(style, DateConverter.decode((long) rawObject.isNumber().doubleValue()));
            }
            return true;
        } else if (ServerToClientModel.YEAR_ARROWS_VISIBLE == model) {
            uiObject.setYearArrowsVisible(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
