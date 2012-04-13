
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.form2.validator.ValidationResult;

public interface FormFieldListener {

    public void afterReset();

    public void afterValidation(ValidationResult validationResult);
}
