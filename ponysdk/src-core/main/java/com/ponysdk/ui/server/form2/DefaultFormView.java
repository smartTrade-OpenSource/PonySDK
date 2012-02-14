
package com.ponysdk.ui.server.form2;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.validator.ValidationResult;

public class DefaultFormView extends PSimplePanel implements FormView {

    private final PPanel layout;

    private final Map<String, FormFieldComponent> componentByCaption = new HashMap<String, FormFieldComponent>();

    public DefaultFormView() {
        this(new PVerticalPanel());
    }

    public DefaultFormView(final PPanel layout) {
        this.layout = layout;
        setWidget(layout);
    }

    @Override
    public void addFormField(final String caption, final IsPWidget component) {
        final FormFieldComponent formFieldComponent = new FormFieldComponent(caption, component);
        componentByCaption.put(caption, formFieldComponent);
        layout.add(formFieldComponent);
    }

    @Override
    public void removeFormField(final String caption, final IsPWidget component) {
        layout.remove(componentByCaption.remove(caption));
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

    private class FormFieldComponent extends PVerticalPanel {

        private final PHorizontalPanel headerLayout = new PHorizontalPanel();

        private final PLabel requiredLabel = new PLabel("*");

        private final PLabel captionLabel = new PLabel();

        private final PHTML errorLabel = new PHTML();

        public FormFieldComponent(final String caption, final IsPWidget w) {
            headerLayout.setWidth("100%");
            headerLayout.setStyleProperty("border", "1px solid red");
            requiredLabel.setVisible(false);
            captionLabel.setVisible(false);
            errorLabel.setVisible(false);
            headerLayout.add(requiredLabel);
            headerLayout.add(captionLabel);
            headerLayout.add(errorLabel);
            setCaption(caption);
            add(headerLayout);
            add(w);
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

        public void setRequired(final boolean required) {
            requiredLabel.setVisible(required);
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
