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
package com.ponysdk.ui.server.list.form;

import com.ponysdk.core.export.ExporterTools;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.form.FormActivity;
import com.ponysdk.ui.server.form.FormConfiguration;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.FormView;
import com.ponysdk.ui.server.form.validator.NotEmptyFieldValidator;

public class AddCustomColumnDescriptorForm<T> extends FormActivity {

    private FormField captionFormField;
    private FormField fieldPathFormField;
    private final Class<T> clas;

    public AddCustomColumnDescriptorForm(FormConfiguration formConfiguration, FormView formView, Class<T> instanceClass) {
        super(formConfiguration, formView);
        this.clas = instanceClass;
    }

    @Override
    public void start(PAcceptsOneWidget world) {
        super.start(world);
        captionFormField = new FormField("Caption");
        captionFormField.addValidator(new NotEmptyFieldValidator());
        addFormField(captionFormField);

        fieldPathFormField = new FormField("Field Path");
        fieldPathFormField.addValidator(new NotEmptyFieldValidator());
        addFormField(fieldPathFormField);
    }

    @Override
    public boolean isValid() {
        final String fieldPath = fieldPathFormField.getStringValue();
        final boolean valid = super.isValid() && ExporterTools.hasProperty(clas, fieldPath);
        if (!valid)
            fieldPathFormField.getFormFieldRenderer().addErrorMessage("Error occured when finding property#" + fieldPath);
        return valid;
    }

    public String getCaption() {
        return captionFormField.getStringValue();
    }

    public String getFieldPath() {
        return fieldPathFormField.getStringValue();
    }

}
