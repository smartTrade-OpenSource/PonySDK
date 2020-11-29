package com.ponysdk.core.ui.form2.impl.validator;

import com.ponysdk.core.ui.form2.api.FormFieldValidator;
import com.ponysdk.core.ui.form2.api.ValidationResult;

import java.util.regex.Pattern;

public class EmailValidator implements FormFieldValidator {

    private static final String EMAIL_REGEX = "^[\\w-+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    private static final Pattern pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);


    @Override
    public ValidationResult isValid(String value) {
        if (pattern.matcher(value).matches())
            return ValidationResult.OK();
        else
            return ValidationResult.KO("Invalid email");
    }

}
