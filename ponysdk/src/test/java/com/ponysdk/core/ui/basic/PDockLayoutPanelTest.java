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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.PDirection;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PDockLayoutPanelTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }


    @Test
    public void testInit() {
        final PDockLayoutPanel panel = Element.newPDockLayoutPanel(PUnit.EM);
        assertEquals(WidgetType.DOCK_LAYOUT_PANEL, panel.getWidgetType());
        assertEquals(PUnit.EM, panel.getUnit());
        PWindow.getMain().add(panel);
        assertTrue(writer.valuesOf(ServerToClientModel.UNIT).contains(PUnit.EM.getByteValue()));
    }

    @Test
    public void testAddInEachDirection() {
        final PDockLayoutPanel panel = Element.newPDockLayoutPanel(PUnit.PX);
        PWindow.getMain().add(panel);

        writer.clear();
        panel.addNorth(Element.newPLabel("n"), 10);
        assertTrue(writer.valuesOf(ServerToClientModel.DIRECTION).contains(PDirection.NORTH.getValue()));
        assertTrue(writer.valuesOf(ServerToClientModel.SIZE).contains(10.0));
        panel.addSouth(Element.newPLabel("s"), 11);
        assertTrue(writer.valuesOf(ServerToClientModel.DIRECTION).contains(PDirection.SOUTH.getValue()));
        panel.addEast(Element.newPLabel("e"), 12);
        assertTrue(writer.valuesOf(ServerToClientModel.DIRECTION).contains(PDirection.EAST.getValue()));
        panel.addWest(Element.newPLabel("w"), 13);
        assertTrue(writer.valuesOf(ServerToClientModel.DIRECTION).contains(PDirection.WEST.getValue()));
        panel.addLineEnd(Element.newPLabel("le"), 14);
        assertTrue(writer.valuesOf(ServerToClientModel.DIRECTION).contains(PDirection.LINE_END.getValue()));
        panel.addLineStart(Element.newPLabel("ls"), 15);
        assertTrue(writer.valuesOf(ServerToClientModel.DIRECTION).contains(PDirection.LINE_START.getValue()));
        final PLabel center = Element.newPLabel("c");
        panel.add(center);
        assertTrue(writer.valuesOf(ServerToClientModel.DIRECTION).contains(PDirection.CENTER.getValue()));
        assertTrue(writer.valuesOf(ServerToClientModel.SIZE).contains(0.0));

        assertEquals(7, panel.getWidgetCount());
        assertSame(panel, center.getParent());
        assertTrue(center.isInitialized());
    }

    @Test
    public void testAddMovesWidgetFromPreviousParent() {
        final PFlowPanel previous = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("l");
        previous.add(label);
        final PDockLayoutPanel panel = Element.newPDockLayoutPanel(PUnit.PX);
        panel.addNorth(label, 5);
        assertEquals(0, previous.getWidgetCount());
        assertSame(panel, label.getParent());
    }

    @Test
    public void testWidgetSizeHiddenAndAnimate() {
        final PDockLayoutPanel panel = Element.newPDockLayoutPanel(PUnit.PX);
        PWindow.getMain().add(panel);
        final PLabel label = Element.newPLabel("l");
        panel.addWest(label, 20);

        writer.clear();
        panel.setWidgetSize(label, 42);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_SIZE).contains(42.0));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_ID).contains(label.getID()));

        panel.setWidgetHidden(label, true);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_HIDDEN).contains(true));

        panel.animate(Duration.ofMillis(100));
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATE).contains(100));
    }
}
