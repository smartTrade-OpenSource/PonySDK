/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.binder;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PWidget;

/**
 * What a field shows of its error, for the eyes and for a screen reader: {@code aria-invalid="true"} on the field,
 * {@code aria-describedby} naming a message element placed after it, the message in that element
 * (docs/decisions/0021-forms-binder.md). Attributes only: nothing new on the wire.
 */
final class FieldDisplay {

    static final String INVALID = "aria-invalid";
    static final String DESCRIBED_BY = "aria-describedby";
    static final String REQUIRED = "aria-required";
    static final String MESSAGE_STYLE = "pony-FieldError";

    private final PWidget widget;
    private PElement message;
    private boolean shown;

    FieldDisplay(final PWidget widget) {
        this.widget = widget;
    }

    void required() {
        put(REQUIRED, "true");
    }

    /** Shows {@code text} as the field's error, or clears the error when it is null. */
    void show(final String text) {
        if (text == null) {
            clear();
            return;
        }
        // A date box's terminal drops the attribute when it parses the text, and the server's copy would not know
        if (widget instanceof PDateBox) remove(INVALID);
        put(INVALID, "true");
        final PElement element = message();
        if (element != null) {
            element.setInnerText(text);
            element.setVisible(true);
            put(DESCRIBED_BY, element.getAttribute("id"));
        }
        shown = true;
    }

    private void clear() {
        if (!shown) return;
        shown = false;
        remove(INVALID);
        remove(DESCRIBED_BY);
        if (message != null) message.setVisible(false);
    }

    /** The message element, created on the first error and placed after the field; null where it cannot be placed. */
    private PElement message() {
        if (message == null && widget.getParent() instanceof PComplexPanel parent) {
            final PElement element = Element.newDiv();
            element.addStyleName(MESSAGE_STYLE);
            element.setAttribute("role", "alert"); // an alert put in the page is announced
            element.setAttribute("id", "pony-error-" + element.getID());
            parent.insert(element, parent.getWidgetIndex(widget) + 1);
            message = element;
        }
        return message;
    }

    // A check box's attributes belong on its input, the element that is read and focused: its root is a label
    private void put(final String name, final String value) {
        if (widget instanceof PCheckBox box) box.setInputAttribute(name, value);
        else widget.setAttribute(name, value);
    }

    private void remove(final String name) {
        if (widget instanceof PCheckBox box) box.removeInputAttribute(name);
        else widget.removeAttribute(name);
    }

}
