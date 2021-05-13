package com.ponysdk.core.ui.form2.api;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class FormField<V> implements IsPWidget {

    private boolean initialized;
    private String caption;
    private String description;
    private V initialValue;
    private V value;

    private Consumer<V> valueChangeHandler;

    private final FormFieldPanel panel = new FormFieldPanel();

    private FormFieldValidator validator;

    public FormField(final String caption) {
        setCaption(caption);
    }

    public boolean isDirty() {
        return !Objects.equals(getInitialValue(), getValue());
    }

    public void setValueChangeHandler(Consumer<V> handler) {
        this.valueChangeHandler = handler;
    }

    public ValidationResult validate() {
        if (!isEnabled()) {
            panel.removeErrorStyle();
            panel.removeDirtyStyle();
            return ValidationResult.OK();
        }

        ValidationResult result = doValidation(validator, value);

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
        value = getInitialValue();
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
        if (!initialized) {
            initialized = true;
            final PWidget innerWidget = createInnerWidget();
            panel.addInnerWidget(innerWidget);
            commit(); //set initial value
        }
        return panel;
    }

    private void checkDirty() {
        if (isDirty()) {
            panel.applyDirtyStyle();
        } else {
            panel.removeDirtyStyle();
        }
    }

    private V getInitialValue() {
        return initialValue;
    }

    public void show() {
        panel.setVisible(true);
    }

    public void hide() {
        panel.setVisible(false);
    }

    public boolean isVisible() {
        return panel.isVisible();
    }

    protected abstract PWidget createInnerWidget();

    public abstract void enable();

    public abstract void disable();

    public abstract boolean isEnabled();

    public V getValue() {
        return value;
    }

    public boolean setValue(V value) {
        return setValue(value, false);
    }

    public boolean setValue(V value, boolean fireEvent) {
        if (!doValidation(validator, value).isValid()) return false;
        doSetValue(value);
        this.value = value;
        if (valueChangeHandler != null && fireEvent) valueChangeHandler.accept(value);
        return false;
    }

    protected abstract void doSetValue(V value);

    public abstract void focus();

    protected abstract ValidationResult doValidation(FormFieldValidator validator, V value);

}
