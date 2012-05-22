
package com.ponysdk.ui.server.form2;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.formfield.FormFieldListener;
import com.ponysdk.ui.server.form2.validator.ValidationResult;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class FormFieldComponent extends PFlexTable implements FormFieldListener {

    public enum CaptionOriantation {
        LEFT, TOP, RIGHT, BOTTOM
    }

    private final PLabel captionLabel = new PLabel();

    private final PHTML errorLabel = new PHTML();

    private final FormField<?> formField;

    public FormFieldComponent(final String caption, final FormField<?> formField) {
        this(caption, CaptionOriantation.TOP, formField);
    }

    public FormFieldComponent(final String caption, final CaptionOriantation captionOriantation, final FormField<?> formField) {
        addStyleName(PonySDKTheme.FORM_FORMFIELD_COMPONENT);

        this.formField = formField;
        this.captionLabel.setVisible(false);
        this.errorLabel.setVisible(false);

        formField.addFormFieldListener(this);

        setCaption(caption);

        setCaptionOriantation(captionOriantation);
    }

    public void setCaptionOriantation(final CaptionOriantation captionOriantation) {
        for (int i = getRowCount(); i > 0; i--) {
            removeRow(i - 1);
        }

        switch (captionOriantation) {
            case LEFT:
                setWidget(0, 0, errorLabel);
                setWidget(1, 0, captionLabel);
                getCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
                getCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
                setWidget(1, 1, formField.asWidget());
                getFlexCellFormatter().setColSpan(0, 0, 2);
                break;
            case TOP:
                setWidget(0, 0, captionLabel);
                setWidget(0, 1, errorLabel);
                setWidget(1, 0, formField.asWidget());
                getCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
                getCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
                getFlexCellFormatter().setColSpan(1, 0, 2);
                break;
            case RIGHT:
                setWidget(0, 0, errorLabel);
                setWidget(1, 0, formField.asWidget());
                setWidget(1, 1, captionLabel);
                getCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
                getCellFormatter().setHorizontalAlignment(1, 1, PHorizontalAlignment.ALIGN_RIGHT);
                getFlexCellFormatter().setColSpan(0, 0, 2);
                break;
            case BOTTOM:
                setWidget(0, 0, formField.asWidget());
                setWidget(1, 0, captionLabel);
                setWidget(1, 1, errorLabel);
                getCellFormatter().setHorizontalAlignment(1, 0, PHorizontalAlignment.ALIGN_LEFT);
                getCellFormatter().setHorizontalAlignment(1, 1, PHorizontalAlignment.ALIGN_RIGHT);
                getFlexCellFormatter().setColSpan(0, 0, 2);
                break;

            default:
                break;
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
    public void afterReset(final FormField<?> formField) {
        errorLabel.setVisible(false);
    }

    @Override
    public void afterValidation(final FormField<?> formField, final ValidationResult validationResult) {
        if (validationResult.isValid()) {
            errorLabel.setVisible(false);
        } else {
            errorLabel.setHTML("<a class=\"htooltip\">invalid field<span>" + validationResult.getErrorMessage() + "<img src=\"images/error_16.png\"/></span></a>");
            errorLabel.setVisible(true);
        }
    }
}