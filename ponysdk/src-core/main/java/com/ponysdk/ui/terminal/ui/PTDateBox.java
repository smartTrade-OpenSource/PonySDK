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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.ui.PTDateBox.MyDateBox;

public class PTDateBox extends PTWidget<MyDateBox> {

    private static final DefaultFormat DEFAULT_FORMAT = GWT.create(DefaultFormat.class);

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        // Model.PICKER
        final PTDatePicker datePicker = (PTDatePicker) uiService.getPTObject(buffer.getBinaryModel().getIntValue());
        this.uiObject = new MyDateBox(datePicker.cast(), null, DEFAULT_FORMAT);
        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);

        // Model.DATE_FORMAT_PATTERN
        final MyDateBox dateBox = cast();
        dateBox.setFormat(new DefaultFormat(DateTimeFormat.getFormat(buffer.getBinaryModel().getStringValue())));
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final MyDateBox dateBox = cast();
        if (Model.VALUE.equals(binaryModel.getModel())) {
            dateBox.getTextBox().setText(binaryModel.getStringValue());
            return true;
        }
        if (Model.DATE_FORMAT_PATTERN.equals(binaryModel.getModel())) {
            dateBox.setFormat(new DefaultFormat(DateTimeFormat.getFormat(binaryModel.getStringValue())));
            return true;
        }
        if (Model.ENABLED.equals(binaryModel.getModel())) {
            dateBox.setEnabled(binaryModel.getBooleanValue());
            return true;
        }
        if (Model.TIME.equals(binaryModel.getModel())) {
            dateBox.setDefaultMonth(binaryModel.getLongValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_DATE_VALUE_CHANGE_HANDLER.equals(handlerModel)) {
            final DateBox dateBox = cast();
            final TextBox textBox = dateBox.getTextBox();
            dateBox.addValueChangeHandler(new ValueChangeHandler<Date>() {

                @Override
                public void onValueChange(final ValueChangeEvent<Date> event) {
                    triggerEvent(uiService, dateBox);
                }

            });
            textBox.addValueChangeHandler(new ValueChangeHandler<String>() {

                @Override
                public void onValueChange(final ValueChangeEvent<String> event) {
                    triggerEvent(uiService, dateBox);
                }
            });
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    protected void triggerEvent(final UIService uiService, final DateBox dateBox) {
        final PTInstruction instruction = new PTInstruction();
        instruction.setObjectID(getObjectID());
        instruction.put(HandlerModel.HANDLER_DATE_VALUE_CHANGE_HANDLER);
        instruction.put(Model.VALUE, dateBox.getTextBox().getText());
        uiService.sendDataToServer(dateBox, instruction);
    }

    public static class MyDateBox extends DateBox {

        private Date defaultMonth = null;

        public MyDateBox(final DatePicker picker, final Date date, final Format format) {
            super(picker, date, format);
        }

        public void setDefaultMonth(final long m) {
            defaultMonth = new Date(m);
        }

        @Override
        public void showDatePicker() {
            if (!getTextBox().getText().trim().isEmpty() || defaultMonth == null) {
                super.showDatePicker();
                return;
            }

            super.showDatePicker();
            getDatePicker().setCurrentMonth(defaultMonth);
        }

    }
}
