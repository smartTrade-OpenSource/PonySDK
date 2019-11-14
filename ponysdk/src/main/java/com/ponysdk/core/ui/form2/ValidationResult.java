package com.ponysdk.core.ui.form2;

public class ValidationResult {

    private static final ValidationResult OK_RESULT = new ValidationResult(true, null, null);

    private boolean valid;
    private String errorMessage;
    private Object data;

    private ValidationResult(boolean valid, String errorMessage, Object data) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public static ValidationResult OK() {
        return OK_RESULT;
    }

    public static ValidationResult OK(final Object data) {
        return newValidationResult(true, null, data);
    }

    public static ValidationResult KO(final String errorMessage) {
        return KO(errorMessage, null);
    }

    public static ValidationResult KO(final String errorMessage, final Object data) {
        return new ValidationResult(false, errorMessage, data);
    }

    public static ValidationResult newValidationResult(final boolean valid, String errorMessage, final Object data) {
        return new ValidationResult(valid, errorMessage, data);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errorMessage='" + errorMessage + '\'' +
                ", data=" + data +
                '}';
    }
}
