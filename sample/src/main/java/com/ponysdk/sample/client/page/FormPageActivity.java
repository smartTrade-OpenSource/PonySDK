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

import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.form.DefaultFormView;
import com.ponysdk.ui.server.form.FormActivity;
import com.ponysdk.ui.server.form.FormConfiguration;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.FormView;
import com.ponysdk.ui.server.form.renderer.DateBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.FormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.ListBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.TextAreaFormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.TextBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.TwinListBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.validator.DoubleFieldValidator;
import com.ponysdk.ui.server.form.validator.NotEmptyFieldValidator;

public class FormPageActivity extends SamplePageActivity {

    public FormPageActivity() {
        super("Form", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel layout = new PVerticalPanel();

        final FormView formView = new DefaultFormView();
        final FormConfiguration formConfiguration = new FormConfiguration();
        formConfiguration.setName("Form");
        final FormActivity formActivity = new FormActivity(formConfiguration, formView);

        final FormField field1 = new FormField("field1");
        formActivity.addFormField(field1);
        field1.addValidator(new NotEmptyFieldValidator());
        field1.addValidator(new DoubleFieldValidator());

        final FormFieldRenderer field2Renderer = new TextBoxFormFieldRenderer("field2");
        final FormField field2 = new FormField(field2Renderer);
        formActivity.addFormField(field2);

        final ListBoxFormFieldRenderer field3Renderer = new ListBoxFormFieldRenderer("field3");
        field3Renderer.addItem("Choice 1", 1);
        field3Renderer.addItem("Choice 2", 2);
        field3Renderer.addItem("Choice 3", 3);
        field3Renderer.addItem("Choice 4", 4);
        final FormField field3 = new FormField(field3Renderer);
        formActivity.addFormField(field3);

        final FormFieldRenderer field4Renderer = new DateBoxFormFieldRenderer("field4");
        final FormField field4 = new FormField(field4Renderer);
        formActivity.addFormField(field4);

        final FormFieldRenderer field5Renderer = new TextAreaFormFieldRenderer("field5");
        final FormField field5 = new FormField(field5Renderer);
        formActivity.addFormField(field5);

        final TwinListBoxFormFieldRenderer<String> field6Renderer = new TwinListBoxFormFieldRenderer<String>("field6");
        final FormField field6 = new FormField(field6Renderer);
        formActivity.addFormField(field6);

        field6Renderer.addItem("Choice 1");
        field6Renderer.addItem("Choice 2");
        field6Renderer.addItem("Choice 3");
        field6Renderer.addItem("Choice 4");
        field6Renderer.addItem("Choice 5");
        field6Renderer.addItem("Choice 6");
        field6Renderer.setSelectedItem("Choice 2");
        field6Renderer.setSelectedItem("Choice 3");

        final PSimplePanel formLayout = new PSimplePanel();
        layout.add(formLayout);
        formActivity.start(formLayout);

        final PButton validateButton = new PButton("Validate");
        validateButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final boolean isValid = formActivity.isValid();
                PNotificationManager.showTrayNotification("The form is valid? " + (isValid ? "YES" : "NO"));
            }
        });
        layout.add(validateButton);

        final PButton resetButton = new PButton("Reset");
        resetButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                formActivity.reset();
            }
        });
        layout.add(resetButton);

        examplePanel.setWidget(layout);
    }

}
