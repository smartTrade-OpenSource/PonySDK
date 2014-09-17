
package com.ponysdk.ui.server.form2.formfield;

import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PWidget;

public class RadioButtonFormField extends FormField<Boolean> {

    private final PRadioButton radioButton;

    public RadioButtonFormField(final String name) {
        this(new PRadioButton(name));
    }

    public RadioButtonFormField(final String name, final String label) {
        this(new PRadioButton(name, label));
    }

    public RadioButtonFormField(final PRadioButton radioButton) {
        super(null);
        this.radioButton = radioButton;
    }

    @Override
    public PWidget asWidget() {
        return radioButton;
    }

    @Override
    public void reset0() {
        radioButton.setValue(false);
    }

    @Override
    public Boolean getValue() {
        return radioButton.getValue();
    }

    @Override
    public void setValue(final Boolean value) {
        radioButton.setValue(value);
    }

    @Override
    protected String getStringValue() {
        return radioButton.getValue().toString();
    }

    public PRadioButton getRadioButton() {
        return radioButton;
    }

}
