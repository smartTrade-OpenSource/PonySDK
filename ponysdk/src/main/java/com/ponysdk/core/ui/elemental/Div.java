package com.ponysdk.core.ui.elemental;

import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.PObject;


public class Div extends PObject{

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DIV;
    }
}