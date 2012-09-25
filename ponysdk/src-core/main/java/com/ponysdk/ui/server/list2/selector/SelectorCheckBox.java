
package com.ponysdk.ui.server.list2.selector;

import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;

public class SelectorCheckBox<T> extends PCheckBox implements Selectable<T> {

    @Override
    public void select() {
        setValue(true);
        onValueChange(new PValueChangeEvent<Boolean>(this, true));
    }

    @Override
    public void unselect() {
        setValue(false);
        onValueChange(new PValueChangeEvent<Boolean>(this, false));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getSelectedData() {
        if (getValue()) return (T) data;
        else return null;
    }

}