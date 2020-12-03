package com.ponysdk.core.ui.form2.impl.formfield;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.form2.api.FormField;

public abstract class AbstractInputFormField<V> extends FormField<V> {
    protected PTextBox input;

    public AbstractInputFormField(String caption) {
        super(caption);
    }

    @Override
    protected PTextBox createInnerWidget() {
        input = Element.newPTextBox();
        input.setTabindex(PWidget.TabindexMode.TABULABLE);
        input.addBlurHandler(e -> validate());
        input.addValueChangeHandler(e -> validate());
        return input;
    }

    @Override
    protected void afterInitialisation() {
        super.afterInitialisation();
        commit(); //commit the default value
    }

    @Override
    public void enable() {
        input.removeAttribute("disabled");
    }

    @Override
    public void disable() {
        input.setAttribute("disabled");
    }

    @Override
    public boolean isEnabled() {
        return !input.hasAttribute("disabled");
    }

    @Override
    public String getStringToValidate() {
        return input.getText();
    }

}
