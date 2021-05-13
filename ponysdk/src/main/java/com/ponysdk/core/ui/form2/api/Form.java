package com.ponysdk.core.ui.form2.api;

import java.util.ArrayList;
import java.util.List;

public class Form {

    private final List<Form> subForms = new ArrayList<>();
    private final List<FormField> formfields = new ArrayList<>();

    public void addForm(Form form) {
        subForms.add(form);
    }

    public void removeForm(Form form) {
        subForms.remove(form);
    }

    public void addFormField(int index, FormField<?> formField) {
        formfields.add(index, formField);
    }

    public void addFormField(FormField<?> formField) {
        formfields.add(formField);
    }

    public boolean removeFormField(FormField<?> formField) {
        return formfields.remove(formField);
    }

    public void reset() {
        for (FormField<?> formField : formfields) {
            formField.reset();
        }

        for (Form form : subForms) {
            form.reset();
        }
    }

    public List<FormField<?>> validate() {
        List<FormField<?>> fieldsInError = new ArrayList<>();

        for (FormField<?> formField : formfields) {
            if (!formField.validate().isValid()) {
                fieldsInError.add(formField);
            }
        }

        for (Form form : subForms) {
            fieldsInError.addAll(form.validate());
        }

        return fieldsInError;
    }
}
