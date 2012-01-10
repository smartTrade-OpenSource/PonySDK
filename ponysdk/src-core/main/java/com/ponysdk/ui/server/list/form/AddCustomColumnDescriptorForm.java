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

import java.beans.PropertyDescriptor;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.form.FormActivity;
import com.ponysdk.ui.server.form.FormConfiguration;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.FormView;
import com.ponysdk.ui.server.form.validator.NotEmptyFieldValidator;

public class AddCustomColumnDescriptorForm<T> extends FormActivity {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AddCustomColumnDescriptorForm.class);

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
        final boolean valid = super.isValid() && hasProperty(clas, fieldPath);
        if (!valid) fieldPathFormField.getFormFieldRenderer().addErrorMessage("Error occured when finding property#" + fieldPath);
        return valid;
    }

    public String getCaption() {
        return captionFormField.getStringValue();
    }

    public String getFieldPath() {
        return fieldPathFormField.getStringValue();
    }

    private static boolean hasProperty(Class<?> clas, String property) {
        final String propertyPath = property;
        boolean isValid = true;
        Class<?> propertyClass = null;
        try {
            if (propertyPath != null) {
                final String[] tokens = propertyPath.split("\\.");
                final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(clas);
                for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (propertyDescriptor.getName().equals(tokens[0])) {
                        propertyClass = propertyDescriptor.getPropertyType();
                        break;
                    }
                }

                if (propertyClass == null) throw new Exception("unknown property#" + tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    final PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(propertyClass);
                    boolean found = false;
                    for (final PropertyDescriptor propertyDescriptor : descriptors) {
                        if (propertyDescriptor.getName().equals(tokens[i])) {
                            propertyClass = propertyDescriptor.getPropertyType();
                            found = true;
                        }
                    }
                    if (!found) throw new Exception("unknown property#" + tokens[i] + " for class#" + propertyClass);
                }
            }
        } catch (final Exception e) {
            final String errorMessage = "Error occured when finding property '" + propertyPath + "'";
            log.error(errorMessage, e);
            isValid = false;
        }
        return isValid;
    }

}
