package com.ponysdk.core.ui.form2.api;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;

import java.util.Objects;

public abstract class FormField<V> implements IsPWidget {

    private boolean initialized;
    private String caption;
    private String description;
    private V initialValue;

    private final Behavior behavior;
    private final FormFieldPanel panel = new FormFieldPanel();

    private FormFieldValidator validator;

    public FormField(final String caption) {
        this(caption, new Behavior());
        behavior.setEnterKeyUp(PKeyUpHandler.newEnterKeyUp(e -> this.commit()));
    }

    public FormField(final String caption, Behavior behavior) {
        setCaption(caption);
        this.behavior = behavior;
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
        if (!initialized) {
            initialized = true;
            final PWidget innerWidget = createInnerWidget();
            if (behavior.getEnterKeyUp() != null) {
                innerWidget.addDomHandler(behavior.getEnterKeyUp(), PKeyUpEvent.TYPE);
            }
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

    protected abstract PWidget createInnerWidget();

    public abstract void enable();

    public abstract void disable();

    public abstract boolean isEnabled();

    public abstract String getStringToValidate();

    public abstract V getValue();

    public abstract void setValue(V value);

    public static class Behavior {
        private PKeyUpHandler enterKeyUp;

        public PKeyUpHandler getEnterKeyUp() {
            return enterKeyUp;
        }

        public void setEnterKeyUp(PKeyUpHandler enterKeyUp) {
            this.enterKeyUp = enterKeyUp;
        }

    }

}
