
package com.ponysdk.ui.server.form.formfield;

import com.ponysdk.ui.server.form.validator.ValidationResult;

public interface FormFieldListener {

    public void afterReset(FormField<?> formField);

    public void afterValidation(FormField<?> formField, ValidationResult validationResult);
}
