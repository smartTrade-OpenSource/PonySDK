
package com.ponysdk.ui.server.form2;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.formfield.FormFieldListener;
import com.ponysdk.ui.server.form2.validator.ValidationResult;
import com.ponysdk.ui.server.list2.Resetable;
import com.ponysdk.ui.server.list2.Validable;

public class FormFieldComponent extends PFlowPanel implements FormFieldListener, Validable, Resetable {

    public enum CaptionOrientation {
        LEFT, TOP, RIGHT, BOTTOM
    }

    private CaptionOrientation captionOriantation;

    protected final PLabel captionLabel = new PLabel();
    protected final PHTML errorLabel = new PHTML();
    protected final FormField<?> formField;

    public FormFieldComponent(final String caption, final FormField<?> formField) {
        this(caption, CaptionOrientation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final CaptionOrientation captionOriantation, final FormField<?> formField) {
        addStyleName(PonySDKTheme.FORM_FORMFIELD_COMPONENT);
        errorLabel.addStyleName("error-label");

        this.formField = formField;
        this.captionLabel.setVisible(false);

        formField.addFormFieldListener(this);

        setCaption(caption);

        add(errorLabel);
        add(captionLabel);
        add(formField.asWidget());

        setCaptionOriantation(captionOriantation);
    }

    public void setCaptionOriantation(final CaptionOrientation captionOriantation) {
        if (this.captionOriantation != null) removeStyleName(this.captionOriantation.name());
        this.captionOriantation = captionOriantation;
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
    }

    @Override
    public void afterValidation(final FormField<?> formField, final ValidationResult validationResult) {
        if (validationResult.isValid()) {
            removeStyleName("error");
        } else {
            addStyleName("error");
            errorLabel.setHTML("<a class=\"htooltip\"><span>" + validationResult.getErrorMessage() + "</span></a>");
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