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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PCheckBoxTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
        // testAttach counts the widgets of the root panel
        PWindow.getMain().getPRootPanel().clear();
    }

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
    @Test
    public void stateIsStackedBeforeAttachAndSentOnceAfter() {
        final PCheckBox checkBox = Element.newPCheckBox("label");
        checkBox.setState(PCheckBoxState.INDETERMINATE);
        assertTrue(writer.isEmpty());
        PWindow.getMain().add(checkBox);
        assertEquals(List.of(PCheckBoxState.INDETERMINATE.getValue()), writer.valuesOf(ServerToClientModel.VALUE_CHECKBOX));
        assertTrue(writer.valuesOf(ServerToClientModel.TEXT).contains("label"));

        writer.clear();
        checkBox.setValue(true);
        checkBox.setValue(true);
        checkBox.setValue(null);
        assertEquals(List.of(PCheckBoxState.CHECKED.getValue(), PCheckBoxState.UNCHECKED.getValue()),
            writer.valuesOf(ServerToClientModel.VALUE_CHECKBOX));
        assertTrue(checkBox.toString().endsWith("state=UNCHECKED"));
    }

    @Test
    public void inputAttributesAreSentOnChangeOnly() {
        final PCheckBox checkBox = Element.newPCheckBox();
        PWindow.getMain().add(checkBox);
        writer.clear();

        checkBox.setInputAttribute(null, "ignored");
        checkBox.setInputAttribute("aria-label", "agree");
        checkBox.setInputAttribute("aria-label", "agree");
        assertEquals(List.of("agree"), writer.valuesAfter(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY, "aria-label"));
        assertEquals(1, writer.count(ServerToClientModel.PUT_INPUT_ATTRIBUTE_KEY));

        checkBox.removeInputAttribute("aria-label");
        checkBox.removeInputAttribute("aria-label");
        checkBox.removeInputAttribute("unknown");
        assertEquals(List.of("aria-label"), writer.valuesOf(ServerToClientModel.REMOVE_INPUT_ATTRIBUTE_KEY));
    }

    @Test
    public void aTerminalValueChangeNotifiesOnlyOnRealChangeAndWithoutEcho() {
        final PCheckBox checkBox = Element.newPCheckBox();
        PWindow.getMain().add(checkBox);
        final List<Boolean> values = new ArrayList<>();
        final PValueChangeHandler<Boolean> handler = e -> values.add(e.getData());
        checkBox.addValueChangeHandler(handler);
        assertEquals(1, checkBox.getValueChangeHandlers().size());
        writer.clear();

        PEmulator.valueChange(checkBox, true);
        PEmulator.valueChange(checkBox, true);
        assertEquals(List.of(true), values);
        assertEquals(PCheckBoxState.CHECKED, checkBox.getState());
        assertTrue(writer.isEmpty());

        assertTrue(checkBox.removeValueChangeHandler(handler));
        assertFalse(checkBox.removeValueChangeHandler(handler));
        PEmulator.valueChange(checkBox, false);
        assertFalse(checkBox.getValue());
        assertEquals(1, values.size());
    }

    @Test
    public void aDisabledOrHiddenCheckBoxIgnoresTheTerminal() {
        final PCheckBox checkBox = Element.newPCheckBox();
        assertTrue(checkBox.getValueChangeHandlers().isEmpty());
        assertFalse(checkBox.removeValueChangeHandler(e -> {
        }));
        checkBox.setEnabled(false);
        PEmulator.valueChange(checkBox, true);
        assertFalse(checkBox.getValue());
        checkBox.setEnabled(true);
        checkBox.setVisible(false);
        PEmulator.valueChange(checkBox, true);
        assertFalse(checkBox.getValue());
    }

    @Test
    public void otherTerminalDataReachesTheDomHandlers() {
        final PCheckBox checkBox = Element.newPCheckBox();
        PWindow.getMain().add(checkBox);
        final AtomicInteger clicks = new AtomicInteger();
        checkBox.addClickHandler(e -> clicks.incrementAndGet());
        PEmulator.click(checkBox);
        checkBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_STRING_VALUE_CHANGE.toStringValue(), "x").build());
        assertEquals(1, clicks.get());
        assertFalse(checkBox.getValue());
    }

    @Test
    public void aCheckBoxWithoutTextOrHandlerStillChanges() {
        final PCheckBox checkBox = Element.newPCheckBox();
        checkBox.setHTML("<b>h</b>");
        assertTrue(checkBox.dumpDOM().endsWith("><b>h</b></input>"), checkBox.dumpDOM());
        checkBox.onValueChange(new com.ponysdk.core.ui.basic.event.PValueChangeEvent<>(checkBox, true));
        assertEquals(PCheckBoxState.CHECKED, checkBox.getState());
    }

    @Test
    public void aTextCheckBoxDumpsItsText() {
        assertTrue(Element.newPCheckBox("t").dumpDOM().endsWith(">t</input>"));
    }
}
