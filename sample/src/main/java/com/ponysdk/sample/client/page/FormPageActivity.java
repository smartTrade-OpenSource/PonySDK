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

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.form.Form;
import com.ponysdk.core.ui.form.FormFieldComponent;
import com.ponysdk.core.ui.form.FormFieldComponent.CaptionOrientation;
import com.ponysdk.core.ui.form.formfield.CheckBoxFormField;
import com.ponysdk.core.ui.form.formfield.DateBoxFormField;
import com.ponysdk.core.ui.form.formfield.StringListBoxFormField;
import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.form.validator.*;
import com.ponysdk.core.ui.rich.PNotificationManager;

public class FormPageActivity extends SamplePageActivity {

    public FormPageActivity() {
        super("Form", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlexTable panel = Element.newPFlexTable();

        final Form form = new Form();

        final StringTextBoxFormField field1 = new StringTextBoxFormField();
        field1.setValidator(new NotEmptyFieldValidator());
        final StringTextBoxFormField field2 = new StringTextBoxFormField();
        field2.setValidator(new DoubleFieldValidator());
        final StringTextBoxFormField field3 = new StringTextBoxFormField();
        field3.setValidator(new CompositeFieldValidator(new NotEmptyFieldValidator(), new DoubleFieldValidator()));
        final StringTextBoxFormField field4 = new StringTextBoxFormField();
        field4.setValidator(new CompositeFieldValidator(new NotEmptyFieldValidator(), new EmailFieldValidator()));
        final CheckBoxFormField field5 = new CheckBoxFormField();
        field5.setValidator(new UncheckedFieldValidator());

        final PListBox listBox = Element.newPListBox(true);
        listBox.addItem("Item 1");
        listBox.addItem("Item 2");
        listBox.addItem("Item 3");
        listBox.addItem("Item 4");
        listBox.addItem("Item 5");
        listBox.addItem("Item 6");

        final StringListBoxFormField field6 = new StringListBoxFormField(listBox);
        field6.setValidator(new NotEmptyFieldValidator());

        final DateBoxFormField field7 = new DateBoxFormField();
        field7.setValidator(new NotEmptyFieldValidator());

        final DateBoxFormField field8 = new DateBoxFormField();
        field7.setValidator(new NotEmptyFieldValidator());

        final StringTextBoxFormField field9 = new StringTextBoxFormField();
        field9.setValidator(new NotEmptyFieldValidator());

        final StringTextBoxFormField field10 = new StringTextBoxFormField();
        field10.setValidator(new TwinFieldValidator("Field doesn't match", field9));

        form.addFormField(field1);
        form.addFormField(field2);
        form.addFormField(field3);
        form.addFormField(field4);
        form.addFormField(field5);
        form.addFormField(field6);
        form.addFormField(field7);
        form.addFormField(field8);
        form.addFormField(field9);
        form.addFormField(field10);

        final FormFieldComponent formFieldComponent1 = new FormFieldComponent("field1", field1);
        final FormFieldComponent formFieldComponent2 = new FormFieldComponent("field2", field2);
        final FormFieldComponent formFieldComponent3 = new FormFieldComponent("field3", field3);
        final FormFieldComponent formFieldComponent4 = new FormFieldComponent("field4", field4);
        final FormFieldComponent formFieldComponent5 = new FormFieldComponent("field5", field5);
        final FormFieldComponent formFieldComponent6 = new FormFieldComponent("field6", field6);
        final FormFieldComponent formFieldComponent7 = new FormFieldComponent("field7", field7);
        final FormFieldComponent formFieldComponent8 = new FormFieldComponent("field8", field8);
        final FormFieldComponent formFieldComponent9 = new FormFieldComponent("field9", field9);
        final FormFieldComponent formFieldComponent10 = new FormFieldComponent("field10", field10);

        final PFlexTable formLayout = Element.newPFlexTable();
        formLayout.addStyleName("cell-top");
        formLayout.setWidget(0, 0, formFieldComponent1);
        formLayout.setWidget(0, 1, formFieldComponent2);
        formLayout.setWidget(1, 0, formFieldComponent3);
        formLayout.setWidget(1, 1, formFieldComponent4);
        formLayout.setWidget(2, 0, formFieldComponent5);
        formLayout.setWidget(2, 1, formFieldComponent6);
        formLayout.setWidget(3, 0, formFieldComponent7);
        formLayout.setWidget(3, 1, formFieldComponent8);
        formLayout.setWidget(4, 0, formFieldComponent9);
        formLayout.setWidget(4, 1, formFieldComponent10);

        final PButton validateButton = Element.newPButton("Validate");
        validateButton.addClickHandler(clickEvent -> {
            final boolean isValid = form.isValid();
            PNotificationManager.showTrayNotification(getView().asWidget().getWindow(),
                "The form is valid? " + (isValid ? "YES" : "NO"));
        });

        final PButton resetButton = Element.newPButton("Reset");
        resetButton.addClickHandler(clickEvent -> {
            form.reset();
            PNotificationManager.showHumanizedNotification(getView().asWidget().getWindow(), "The form has been reseted");
        });

        final PListBox captionOriantationList = Element.newPListBox(true);
        for (final CaptionOrientation captionOriantation : CaptionOrientation.values()) {
            captionOriantationList.addItem(captionOriantation.name(), captionOriantation);
        }
        captionOriantationList.addChangeHandler(event -> {
            final CaptionOrientation captionOriantation = (CaptionOrientation) captionOriantationList.getSelectedValue();

            formFieldComponent1.setCaptionOrientation(captionOriantation);
            formFieldComponent2.setCaptionOrientation(captionOriantation);
            formFieldComponent3.setCaptionOrientation(captionOriantation);
            formFieldComponent4.setCaptionOrientation(captionOriantation);
            formFieldComponent5.setCaptionOrientation(captionOriantation);
            formFieldComponent6.setCaptionOrientation(captionOriantation);
            formFieldComponent7.setCaptionOrientation(captionOriantation);
            formFieldComponent8.setCaptionOrientation(captionOriantation);
        });

        panel.setWidget(0, 0, validateButton);
        panel.getCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
        panel.setWidget(0, 1, resetButton);
        panel.getCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
        panel.setWidget(0, 2, captionOriantationList);
        panel.getCellFormatter().setHorizontalAlignment(0, 2, PHorizontalAlignment.ALIGN_RIGHT);
        panel.setWidget(1, 0, formLayout);
        panel.getCellFormatter().setColSpan(1, 0, 3);

        examplePanel.setWidget(panel);
    }

}
