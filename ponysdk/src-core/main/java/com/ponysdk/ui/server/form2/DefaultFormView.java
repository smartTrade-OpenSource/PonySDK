
package com.ponysdk.ui.server.form2;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.validator.ValidationResult;

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

}
