package com.ponysdk.core.ui.form2;

@FunctionalInterface
public interface FieldValidator {

    ValidationResult isValid(String value);

}
