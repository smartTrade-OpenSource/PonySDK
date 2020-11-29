package com.ponysdk.core.ui.form2.api;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PWidget;

public class FormFieldPanel extends PFlowPanel {
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_DIFF = "dirty";

    private final PElement captionSpan;

    protected PWidget innerWidget;

    FormFieldPanel() {
        addStyleName("form-field");
        captionSpan = Element.newSpan();
        captionSpan.addStyleName("caption");
        add(captionSpan);
    }

    void addInnerWidget(PWidget innerWidget) {
        this.innerWidget = innerWidget;
        innerWidget.addStyleName("inner-widget");
        add(innerWidget);
    }

    void applyErrorStyle(final String message) {
        setAttribute(ATTR_ERROR, message);
    }

    void removeErrorStyle() {
        removeAttribute(ATTR_ERROR);
    }

    void applyDirtyStyle() {
        setAttribute(ATTR_DIFF,"true");
    }

    void removeDirtyStyle() {
        removeAttribute(ATTR_DIFF);
    }

    void setCaption(String caption) {
        if (caption == null) {
            captionSpan.setVisible(false);
            return;
        }
        captionSpan.setInnerText(caption);
        captionSpan.setVisible(true);
    }
}