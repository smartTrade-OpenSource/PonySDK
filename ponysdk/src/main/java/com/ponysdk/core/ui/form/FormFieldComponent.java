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

import com.ponysdk.core.ui.basic.IsPWidget;
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

    public enum CaptionOrientation {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM
    }

    private CaptionOrientation captionOrientation;

    protected PFlowPanel container = new PFlowPanel();
    protected PLabel captionLabel;
    protected PLabel errorLabel;
    protected final FormField<?, ? extends IsPWidget> formField;

    public FormFieldComponent(final FormField<?, ? extends IsPWidget> formField) {
        this(null, CaptionOrientation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final FormField<?, ? extends IsPWidget> formField) {
        this(caption, CaptionOrientation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final CaptionOrientation captionOrientation,
            final FormField<?, ? extends IsPWidget> formField) {
        this.formField = formField;
        add(container);
        buildUI(caption);
        setCaptionOrientation(captionOrientation);
    }

    protected void buildUI(final String caption) {
        addStyleName("pform-field-component");

        formField.addFormFieldListener(this);
        buildCaption(caption);
        container.add(formField.asWidget());
        buildErrorLabel();
    }

    protected void buildErrorLabel() {
        errorLabel = new PLabel();
        errorLabel.addStyleName("error-label");
        container.add(errorLabel);
    }

    protected void buildCaption(final String caption) {
        captionLabel = new PLabel();
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
    public void afterReset(final FormField<?, ? extends IsPWidget> formField) {
        removeStyleName("error");
        errorLabel.setText("");
    }

    @Override
    public void afterValidation(final FormField<?, ? extends IsPWidget> formField, final ValidationResult validationResult) {
        if (validationResult.isValid()) {
            removeStyleName("error");
            errorLabel.setText("");
        } else {
            addStyleName("error");
            errorLabel.setText(validationResult.getErrorMessage());
        }
    }

    @Override
    public ValidationResult isValid() {
        return formField.isValid();
    }

    @Override
    public void reset() {
        formField.reset();
    }
}