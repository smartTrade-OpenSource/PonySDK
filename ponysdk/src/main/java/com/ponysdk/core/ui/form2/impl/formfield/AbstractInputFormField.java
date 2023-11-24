
package com.ponysdk.core.ui.form2.impl.formfield;

import java.util.HashSet;
import java.util.Set;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form2.api.FormField;

public abstract class AbstractInputFormField<V> extends FormField<V> {

    protected PTextBox input;

    private final Set<PValueChangeHandler<V>> handlers = new HashSet<>();

    public AbstractInputFormField(String caption) {
        super(caption);
    }

    @Override
    protected PTextBox createInnerWidget() {
        input = Element.newPTextBox();
        input.setTabindex(PWidget.TabindexMode.TABULABLE);
        input.addBlurHandler(e -> validate());
        input.addValueChangeHandler(e -> onChanged());
        return input;
    }

    private void onChanged() {
        validate();
        for (final PValueChangeHandler<V> handler : handlers) {
            handler.onValueChange(new PValueChangeEvent<>(this, getValue()));
        }
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

    public void addValueChangeHandler(PValueChangeHandler<V> handler) {
        handlers.add(handler);
    }

    public boolean removeValueChangeHandler(final PValueChangeHandler<V> handler) {
        return handlers != null && handlers.remove(handler);
    }    
    
    public String getPlaceHolder() {
    	return input.getPlaceholder();
    }
    
    public void setPlaceHolder(String value) {
    	input.setPlaceholder(value);
    }

    @Override
    public void focus() {
        input.focus();
    }

    @Override
    public void blur() {
        input.blur();
    }
}
