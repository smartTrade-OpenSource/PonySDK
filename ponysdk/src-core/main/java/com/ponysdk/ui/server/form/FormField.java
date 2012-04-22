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
import java.util.List;

import com.ponysdk.core.event.PEventHandler;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.event.PDomEvent;
import com.ponysdk.ui.server.form.renderer.FormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.TextBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.validator.FieldValidator;
import com.ponysdk.ui.server.form.validator.ValidationResult;

public class FormField {

    private final List<FieldValidator> validators = new ArrayList<FieldValidator>();

    private final List<ResetHandler> resetHandlers = new ArrayList<ResetHandler>();

    private final FormFieldRenderer formFieldRenderer;

    public interface ResetHandler {

        void onReset();
    }

    public FormField() {
        this.formFieldRenderer = new TextBoxFormFieldRenderer();
    }

    public FormField(String caption) {
        this.formFieldRenderer = new TextBoxFormFieldRenderer(caption);
    }

    public FormField(FormFieldRenderer formFieldRenderer) {
        this.formFieldRenderer = formFieldRenderer;
    }

    public void ensureDebugID(String id) {
        this.formFieldRenderer.ensureDebugID(id);
    }

    public boolean isValid() {
        clearErrorMessage();

        if (validators.isEmpty()) return true;

        boolean valid = true;

        for (final FieldValidator fieldValidator : validators) {
            final ValidationResult validationResult = fieldValidator.isValid(this);
            if (!validationResult.isValid()) {
                valid = false;
                addErrorMessage(validationResult.getErrorMessage());
            }
        }

        return valid;
    }

    public void reset() {
        formFieldRenderer.reset();
        for (ResetHandler resetHandler : resetHandlers) {
            resetHandler.onReset();
        }
    }

    public IsPWidget render() {
        return formFieldRenderer.render(this);
    }

    public void setValue(Object value) {
        formFieldRenderer.setValue(value);
    }

    public Object getValue() {
        return formFieldRenderer.getValue();
    }

    public String getStringValue() {
        return formFieldRenderer.getValue() == null ? null : formFieldRenderer.getValue().toString();
    }

    public Integer getIntegerValue() {
        return formFieldRenderer.getValue() == null ? null : Integer.parseInt(formFieldRenderer.getValue().toString());
    }

    public Long getLongValue() {
        return formFieldRenderer.getValue() == null ? null : Long.parseLong(formFieldRenderer.getValue().toString());
    }

    public Float getFloatValue() {
        return formFieldRenderer.getValue() == null ? null : Float.parseFloat(formFieldRenderer.getValue().toString());
    }

    public Double getDoubleValue() {
        return formFieldRenderer.getValue() == null ? null : Double.parseDouble(formFieldRenderer.getValue().toString());
    }

    public Boolean getBooleanValue() {
        return formFieldRenderer.getValue() == null ? null : Boolean.parseBoolean(formFieldRenderer.getValue().toString());
    }

    public void addValidator(FieldValidator validator) {
        this.validators.add(validator);
    }

    public void removeValidator(FieldValidator validator) {
        this.validators.remove(validator);
    }

    private void addErrorMessage(String fieldMsgNotABoolean) {
        formFieldRenderer.addErrorMessage(fieldMsgNotABoolean);
    }

    private void clearErrorMessage() {
        formFieldRenderer.clearErrorMessage();
    }

    public void setEnabled(boolean enable) {
        formFieldRenderer.setEnabled(enable);
    }

    public FormFieldRenderer getFormFieldRenderer() {
        return formFieldRenderer;
    }

    public <H extends PEventHandler> void addDomHandler(final H handler, final PDomEvent.Type<H> type) {
        formFieldRenderer.addDomHandler(handler, type);
    }

    public void addResetHandler(ResetHandler handler) {
        resetHandlers.add(handler);
    }
}
