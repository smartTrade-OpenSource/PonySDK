/*
 * Copyright (c) 2018 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PCheckBoxTest extends PSuite {

    @Test
    public void testInit() {
        final PCheckBox widget = Element.newPCheckBox();
        assertEquals(WidgetType.CHECKBOX, widget.getWidgetType());
        assertNotNull(widget.toString());
    }

    @Test
    public void testAttach() {
        final PCheckBox widget = Element.newPCheckBox();
        PWindow.getMain().add(widget);
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 1);
        widget.removeFromParent();
        assertEquals(PWindow.getMain().getPRootPanel().getWidgetCount(), 0);
    }

    @Test
    public void testLabel() {
        final PCheckBox w1 = Element.newPCheckBox("label");
        assertEquals(w1.getText(), "label");

        final PCheckBox w2 = Element.newPCheckBox();
        w2.setText("label");
        assertEquals(w2.getText(), "label");
    }

    @Test
    public void testState() {
        final PCheckBox widget = Element.newPCheckBox();
        assertEquals(widget.getState(), PCheckBoxState.UNCHECKED);
        widget.setState(PCheckBoxState.CHECKED);
        assertEquals(widget.getState(), PCheckBoxState.CHECKED);
        widget.setState(PCheckBoxState.UNCHECKED);
        assertEquals(widget.getState(), PCheckBoxState.UNCHECKED);
        widget.setState(PCheckBoxState.INDETERMINATE);
        assertEquals(widget.getState(), PCheckBoxState.INDETERMINATE);
    }

    @Test
    public void testValue() {
        final PCheckBox w1 = Element.newPCheckBox();
        assertFalse(w1.getValue());
        w1.setValue(true);
        assertTrue(w1.getValue());
        w1.setValue(false);
        assertFalse(w1.getValue());
    }

    @Test
    public void testValueChangeHandler() {
        final PCheckBox w1 = Element.newPCheckBox();
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        PValueChangeHandler<Boolean> handler = event -> handlerFiredCount.incrementAndGet();
        w1.addValueChangeHandler(handler);
        assertEquals(w1.getValueChangeHandlers().size(), 1);
        w1.removeValueChangeHandler(handler);
        assertEquals(w1.getValueChangeHandlers().size(), 0);
        w1.addValueChangeHandler(handler);
        assertEquals(w1.getValueChangeHandlers().size(), 1);

        PEmulator.valueChange(w1, true);
        assertEquals(handlerFiredCount.get(), 1);
        PEmulator.valueChange(w1, false);
        assertEquals(handlerFiredCount.get(), 2);
    }

    @Test
    public void testDumDOM() {
        final PCheckBox checkBox = Element.newPCheckBox();
        String ID = String.valueOf(checkBox.getID());

        assertEquals("<input type=\"checkbox\" pid=\"" + ID + "\" class=\"\"></input>", checkBox.dumpDOM());

        checkBox.setHTML("<a>html</a>");
        assertEquals("<input type=\"checkbox\" pid=\"" + ID + "\" class=\"\"><a>html</a></input>", checkBox.dumpDOM());

        checkBox.setHTML("text");
        assertEquals("<input type=\"checkbox\" pid=\"" + ID + "\" class=\"\">text</input>", checkBox.dumpDOM());

        checkBox.addStyleName("style");
        assertEquals("<input type=\"checkbox\" pid=\"" + ID + "\" class=\"style\">text</input>", checkBox.dumpDOM());

        checkBox.setState(PCheckBoxState.CHECKED);
        assertEquals("<input type=\"checkbox\" pid=\"" + ID + "\" checked class=\"style\">text</input>", checkBox.dumpDOM());
    }
}
