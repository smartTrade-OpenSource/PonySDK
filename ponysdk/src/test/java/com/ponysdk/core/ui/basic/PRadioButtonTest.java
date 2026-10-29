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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.PCheckBoxState;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PRadioButtonTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    @Test
    public void aRadioButtonIsACheckBoxOfItsOwnType() {
        final PRadioButton radio = Element.newPRadioButton("r");
        assertEquals(WidgetType.RADIO_BUTTON, radio.getWidgetType());
        assertEquals("r", radio.getText());
        PWindow.getMain().add(radio);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_TYPE).contains(WidgetType.RADIO_BUTTON.getValue()));
    }

    @Test
    public void aRadioButtonCannotBeIndeterminate() {
        final PRadioButton radio = Element.newPRadioButton();
        assertThrows(IllegalArgumentException.class, () -> radio.setState(PCheckBoxState.INDETERMINATE));
        assertEquals(PCheckBoxState.UNCHECKED, radio.getState());
    }

    @Test
    public void setStateSendsAndNotifiesOnChangeOnly() {
        final PRadioButton radio = Element.newPRadioButton();
        PWindow.getMain().add(radio);
        final List<Boolean> values = new ArrayList<>();
        radio.addValueChangeHandler(e -> values.add(e.getData()));
        writer.clear();

        radio.setState(PCheckBoxState.CHECKED);
        radio.setState(PCheckBoxState.CHECKED);
        radio.setValue(false);

        assertEquals(List.of(true, false), values);
        assertEquals(List.of(PCheckBoxState.CHECKED.getValue(), PCheckBoxState.UNCHECKED.getValue()),
            writer.valuesOf(ServerToClientModel.VALUE_CHECKBOX));
    }

    @Test
    public void aSilentStateChangeNeitherSendsNorNotifies() {
        final PRadioButton radio = Element.newPRadioButton();
        PWindow.getMain().add(radio);
        final List<Boolean> values = new ArrayList<>();
        radio.addValueChangeHandler(e -> values.add(e.getData()));
        writer.clear();

        radio.setState(PCheckBoxState.CHECKED, false);
        assertEquals(PCheckBoxState.CHECKED, radio.getState());
        assertTrue(values.isEmpty());
        assertTrue(writer.isEmpty());

        radio.setState(PCheckBoxState.UNCHECKED, true, false);
        assertTrue(values.isEmpty());
        assertEquals(List.of(PCheckBoxState.UNCHECKED.getValue()), writer.valuesOf(ServerToClientModel.VALUE_CHECKBOX));
    }

    @Test
    public void withoutHandlerAStateChangeIsOnlySent() {
        final PRadioButton radio = Element.newPRadioButton();
        PWindow.getMain().add(radio);
        writer.clear();
        radio.setState(PCheckBoxState.CHECKED);
        assertEquals(List.of(PCheckBoxState.CHECKED.getValue()), writer.valuesOf(ServerToClientModel.VALUE_CHECKBOX));
    }

    @Test
    public void theNameIsSentOnChangeOnly() {
        final PRadioButton radio = Element.newPRadioButton();
        PWindow.getMain().add(radio);
        writer.clear();
        radio.setName("group");
        radio.setName("group");
        assertEquals(List.of("group"), writer.valuesOf(ServerToClientModel.NAME));
    }

}
