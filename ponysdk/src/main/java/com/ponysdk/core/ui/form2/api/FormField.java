package com.ponysdk.core.ui.form2.api;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;

import java.util.Objects;

public abstract class FormField<V> implements IsPWidget {

    private String caption;
    private String description;

    protected FormFieldPanel panel;
    protected FormFieldValidator validator;
    protected V initialValue;

    public FormField(final String caption) {
        this.caption = caption;
        panel = new FormFieldPanel();
        panel.setCaption(caption);
        panel.addInnerWidget(createInnerWidget());
        afterInitialisation();
    }

    private void checkDirty() {
        if (isDirty()) {
            panel.applyDirtyStyle();
        } else {
            panel.removeDirtyStyle();
        }
    }

    public boolean isDirty() {
        return !Objects.equals(getInitialValue(), getValue());
    }

    public ValidationResult validate() {
        if (!isEnabled()) {
            panel.removeErrorStyle();
            panel.removeDirtyStyle();
            return ValidationResult.OK();
        }

        ValidationResult result = ValidationResult.OK();

        if (validator != null) {
            result = validator.isValid(getStringToValidate());
        }

        if (result.isValid()) {
            panel.removeErrorStyle();
        } else {
            panel.applyErrorStyle(result.getErrorMessage());
        }

        checkDirty();

        return result;
    }

    public void setValidator(final FormFieldValidator validator) {
        if (this.validator != null) {
            this.validator.unbindFormField(this);
        }

        this.validator = validator;
        this.validator.bindFormField(this);
    }

    public void commit() {
        initialValue = getValue();
        panel.removeDirtyStyle();
    }

    public void reset() {
        panel.removeErrorStyle();
        setValue(getInitialValue());
        panel.removeDirtyStyle();
    }

    public void setDescription(String description) {
        this.description = description;
        panel.setDescription(description);
    }

    public void setCaption(String caption) {
        this.caption = caption;
        panel.setCaption(caption);
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public PWidget asWidget() {
        return panel;
    }

    private V getInitialValue() {
        return initialValue;
    }

    protected void afterInitialisation() {
    }

    protected abstract PWidget createInnerWidget();

    public abstract void enable();

    public abstract void disable();

    public abstract boolean isEnabled();

    public abstract String getStringToValidate();

    public abstract V getValue();

    public abstract void setValue(V value);

}
