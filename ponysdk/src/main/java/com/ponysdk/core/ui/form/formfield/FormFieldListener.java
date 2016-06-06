
package com.ponysdk.core.ui.form.formfield;

import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.form.validator.ValidationResult;

public interface FormFieldListener {

    void afterReset(FormField<?, ? extends PWidget> formField);

    void afterValidation(FormField<?, ? extends PWidget> formField, ValidationResult validationResult);
}
