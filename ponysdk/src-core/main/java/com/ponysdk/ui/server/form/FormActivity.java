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

package com.ponysdk.ui.server.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.deprecated.AbstractActivity;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.form.event.SubmitFormEvent;

public class FormActivity extends AbstractActivity {

    private final List<FormField> formFields = new ArrayList<FormField>();

    private final Map<FormField, IsPWidget> widgetByFormField = new HashMap<FormField, IsPWidget>();

    protected final FormView formView;

    public FormActivity(final FormView formView) {
        this(null, formView);
    }

    public FormActivity(final FormConfiguration formConfiguration, final FormView formView) {
        this.formView = formView;
        this.formView.asWidget().addDomHandler(new PKeyUpFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                if (isValid()) {
                    fireEvent(new SubmitFormEvent(FormActivity.this));
                }

            }
        }, PKeyUpEvent.TYPE);
    }

    public void addFormField(final FormField formField) {
        formFields.add(formField);
        final IsPWidget renderer = formField.render();
        formView.addFormField(renderer);
        widgetByFormField.put(formField, renderer);
    }

    public void removeFormField(final FormField formField) {
        formFields.remove(formField);
        formView.removeFormField(widgetByFormField.get(formField));
    }

    public boolean isValid() {
        boolean valid = true;
        for (final FormField formField : formFields) {
            if (!formField.isValid()) {
                valid = false;
            }
        }
        return valid;
    }

    public void reset() {
        for (final FormField formField : formFields) {
            formField.reset();
        }
    }

    @Override
    public void start(final PAcceptsOneWidget world) {
        world.setWidget(formView.asWidget());
    }

    // @Override
    // public void goTo(final Place newPlace, final PAcceptsOneWidget world) {
    // super.goTo(newPlace, world);
    // world.setWidget(formView.asWidget());
    // }

}
