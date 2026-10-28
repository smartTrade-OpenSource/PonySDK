/*
 * Copyright (c) 2026 PonySDK
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;
import static org.junit.jupiter.api.Assertions.*;

public class PRadioButtonGroupTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    private static PRadioButton attachedRadio(final String label) {
        final PRadioButton radio = Element.newPRadioButton(label);
        PWindow.getMain().add(radio);
        return radio;
    }

    @Test
    public void addedButtonsTakeTheGroupName() {
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("colors");
        assertEquals("colors", group.getName());
        final PRadioButton red = attachedRadio("red");
        final PRadioButton blue = attachedRadio("blue");
        writer.clear();

        group.addRadioButton(red);
        group.addRadioButton(blue);
        assertEquals(List.of("colors", "colors"), writer.valuesOf(ServerToClientModel.NAME));

        writer.clear();
        group.setName("colors");
        assertTrue(writer.isEmpty());
        group.setName("shades");
        assertEquals("shades", group.getName());
        assertEquals(List.of("shades", "shades"), writer.valuesOf(ServerToClientModel.NAME));
    }

    @Test
    public void checkingOneButtonUnchecksTheOthersSilentlyAndNotifiesTheGroup() {
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("g");
        final PRadioButton a = attachedRadio("a");
        final PRadioButton b = attachedRadio("b");
        final PRadioButton c = attachedRadio("c");
        Stream.of(a, b, c).forEach(group::addRadioButton);
        final List<Object> notified = new ArrayList<>();
        final PValueChangeHandler<Boolean> handler = e -> notified.add(e.getSource());
        group.addValueChangeHandler(handler);

        PEmulator.valueChange(a, true);
        assertTrue(a.getValue());
        writer.clear();
        b.setState(PCheckBoxState.CHECKED);

        assertTrue(b.getValue());
        assertFalse(a.getValue());
        assertFalse(c.getValue());
        assertEquals(List.of(a, b), notified);
        // only b is sent: the terminal unchecks the others of the same name itself
        assertEquals(List.of(PCheckBoxState.CHECKED.getValue()), writer.valuesOf(ServerToClientModel.VALUE_CHECKBOX));

        assertTrue(group.removeValueChangeHandler(handler));
        assertFalse(group.removeValueChangeHandler(handler));
        c.setState(PCheckBoxState.CHECKED);
        assertEquals(2, notified.size());
        assertFalse(b.getValue());
    }

    @Test
    public void aRemovedButtonIsNoLongerUnchecked() {
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("g");
        final PRadioButton a = attachedRadio("a");
        final PRadioButton b = attachedRadio("b");
        group.addRadioButton(a);
        group.addRadioButton(b);
        a.setState(PCheckBoxState.CHECKED);

        group.removeRadioButton(a);
        b.setState(PCheckBoxState.CHECKED);

        assertTrue(a.getValue());
        assertTrue(b.getValue());
    }

    @Test
    public void handlersAndRemovalOnAnEmptyGroupAreNoOps() {
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("g");
        assertFalse(group.removeValueChangeHandler(e -> {
        }));
        group.removeRadioButton(Element.newPRadioButton());
        assertEquals("g", group.getName());
    }

    @Test
    public void anEmptyGroupCanBeRenamed() {
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("g");
        group.setName("h");
        assertEquals("h", group.getName());
    }

    @Test
    public void uncheckingAButtonNeverChecksSeveralOthers() {
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("g");
        final PRadioButton a = attachedRadio("a");
        final PRadioButton b = attachedRadio("b");
        final PRadioButton c = attachedRadio("c");
        Stream.of(a, b, c).forEach(group::addRadioButton);
        a.setState(PCheckBoxState.CHECKED);

        a.setState(PCheckBoxState.UNCHECKED);

        assertTrue(Stream.of(a, b, c).filter(PCheckBox::getValue).count() <= 1, "a radio group has at most one checked button");
    }

    @Test
    public void everyHandlerIsKept() {
        final PRadioButtonGroup group = Element.newPRadioButtonGroup("g");
        final List<Boolean> values = new ArrayList<>();
        group.addValueChangeHandler(event -> values.add(event.getData()));
        group.addValueChangeHandler(event -> values.add(event.getData()));
        final PRadioButton button = Element.newPRadioButton("b");
        group.addRadioButton(button);
        button.onValueChange(new com.ponysdk.core.ui.basic.event.PValueChangeEvent<>(button, true));
        assertEquals(List.of(true, true), values);
    }
}
