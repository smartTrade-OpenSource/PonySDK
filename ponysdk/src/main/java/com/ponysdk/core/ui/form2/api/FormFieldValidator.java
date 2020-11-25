package com.ponysdk.core.ui.form2.api;

public interface FormFieldValidator {

    ValidationResult isValid(String value);

    void bindFormField(FormField<?> formField);

    void unbindFormField(FormField<?> formField);

}
