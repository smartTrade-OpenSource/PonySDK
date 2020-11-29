package com.ponysdk.core.ui.form2.api;

public interface FormFieldValidator {

    ValidationResult isValid(String value);

    default void bindFormField(FormField<?> formField) {
    }

    default void unbindFormField(FormField<?> formField) {
    }

}
