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

import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.form2.DefaultFormView;
import com.ponysdk.ui.server.form2.FormActivity;
import com.ponysdk.ui.server.form2.FormView;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.formfield.StringTextBoxFormField;
import com.ponysdk.ui.server.form2.validator.NotEmptyFieldValidator;

public class Form2PageActivity extends SamplePageActivity {

    public Form2PageActivity() {
        super("Form 2", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();

        final FormView formView = new DefaultFormView();
        final FormActivity formActivity = new FormActivity(formView);

        final FormField<String> field1 = new StringTextBoxFormField();
        field1.setValidator(new NotEmptyFieldValidator());
        final FormField<String> field2 = new StringTextBoxFormField();
        field2.setValidator(new NotEmptyFieldValidator());
        final FormField<String> field3 = new StringTextBoxFormField();
        field3.setValidator(new NotEmptyFieldValidator());
        final FormField<String> field4 = new StringTextBoxFormField();
        field4.setValidator(new NotEmptyFieldValidator());
        final FormField<String> field5 = new StringTextBoxFormField();
        field5.setValidator(new NotEmptyFieldValidator());
        final FormField<String> field6 = new StringTextBoxFormField();
        field6.setValidator(new NotEmptyFieldValidator());

        formActivity.addFormField("Field1", field1);

        final PButton validateButton = new PButton("Validate");
        validateButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final boolean isValid = formActivity.isValid();
                PNotificationManager.showTrayNotification("The form is valid? " + (isValid ? "YES" : "NO"));
            }
        });

        final PSimplePanel formLayout = new PSimplePanel();
        formActivity.start(formLayout);

        panel.add(validateButton);
        panel.add(formLayout);

        examplePanel.setWidget(panel);
    }

}
