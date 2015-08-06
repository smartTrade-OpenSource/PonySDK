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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTDatePicker extends PTWidget<DatePicker> {

    private final DateTimeFormat format = DateTimeFormat.getFormat("yyyy-MM-dd");

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new DatePicker());
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        if (HANDLER.KEY_.DATE_VALUE_CHANGE_HANDLER.equals(addHandler.getString(HANDLER.KEY))) {
            final DatePicker picker = cast();
            picker.addValueChangeHandler(new ValueChangeHandler<Date>() {

                @Override
                public void onValueChange(final ValueChangeEvent<Date> event) {
                    triggerEvent(addHandler, picker, uiService, event);
                }
            });
        } else if (HANDLER.KEY_.SHOW_RANGE.equals(addHandler.getString(HANDLER.KEY))) {
            final DatePicker picker = cast();
            picker.addShowRangeHandler(new ShowRangeHandler<Date>() {

                @Override
                public void onShowRange(final ShowRangeEvent<Date> event) {
                    final PTInstruction instruction = new PTInstruction();
                    instruction.setObjectID(addHandler.getObjectID());
                    instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
                    instruction.put(HANDLER.KEY, HANDLER.KEY_.SHOW_RANGE);
                    instruction.put(PROPERTY.START, Long.toString(event.getStart().getTime()));
                    instruction.put(PROPERTY.END, Long.toString(event.getEnd().getTime()));
                    uiService.sendDataToServer(picker, instruction);
                }
            });
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    protected void triggerEvent(final PTInstruction addHandler, final DatePicker picker, final UIService uiService, final ValueChangeEvent<Date> event) {
        String date;
        int year = -1;
        int month = -1;
        int day = -1;

        if (event.getValue() == null) {
            date = "";
        } else {
            date = Long.toString(event.getValue().getTime());
            final String[] values = format.format(event.getValue()).split("-");
            year = Integer.parseInt(values[0]);
            month = Integer.parseInt(values[1]);
            day = Integer.parseInt(values[2]);
        }

        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(addHandler.getObjectID());
        instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
        instruction.put(HANDLER.KEY, HANDLER.KEY_.DATE_VALUE_CHANGE_HANDLER);
        instruction.put(PROPERTY.VALUE, date);
        instruction.put(PROPERTY.YEAR, year);
        instruction.put(PROPERTY.MONTH, month);
        instruction.put(PROPERTY.DAY, day);
        uiService.sendDataToServer(picker, instruction);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final DatePicker picker = cast();
        if (update.containsKey(PROPERTY.VALUE)) {
            picker.setValue(asDate(update.getString(PROPERTY.VALUE)));
        } else if (update.containsKey(PROPERTY.MONTH)) {
            picker.setCurrentMonth(asDate(update.getString(PROPERTY.MONTH)));
        } else if (update.containsKey(PROPERTY.DATE_ENABLED)) {
            final Boolean enabled = update.getBoolean(PROPERTY.ENABLED);
            final JSONArray jsonArray = update.get(PROPERTY.DATE_ENABLED).isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                picker.setTransientEnabledOnDates(enabled, asDate(jsonArray.get(i).isString().stringValue()));
            }
        } else if (update.containsKey(PROPERTY.ADD_DATE_STYLE)) {
            final String style = update.getString(PROPERTY.STYLE_NAME);
            final JSONArray jsonArray = update.get(PROPERTY.ADD_DATE_STYLE).isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                picker.addStyleToDates(style, asDate(jsonArray.get(i).isString().stringValue()));
            }
        } else if (update.containsKey(PROPERTY.REMOVE_DATE_STYLE)) {
            final String style = update.getString(PROPERTY.STYLE_NAME);
            final JSONArray jsonArray = update.get(PROPERTY.REMOVE_DATE_STYLE).isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                picker.removeStyleFromDates(style, asDate(jsonArray.get(i).isString().stringValue()));
            }
        } else {
            super.update(update, uiService);
        }
    }

    private Date asDate(final String s) {
        return new Date(Long.parseLong(s));
    }

}
