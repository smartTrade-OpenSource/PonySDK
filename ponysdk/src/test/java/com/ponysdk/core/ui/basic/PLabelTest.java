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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PLabelTest extends PSuite {

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
    public void stateSetBeforeAttachTravelsInTheCreate() {
        final PLabel label = Element.newPLabel("text");
        label.setAttributeLinkedToValue("data-value");
        assertEquals("data-value", label.getAttributeLinkedToValue());
        assertTrue(writer.isEmpty());

        PWindow.getMain().add(label);

        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_TYPE).contains(WidgetType.LABEL.getValue()));
        assertEquals(List.of("text"), writer.valuesOf(ServerToClientModel.TEXT));
        assertEquals(List.of("data-value"), writer.valuesOf(ServerToClientModel.ATTRIBUTE_LINKED_TO_VALUE));
    }

    @Test
    public void changesAfterAttachAreSentOnce() {
        final PLabel label = Element.newPLabel();
        assertNull(label.getText());
        PWindow.getMain().add(label);
        assertEquals(0, writer.count(ServerToClientModel.TEXT));
        writer.clear();

        label.setText("a");
        label.setText("a");
        label.setAttributeLinkedToValue("v");
        label.setAttributeLinkedToValue("v");

        assertEquals("a", label.getText());
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.TEXT));
        assertEquals(List.of("v"), writer.valuesOf(ServerToClientModel.ATTRIBUTE_LINKED_TO_VALUE));
        assertTrue(label.toString().endsWith("text=a"));
    }

    @Test
    public void everyDomHandlerIsRegisteredOnTheTerminal() {
        final PLabel label = Element.newPLabel("l");
        PWindow.getMain().add(label);
        writer.clear();

        final List<String> clicks = new ArrayList<>();
        label.addClickHandler(e -> clicks.add("click"));
        label.addDoubleClickHandler(e -> {
        });
        label.addDragEndHandler(e -> {
        });
        label.addDragEnterHandler(e -> {
        });
        label.addDragStartHandler(e -> {
        });
        label.addDragLeaveHandler(e -> {
        });
        label.addDragOverHandler(e -> {
        });
        label.addDropHandler(e -> {
        });

        assertEquals(8, writer.count(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(8, writer.valuesOf(ServerToClientModel.HANDLER_TYPE).stream().distinct().count());

        PEmulator.click(label);
        assertEquals(List.of("click"), clicks);
    }

    @Test
    public void dumpDomShowsTextStylesAndVisibility() {
        final PLabel label = Element.newPLabel("hello");
        label.addStyleName("s");
        assertEquals("<label pid=\"" + label.getID() + "\" class=\"s\">hello</label>", label.dumpDOM());
        label.setVisible(false);
        assertEquals("<label pid=\"" + label.getID() + "\" hidden class=\"s\">hello</label>", label.dumpDOM());
    }

}
