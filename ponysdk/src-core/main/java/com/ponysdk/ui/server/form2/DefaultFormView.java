
package com.ponysdk.ui.server.form2;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.validator.ValidationResult;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class DefaultFormView extends PVerticalPanel implements FormView {

    private final Map<String, FormFieldComponent> componentByCaption = new HashMap<String, FormFieldComponent>();

    @Override
    public void addFormField(final String caption, final IsPWidget component) {
        final FormFieldComponent formFieldComponent = new FormFieldComponent(caption, component.asWidget());
        componentByCaption.put(caption, formFieldComponent);
        add(formFieldComponent);
    }

    @Override
    public void removeFormField(final String caption, final IsPWidget component) {
        remove(componentByCaption.remove(caption));
    }

    @Override
    public void onValidationResult(final String caption, final ValidationResult result) {
        final FormFieldComponent formFieldComponent = this.componentByCaption.get(caption);
        if (result.isValid()) {
            formFieldComponent.clearErrors();
        } else {
            formFieldComponent.addErrorMessage(result.getErrorMessage());
        }
    }

    @Override
    public void onReset(final String caption, final FormField<?> formField) {
        final FormFieldComponent formFieldComponent = this.componentByCaption.get(caption);
        formFieldComponent.clearErrors();
    }

    private class FormFieldComponent extends PFlexTable {

        private final PLabel captionLabel = new PLabel();

        private final PHTML errorLabel = new PHTML();

        public FormFieldComponent(final String caption, final PWidget w) {
            addStyleName(PonySDKTheme.FORM_FORMFIELD_COMPONENT);

            captionLabel.setVisible(false);
            errorLabel.setVisible(false);

            setCaption(caption);
            setWidget(0, 0, captionLabel);
            setWidget(0, 1, errorLabel);
            setWidget(1, 0, w);
            getCellFormatter().setHorizontalAlignment(0, 0, PHorizontalAlignment.ALIGN_LEFT);
            getCellFormatter().setHorizontalAlignment(0, 1, PHorizontalAlignment.ALIGN_RIGHT);
            getFlexCellFormatter().setColSpan(1, 0, 2);
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

        public void addErrorMessage(final String msg) {
            errorLabel.setHTML("<a class=\"htooltip\">invalid field<span>" + msg + "<img src=\"images/error_16.png\"/></span></a>");
            errorLabel.setVisible(true);
        }

        public void clearErrors() {
            errorLabel.setVisible(false);
        }
    }

}
