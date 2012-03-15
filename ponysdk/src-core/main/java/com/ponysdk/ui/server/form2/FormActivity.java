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

package com.ponysdk.ui.server.form2;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.activity.AbstractActivity;
import com.ponysdk.core.activity.Activity;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PKeyCode;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.form2.event.SubmitFormEvent;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.validator.ValidationResult;

public class FormActivity extends AbstractActivity implements Activity {

    protected final Map<FormField<?>, String> captionByFormField = new HashMap<FormField<?>, String>();
    protected final Map<String, FormField<?>> formFieldByCaption = new HashMap<String, FormField<?>>();

    protected final FormView formView;

    public FormActivity(final FormView formView) {
        this.formView = formView;
        this.formView.asWidget().addDomHandler(new PKeyUpFilterHandler(PKeyCode.ENTER) {

            @Override
            public void onKeyUp(final int keyCode) {
                if (isValid()) {
                    fireEvent(new SubmitFormEvent(FormActivity.this));
                }

            }
        }, PKeyUpEvent.TYPE);
    }

    public void addFormField(final String caption, final FormField<?> formField) {
        formView.addFormField(caption, formField.asWidget());
        captionByFormField.put(formField, caption);
        formFieldByCaption.put(caption, formField);
    }

    public void removeFormField(final FormField<?> formField) {
        final String caption = captionByFormField.remove(formField);
        formFieldByCaption.remove(caption);
        formView.removeFormField(caption, formField.asWidget());
    }

    public FormField<?> getFormField(final String caption) {
        return formFieldByCaption.get(caption);
    }

    public boolean isValid() {
        boolean valid = true;
        for (final FormField<?> formField : captionByFormField.keySet()) {
            final ValidationResult result = formField.isValid();
            if (!result.isValid()) {
                valid = false;
            }
            formView.onValidationResult(captionByFormField.get(formField), result);
        }
        return valid;
    }

    public void reset() {
        for (final FormField<?> formField : captionByFormField.keySet()) {
            formField.reset();
            formView.onReset(captionByFormField.get(formField), formField);
        }
    }

    @Override
    public void start(final PAcceptsOneWidget world) {
        world.setWidget(formView.asWidget());
    }

}
