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

package com.ponysdk.sample.client.page;

import java.util.Date;

import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.form2.Form;
import com.ponysdk.ui.server.form2.FormFieldComponent;
import com.ponysdk.ui.server.form2.FormFieldComponent.CaptionOrientation;
import com.ponysdk.ui.server.form2.formfield.CheckBoxFormField;
import com.ponysdk.ui.server.form2.formfield.DateBoxFormField;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.formfield.StringListBoxFormField;
import com.ponysdk.ui.server.form2.formfield.StringTextBoxFormField;
import com.ponysdk.ui.server.form2.validator.CompositeFieldValidator;
import com.ponysdk.ui.server.form2.validator.DoubleFieldValidator;
import com.ponysdk.ui.server.form2.validator.EmailFieldValidator;
import com.ponysdk.ui.server.form2.validator.NotEmptyFieldValidator;
import com.ponysdk.ui.server.form2.validator.UncheckedFieldValidator;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class Form2PageActivity extends SamplePageActivity {

    public Form2PageActivity() {
        super("Form 2", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlexTable panel = new PFlexTable();

        final Form form = new Form();

        final FormField<String> field1 = new StringTextBoxFormField();
        field1.setValidator(new NotEmptyFieldValidator());
        final FormField<String> field2 = new StringTextBoxFormField();
        field2.setValidator(new DoubleFieldValidator());
        final FormField<String> field3 = new StringTextBoxFormField();
        field3.setValidator(new CompositeFieldValidator(new NotEmptyFieldValidator(), new DoubleFieldValidator()));
        final FormField<String> field4 = new StringTextBoxFormField();
        field4.setValidator(new CompositeFieldValidator(new NotEmptyFieldValidator(), new EmailFieldValidator()));
        final FormField<Boolean> field5 = new CheckBoxFormField();
        field5.setValidator(new UncheckedFieldValidator());

        final PListBox listBox = new PListBox(true);
        listBox.addItem("Item 1");
        listBox.addItem("Item 2");
        listBox.addItem("Item 3");
        listBox.addItem("Item 4");
        listBox.addItem("Item 5");
        listBox.addItem("Item 6");

        final FormField<String> field6 = new StringListBoxFormField(listBox);
        field6.setValidator(new NotEmptyFieldValidator());

        final FormField<Date> field7 = new DateBoxFormField();
        field7.setValidator(new NotEmptyFieldValidator());

        final FormField<Date> field8 = new DateBoxFormField();
        field7.setValidator(new NotEmptyFieldValidator());

        form.addFormField(field1);
        form.addFormField(field2);
        form.addFormField(field3);
        form.addFormField(field4);
        form.addFormField(field5);
        form.addFormField(field6);
        form.addFormField(field7);
        form.addFormField(field8);

        final FormFieldComponent formFieldComponent1 = new FormFieldComponent("field1", field1);
        final FormFieldComponent formFieldComponent2 = new FormFieldComponent("field2", field2);
        final FormFieldComponent formFieldComponent3 = new FormFieldComponent("field3", field3);
        final FormFieldComponent formFieldComponent4 = new FormFieldComponent("field4", field4);
        final FormFieldComponent formFieldComponent5 = new FormFieldComponent("field5", field5);
        final FormFieldComponent formFieldComponent6 = new FormFieldComponent("field6", field6);
        final FormFieldComponent formFieldComponent7 = new FormFieldComponent("field7", field7);
        final FormFieldComponent formFieldComponent8 = new FormFieldComponent("field8", field8);

        final PFlexTable formLayout = new PFlexTable();
        formLayout.setWidget(0, 0, formFieldComponent1);
        formLayout.setWidget(0, 1, formFieldComponent2);
        formLayout.setWidget(1, 0, formFieldComponent3);
        formLayout.setWidget(1, 1, formFieldComponent4);
        formLayout.setWidget(2, 0, formFieldComponent5);
        formLayout.setWidget(2, 1, formFieldComponent6);
        formLayout.setWidget(3, 0, formFieldComponent7);
        formLayout.setWidget(3, 1, formFieldComponent8);

        final PButton validateButton = new PButton("Validate");
        validateButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final boolean isValid = form.isValid();
                PNotificationManager.showTrayNotification("The form is valid? " + (isValid ? "YES" : "NO"));
            }
        });

        final PButton resetButton = new PButton("Reset");
        resetButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                form.reset();
                PNotificationManager.showHumanizedNotification("The form has been reseted");
            }
        });

        final PListBox captionOriantationList = new PListBox(true);
        for (final CaptionOrientation captionOriantation : CaptionOrientation.values()) {
            captionOriantationList.addItem(captionOriantation.name(), captionOriantation);
        }
        captionOriantationList.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final PChangeEvent event) {
                final CaptionOrientation captionOriantation = (CaptionOrientation) captionOriantationList.getSelectedValue();

                formFieldComponent1.setCaptionOriantation(captionOriantation);
                formFieldComponent2.setCaptionOriantation(captionOriantation);
                formFieldComponent3.setCaptionOriantation(captionOriantation);
                formFieldComponent4.setCaptionOriantation(captionOriantation);
                formFieldComponent5.setCaptionOriantation(captionOriantation);
                formFieldComponent6.setCaptionOriantation(captionOriantation);
                formFieldComponent7.setCaptionOriantation(captionOriantation);
                formFieldComponent8.setCaptionOriantation(captionOriantation);
            }
        });

        panel.setWidget(0, 0, validateButton);
        panel.getFlexCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
        panel.setWidget(0, 1, resetButton);
        panel.getFlexCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
        panel.setWidget(0, 2, captionOriantationList);
        panel.getFlexCellFormatter().setHorizontalAlignment(0, 2, PHorizontalAlignment.ALIGN_RIGHT);
        panel.setWidget(1, 0, formLayout);
        panel.getFlexCellFormatter().setColSpan(1, 0, 3);

        examplePanel.setWidget(panel);
    }
}
