/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
import java.util.Date;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpHandler;
import com.ponysdk.ui.server.form.FormActivity;
import com.ponysdk.ui.server.form.FormConfiguration;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.FormView;
import com.ponysdk.ui.server.form.renderer.ListBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.validator.NotEmptyFieldValidator;
import com.ponysdk.ui.server.list.ComplexListActivity;

public class AddCustomColumnDescriptorForm extends FormActivity {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AddCustomColumnDescriptorForm.class);

    private FormField captionFormField;

    private FormField fieldPathFormField;

    private final Class<?> clas;

    private FormField fieldTypeFormField;

    public static final int KEY_ENTER = 13;

    private final ComplexListActivity<?> complexListActivity;

    public AddCustomColumnDescriptorForm(FormConfiguration formConfiguration, FormView formView, Class<?> instanceClass, ComplexListActivity<?> complexListActivity) {
        super(formConfiguration, formView);
        this.clas = instanceClass;
        this.complexListActivity = complexListActivity;
    }

    @Override
    public void start(PAcceptsOneWidget world) {
        super.start(world);
        captionFormField = new FormField("Caption");
        captionFormField.addValidator(new NotEmptyFieldValidator());
        addFormField(captionFormField);
        addHandlerToFormField(captionFormField);

        fieldPathFormField = new FormField("Field Path");
        fieldPathFormField.addValidator(new NotEmptyFieldValidator());
        addFormField(fieldPathFormField);
        addHandlerToFormField(fieldPathFormField);

        ListBoxFormFieldRenderer typeRenderer = new ListBoxFormFieldRenderer("Field Type");
        typeRenderer.addItem(String.class.getSimpleName(), String.class);
        typeRenderer.addItem(Integer.class.getSimpleName(), Integer.class);
        typeRenderer.addItem(Long.class.getSimpleName(), Long.class);
        typeRenderer.addItem(Double.class.getSimpleName(), Double.class);
        typeRenderer.addItem(Date.class.getSimpleName(), Date.class);
        typeRenderer.addItem(Boolean.class.getSimpleName(), Boolean.class);
        fieldTypeFormField = new FormField(typeRenderer);
        fieldTypeFormField.addValidator(new NotEmptyFieldValidator());
        addFormField(fieldTypeFormField);
        addHandlerToFormField(fieldTypeFormField);

    }

    private void addHandlerToFormField(FormField formField) {
        formField.addDomHandler(new PKeyUpHandler() {

            @Override
            public void onKeyUp(int keyCode) {
                if (keyCode != KEY_ENTER) return;
            }
        }, PKeyUpEvent.TYPE);
    }

    @Override
    public boolean isValid() {
        final String fieldPath = fieldPathFormField.getStringValue();

        boolean valid = super.isValid();
        if (valid) {
            if (complexListActivity.getDescriptorsByCaption().containsKey(captionFormField.getStringValue())) {
                captionFormField.getFormFieldRenderer().addErrorMessage("a column with this caption already exist in the list!");
                valid = false;
            }
            if (!hasProperty(clas, fieldPath)) {
                fieldPathFormField.getFormFieldRenderer().addErrorMessage("Error occured when finding property#" + fieldPath);
                valid = false;
            }
        }
        return valid;
    }

    public Class<?> getFieldType() {
        return ((Class<?>) fieldTypeFormField.getValue());
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
