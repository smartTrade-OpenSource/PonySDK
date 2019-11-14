package com.ponysdk.core.ui.form2;

import com.ponysdk.core.ui.basic.*;

import java.util.Objects;

public abstract class FormField<V, W extends PWidget> implements IsPWidget {

    private static String STYLE_FORM_FIELD = "form-field";
    private static String STYLE_CAPTION = "caption";
    private static String STYLE_INNER_WIDGET = "inner-widget";

    private static String ATTR_REQUIRED = "required";
    private static String ATTR_ERROR = "error";
    private static String ATTR_DIFF = "diff";

    private static ValidationResult OK_RESULT = ValidationResult.OK();
    public static ValidationResult REQUIRED_RESULT = ValidationResult.KO("Required Field");

    protected W innerWidget;
    protected V initialValue;

    private String caption;
    private boolean required;
    private PElement captionSpan;

    protected PFlowPanel widget;
    protected FieldValidator validator;

    public FormField() {
        this(null, false);
    }

    public FormField(final boolean required) {
        this(null, required);
    }

    public FormField(final String caption) {
        this(caption, false);
    }

    public FormField(final String caption, final boolean required) {
        this.caption = caption;
        this.required = required;
    }

    public ValidationResult validate() {
        if (!isEnabled()) {
            clean();
            return OK_RESULT;
        }

        ValidationResult result = OK_RESULT;

        final String stringValue = getStringValue();

        if (required && Objects.requireNonNullElse(stringValue, "").isEmpty()) {
            result = REQUIRED_RESULT;
        } else if (validator != null) {
            result = validator.isValid(stringValue);
        }

        if (result.isValid()) {
            cleanError();
        } else {
            error(result.getErrorMessage());
        }

        checkDiff();

        return result;
    }

    public boolean hasDiff() {
        return widget.hasAttribute(ATTR_DIFF);
    }

    private void clean() {
        cleanError();
        cleanDiff();
    }

    public void reset() {
        clean();
        afterReset();
    }

    protected void error(final String message) {
        widget.setAttribute(ATTR_ERROR, message);
    }

    protected void cleanError() {
        widget.removeAttribute(ATTR_ERROR);
    }

    private void diff() {
        widget.setAttribute(ATTR_DIFF);
    }

    protected void cleanDiff() {
        widget.removeAttribute(ATTR_DIFF);
    }

    private void afterReset() {
        setValue(initialValue);
        checkDiff();
    }

    protected void checkDiff() {
        if (initialValue == null) {
            cleanDiff();
            return;
        }

        if (!initialValue.equals(getValue())) {
            diff();
        } else {
            cleanDiff();
        }
    }

    public void setValidator(final FieldValidator validator) {
        this.validator = validator;
    }

    public void setInitialValue(final V initialValue) {
        this.initialValue = initialValue;
        checkDiff();
    }

    public V getInitialValue() {
        return initialValue;
    }

    public void setValue(final V value, final boolean isInitialValue) {
        setValue(value);
        if (isInitialValue) {
            setInitialValue(value);
        }
    }

    public W getInnerWidget() {
        return innerWidget;
    }

    private void initGUI() {
        widget.addStyleName(STYLE_FORM_FIELD);
        if (required) widget.setAttribute(ATTR_REQUIRED);

        updateCaption();

        innerWidget = createInnerWidget();
        innerWidget.addStyleName(STYLE_INNER_WIDGET);
        widget.add(innerWidget);
    }

    private void updateCaption() {
        if (caption == null && captionSpan == null) {
            return;
        }

        if (caption == null) {
            captionSpan.setVisible(false);
            return;
        }

        if (captionSpan == null) {
            captionSpan = Element.newSpan();
            captionSpan.addStyleName(STYLE_CAPTION);
            widget.add(captionSpan);
        }

        captionSpan.setInnerText(caption);
        captionSpan.setVisible(true);

    }

    protected void afterInitGUI() {
    }

    public void setCaption(String caption) {
        this.caption = caption;
        updateCaption();
    }

    public String getCaption() {
        return caption;
    }

    @Override
    public PWidget asWidget() {
        if (widget == null) {
            widget = Element.newPFlowPanel();
            initGUI();
            afterInitGUI();
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
