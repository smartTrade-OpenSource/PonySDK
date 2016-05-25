
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.form.validator.ValidationResult;

public interface FormFieldListener {

    public void afterReset(FormField<?, ? extends PWidget> formField);

    public void afterValidation(FormField<?, ? extends PWidget> formField, ValidationResult validationResult);
}
