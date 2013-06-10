
package com.ponysdk.ui.server.form2;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.formfield.FormFieldListener;
import com.ponysdk.ui.server.form2.validator.ValidationResult;
import com.ponysdk.ui.server.list2.Resetable;
import com.ponysdk.ui.server.list2.Validable;

/**
 * Rendering of a {@link FormField}
 */
public class FormFieldComponent extends PFlowPanel implements FormFieldListener, Validable, Resetable {

    public enum CaptionOrientation {
        LEFT, TOP, RIGHT, BOTTOM
    }

    private CaptionOrientation captionOrientation;

    protected PLabel captionLabel;
    protected PLabel errorLabel;
    protected final FormField<?> formField;

    public FormFieldComponent(final FormField<?> formField) {
        this(null, CaptionOrientation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final FormField<?> formField) {
        this(caption, CaptionOrientation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final CaptionOrientation captionOrientation, final FormField<?> formField) {
        this.formField = formField;
        setCaptionOrientation(captionOrientation);
        buildUI(caption);
    }

    protected void buildUI(final String caption) {
        addStyleName(PonySDKTheme.FORM_FORMFIELD_COMPONENT);

        formField.addFormFieldListener(this);

        buildCaption(caption);
        add(formField.asWidget());
        buildErrorLabel();
    }

    protected void buildErrorLabel() {
        errorLabel = new PLabel();
        errorLabel.addStyleName("error-label");
        add(errorLabel);
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
    public void afterReset(final FormField<?> formField) {
        removeStyleName("error");
        errorLabel.setText("");
    }

    @Override
    public void afterValidation(final FormField<?> formField, final ValidationResult validationResult) {
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