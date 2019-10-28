package com.ponysdk.core.ui.form2;

import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;

import java.util.Objects;

public abstract class FormField<V, W extends PWidget> implements IsPWidget {

    protected static String TITLE = "error-msg";
    protected static String HAS_ERROR = "has-error";
    protected static String HAS_DIFF = "has-diff";

    private static ValidationResult OK = ValidationResult.OK();
    private static ValidationResult KO = ValidationResult.KO("Required Field");

    protected PFlowPanel widget;
    protected W innerWidget;

    protected V initialValue;
    protected FieldValidator validator;

    protected String caption;
    private boolean required;

    public FormField(final String caption) {
        this(caption, false);
    }

    public FormField(final String caption, final boolean required) {
        this.caption = caption;
        this.required = required;
    }

    public ValidationResult isValid() {
        if (!isEnabled()) {
            removeErrorMessage();
            return OK;
        }

        ValidationResult result = OK;

        final String stringValue = getStringValue();

        if (required && Objects.requireNonNullElse(stringValue, "").isEmpty()) {
            result = KO;
        } else if (validator != null) {
            result = validator.isValid(stringValue);
        }

        if (result.isValid()) {
            removeErrorMessage();
        } else {
            setErrorMessage(result.getErrorMessage());
        }

        return result;
    }

    public void reset() {
        removeErrorMessage();
        afterReset();
    }

    protected void setErrorMessage(final String message) {
        widget.addStyleName(HAS_ERROR);
        widget.setAttribute(TITLE, message);
    }

    protected void removeErrorMessage() {
        widget.removeStyleName(HAS_ERROR);
        widget.removeAttribute(TITLE);
    }

    protected void afterReset() {
        setValue(initialValue);
        checkInitialValue();
    }

    protected void checkInitialValue() {
        if (initialValue != null) {
            if (!initialValue.equals(getValue())) {
                widget.addStyleName(HAS_DIFF);
            } else {
                widget.removeStyleName("has-diff");
            }
        } else {
            widget.removeStyleName("has-diff");
        }
    }

    public void setValidator(final FieldValidator validator) {
        this.validator = validator;
    }

    public void setInitialValue(final V initialValue) {
        this.initialValue = initialValue;
        checkInitialValue();
    }

    public void setValue(final V value, final boolean init) {
        setValue(value);
        if (init) {
            setInitialValue(value);
        }
    }

    public W getFieldWidget() {
        return innerWidget;
    }

    protected PFlowPanel create() {
        return Element.newPFlowPanel();
    }

    protected void init() {
        widget.addStyleName("form-field");
        if (caption != null) {
            final PElement captionSpan = Element.newSpan();
            captionSpan.setInnerText(caption);
            captionSpan.addStyleName("caption");
            if (required) captionSpan.addStyleName("required");
            widget.add(captionSpan);
        }

        innerWidget = createInnerWidget();

        widget.add(innerWidget);

        initFieldWidget();
    }

    protected void initFieldWidget() {
        innerWidget.addStyleName("field");
        innerWidget.setTabindex(TabindexMode.TABULABLE);
    }

    protected void afterInit() {
    }

    @Override
    public PWidget asWidget() {
        if (widget == null) {
            widget = create();
            init();
            afterInit();
        }
        return widget;
    }

    protected abstract W createInnerWidget();

    public abstract void setEnabled(final boolean enabled);

    public abstract boolean isEnabled();

    public abstract String getStringValue();

    public abstract V getValue();

    public abstract void setValue(V value);
}
