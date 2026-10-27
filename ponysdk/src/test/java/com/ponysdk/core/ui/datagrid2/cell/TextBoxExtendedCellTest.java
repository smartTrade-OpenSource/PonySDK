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
package com.ponysdk.core.ui.datagrid2.cell;

import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.datagrid2.DomEvents;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextBoxExtendedCellTest extends PSuite {

    private final List<String> edits = new ArrayList<>();
    private TextBoxExtendedCell<String> cell;
    private PTextBox textBox;
    private ExtendedCellController<String> controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        edits.clear();
        cell = new TextBoxExtendedCell<>("initial", (v, text) -> edits.add(v + "=" + text), 60);
        textBox = (PTextBox) cell.asWidget();
        controller = Mockito.mock(ExtendedCellController.class);
    }

    @Test
    public void textBoxIsInitialized() {
        Assertions.assertEquals("initial", textBox.getText());
        Assertions.assertEquals("60px", textBox.getWidth());
        Assertions.assertTrue(textBox.isEnabledOnRequest());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void enterLeavesExtendedModeAndAppliesEdit() {
        cell.setController(controller);
        DomEvents.stringValueChange(textBox, "edited");

        DomEvents.keyUp(textBox, PKeyCodes.ENTER.getCode());

        Mockito.verify(controller).setPrimaryMode();
        final ArgumentCaptor<Consumer<String>> updater = ArgumentCaptor.forClass(Consumer.class);
        Mockito.verify(controller).updateValue(updater.capture());
        updater.getValue().accept("row");
        Assertions.assertEquals(List.of("row=edited"), edits);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void escapeLeavesExtendedModeWithoutEdit() {
        cell.setController(controller);

        DomEvents.keyUp(textBox, PKeyCodes.ESCAPE.getCode());

        Mockito.verify(controller).setPrimaryMode();
        Mockito.verify(controller, Mockito.never()).updateValue(Mockito.any(Consumer.class));
    }

    @Test
    public void otherKeysAreIgnored() {
        cell.setController(controller);

        DomEvents.keyUp(textBox, PKeyCodes.SPACE.getCode());

        Mockito.verifyNoInteractions(controller);
    }

    @Test
    public void blurLeavesExtendedMode() {
        cell.setController(controller);

        DomEvents.domEvent(textBox, DomHandlerType.BLUR);

        Mockito.verify(controller).setPrimaryMode();
    }

    @Test
    public void eventsWithoutControllerAreIgnored() {
        DomEvents.domEvent(textBox, DomHandlerType.BLUR);
        DomEvents.keyUp(textBox, PKeyCodes.ENTER.getCode());
        DomEvents.domEvent(textBox, DomHandlerType.CLICK);

        Mockito.verifyNoInteractions(controller);
        Assertions.assertTrue(edits.isEmpty());
    }

    @Test
    public void renderSelectAndUnselectDoNotChangeText() {
        cell.render("v", "helper");
        cell.select();
        cell.unselect();

        Assertions.assertEquals("initial", textBox.getText());
    }
}
