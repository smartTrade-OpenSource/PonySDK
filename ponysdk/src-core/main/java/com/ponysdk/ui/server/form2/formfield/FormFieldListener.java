
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.form2.validator.ValidationResult;

public interface FormFieldListener {

    public void afterReset(FormField<?> formField);

    public void afterValidation(FormField<?> formField, ValidationResult validationResult);
}
