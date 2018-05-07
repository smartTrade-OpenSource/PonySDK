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
        private DefaultFormat format;

        @Override
        public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
            datePicker = (PTDatePicker) uiService.getPTObject(buffer.readBinaryModel().getIntValue());
            format = new DefaultFormat(DateTimeFormat.getFormat(buffer.readBinaryModel().getStringValue()));
            super.create(buffer, objectId, uiService);
        }

        @Override
        protected MyDateBox createUIObject() {
            return new MyDateBox(datePicker.uiObject, null, format);
        }

        @Override
        public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
            final ServerToClientModel model = binaryModel.getModel();
            if (ServerToClientModel.VALUE == model) {
                uiObject.getTextBox().setValue(binaryModel.getStringValue());
                return true;
            } else if (ServerToClientModel.DATE_FORMAT_PATTERN == model) {
                format = new DefaultFormat(DateTimeFormat.getFormat(binaryModel.getStringValue()));
                uiObject.setFormat(format);
                return true;
            } else if (ServerToClientModel.ENABLED == model) {
                uiObject.setEnabled(binaryModel.getBooleanValue());
                return true;
            } else if (ServerToClientModel.TIME == model) {
                uiObject.setDefaultMonth(binaryModel.getLongValue());
                return true;
            } else {
                return super.update(buffer, binaryModel);
            }
        }

        final class MyDateBox extends DateBox {

            private Date defaultMonth;
            private Date date;

            private MyDateBox(final DatePicker picker, final Date date, final Format format) {
                super(picker, date, format);
                addValueChangeHandler(event -> onDateSelected());
                getTextBox().addValueChangeHandler(event -> onTextBoxChanged());
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

            private void onTextBoxChanged() {
                Date date = format.parse(this, getTextBox().getText(), true);
                if (date != null) {
                    this.date = date;
                    fireDateChanged();
                }
            }

            @SuppressWarnings("deprecation")
            private void onDateSelected() {
                int hours = date.getHours();
                int minutes = date.getMinutes();
                int seconds = date.getSeconds();
                int millis = (int) date.getTime() % 1000;

                Date datePickerDate = getDatePicker().getValue();
                Date newDate = new Date(datePickerDate.getTime() + hours * 3600000 + minutes * 60000 + seconds * 1000 + millis);
                getTextBox().setText(format.format(this, newDate));

                fireDateChanged();
            }

            private void fireDateChanged() {
                final PTInstruction instruction = new PTInstruction(PTDateBox.this.getObjectID());
                instruction.put(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE, getTextBox().getText());
                uiBuilder.sendDataToServer(this, instruction);
            }

        }
    }
