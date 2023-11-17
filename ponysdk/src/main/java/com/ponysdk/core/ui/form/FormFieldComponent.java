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

package com.ponysdk.core.ui.form;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.form.formfield.FormField;
import com.ponysdk.core.ui.form.formfield.FormFieldListener;
import com.ponysdk.core.ui.form.validator.ValidationResult;
import com.ponysdk.core.ui.list.Resetable;
import com.ponysdk.core.ui.list.Validable;

/**
 * Rendering of a {@link com.ponysdk.core.ui.form.formfield.FormField}
 */
public class FormFieldComponent extends PFlowPanel implements FormFieldListener, Validable, Resetable {

    protected final FormField formField;

    protected PFlowPanel container = Element.newPFlowPanel();
    protected PLabel captionLabel;
    protected PLabel errorLabel;

    private CaptionOrientation captionOrientation;

    public FormFieldComponent(final FormField formField) {
        this(null, CaptionOrientation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final FormField formField) {
        this(caption, CaptionOrientation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final CaptionOrientation captionOrientation, final FormField formField) {
        this.formField = formField;
        add(container);
        buildUI(caption);
        setCaptionOrientation(captionOrientation);
    }

    protected void buildUI(final String caption) {
        addStyleName("pony-Form-FormField-Component");

        formField.addFormFieldListener(this);
        buildCaption(caption);
        container.add(formField.asWidget());
        buildErrorLabel();
    }

    protected void buildErrorLabel() {
        errorLabel = Element.newPLabel();
        errorLabel.addStyleName("error-label");
        container.add(errorLabel);
    }

    protected void buildCaption(final String caption) {
        captionLabel = Element.newPLabel();
        captionLabel.addStyleName("caption");
        captionLabel.setVisible(false);
        setCaption(caption);
        add(captionLabel);
    }

    public void setCaptionOrientation(final CaptionOrientation captionOriantation) {
        if (this.captionOrientation != null) removeStyleName(this.captionOrientation.name());
        this.captionOrientation = captionOriantation;
        addStyleName(captionOriantation.name());

        if (this.captionOrientation != null) {
            final int captionPosition = getWidgetIndex(captionLabel);
            if (captionOrientation == CaptionOrientation.TOP || captionOrientation == CaptionOrientation.LEFT) {
                if (captionPosition != 0) {
                    remove(captionLabel);
                    insert(captionLabel, 0);
                }
            } else {
                if (captionPosition != 1) {
                    remove(captionLabel);
                    insert(captionLabel, 1);
                }
            }
        }
    }

    public void setCaption(final String caption) {
        if (caption != null) {
            captionLabel.setText(caption);
            captionLabel.setVisible(true);
        } else {
            captionLabel.setText(null);
            captionLabel.setVisible(false);
        }
    }

    @Override
    public void afterReset(final FormField formField) {
        clearError();
    }

    @Override
    public void afterValidation(final FormField formField, final ValidationResult validationResult) {
        if (validationResult.isValid()) {
            clearError();
        } else {
            addStyleName("error");
            errorLabel.setText(validationResult.getErrorMessage());
        }
    }

    public void clearError() {
        removeStyleName("error");
        errorLabel.setText("");
    }

    @Override
    public ValidationResult isValid() {
        return formField.isValid();
    }

    @Override
    public void reset() {
        formField.reset();
    }

    public FormField getFormField() {
        return formField;
    }

    public enum CaptionOrientation {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM
    }

}
