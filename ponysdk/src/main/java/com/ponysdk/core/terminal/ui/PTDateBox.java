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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.PTDateBox.MyDateBox;

public class PTDateBox extends PTWidget<MyDateBox> {

    private PTDatePicker datePicker;
    private DefaultFormat defaultFormat;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        datePicker = (PTDatePicker) uiService.getPTObject(buffer.readBinaryModel().getIntValue());
        defaultFormat = new DefaultFormat(DateTimeFormat.getFormat(buffer.readBinaryModel().getStringValue()));
        super.create(buffer, objectId, uiService);
        addValueChangeHandler(uiService);
    }

    @Override
    protected MyDateBox createUIObject() {
        return new MyDateBox(datePicker.uiObject, null, defaultFormat);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.VALUE.ordinal() == modelOrdinal) {
            uiObject.getTextBox().setValue(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.DATE_FORMAT_PATTERN.ordinal() == modelOrdinal) {
            defaultFormat = new DefaultFormat(DateTimeFormat.getFormat(binaryModel.getStringValue()));
            uiObject.setFormat(defaultFormat);
            return true;
        } else if (ServerToClientModel.ENABLED.ordinal() == modelOrdinal) {
            uiObject.setEnabled(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.TIME.ordinal() == modelOrdinal) {
            uiObject.setDefaultMonth(binaryModel.getLongValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    private void addValueChangeHandler(final UIBuilder uiService) {
        final TextBox textBox = uiObject.getTextBox();
        uiObject.addValueChangeHandler(event -> triggerEvent(uiService, uiObject));
        textBox.addValueChangeHandler(event -> triggerEvent(uiService, uiObject));
    }

    private void triggerEvent(final UIBuilder uiService, final DateBox dateBox) {
        final PTInstruction instruction = new PTInstruction(getObjectID());
        instruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, dateBox.getTextBox().getText());
        uiService.sendDataToServer(dateBox, instruction);
    }

    static final class MyDateBox extends DateBox {

        private Date defaultMonth;

        private MyDateBox(final DatePicker picker, final Date date, final Format format) {
            super(picker, date, format);
        }

        private void setDefaultMonth(final long m) {
            defaultMonth = new Date(m);
        }

        @Override
        public void showDatePicker() {
            super.showDatePicker();
            if (defaultMonth != null && getTextBox().getText().trim().isEmpty()) {
                getDatePicker().setCurrentMonth(defaultMonth);
            }
        }

    }
}
