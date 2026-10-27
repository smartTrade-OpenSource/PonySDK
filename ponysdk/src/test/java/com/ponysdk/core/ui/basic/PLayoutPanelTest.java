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

import com.ponysdk.core.model.PAlignment;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PLayoutPanelTest extends PSuite {

    private CapturingModelWriter writer;
    private PLayoutPanel panel;
    private PLabel child;

    @BeforeEach
    public void setUp() {
        writer = CapturingModelWriter.install();
        panel = Element.newPLayoutPanel();
        PWindow.getMain().add(panel);
        child = Element.newPLabel("child");
        panel.add(child);
        writer.clear();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    @Test
    public void testInit() {
        assertEquals(WidgetType.LAYOUT_PANEL, panel.getWidgetType());
        assertSame(panel, child.getParent());
    }

    @Test
    public void testPositions() {
        panel.setWidgetHorizontalPosition(child, PAlignment.END);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_HORIZONTAL_ALIGNMENT).contains(PAlignment.END.getValue()));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_ID).contains(child.getID()));

        panel.setWidgetVerticalPosition(child, PAlignment.STRETCH);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_VERTICAL_ALIGNMENT).contains(PAlignment.STRETCH.getValue()));

        panel.setWidgetHidden(child, true);
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_HIDDEN).contains(true));
    }

    @Test
    public void testLeftRight() {
        panel.setWidgetLeftRight(child, 1, 2, PUnit.PCT);
        assertTrue(writer.valuesOf(ServerToClientModel.UNIT).contains(PUnit.PCT.getByteValue()));
        assertTrue(writer.valuesOf(ServerToClientModel.LEFT).contains(1.0));
        assertTrue(writer.valuesOf(ServerToClientModel.RIGHT).contains(2.0));
    }

    @Test
    public void testLeftWidthAndRightWidth() {
        panel.setWidgetLeftWidth(child, 3, 4, PUnit.PX);
        assertTrue(writer.valuesOf(ServerToClientModel.LEFT).contains(3.0));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDTH).contains(4.0));
        writer.clear();
        panel.setWidgetRightWidth(child, 5, 6, PUnit.PX);
        assertTrue(writer.valuesOf(ServerToClientModel.RIGHT).contains(5.0));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDTH).contains(6.0));
    }

    @Test
    public void testTopBottomHeight() {
        panel.setWidgetTopBottom(child, 7, 8, PUnit.EM);
        assertTrue(writer.valuesOf(ServerToClientModel.TOP).contains(7.0));
        assertTrue(writer.valuesOf(ServerToClientModel.BOTTOM).contains(8.0));
        writer.clear();
        panel.setWidgetTopHeight(child, 9, 10, PUnit.EM);
        assertTrue(writer.valuesOf(ServerToClientModel.TOP).contains(9.0));
        assertTrue(writer.valuesOf(ServerToClientModel.HEIGHT).contains(10.0));
        writer.clear();
        panel.setWidgetBottomHeight(child, 11, 12, PUnit.EM);
        assertTrue(writer.valuesOf(ServerToClientModel.BOTTOM).contains(11.0));
        assertTrue(writer.valuesOf(ServerToClientModel.HEIGHT).contains(12.0));
    }

    @Test
    public void testAnimate() {
        panel.animate(Duration.ofSeconds(1));
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATE).contains(1000));
    }

    @Test
    public void testRequiresChild() {
        final PLabel stranger = Element.newPLabel("s");
        assertThrows(IllegalStateException.class, () -> panel.setWidgetHorizontalPosition(stranger, PAlignment.BEGIN));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetVerticalPosition(stranger, PAlignment.BEGIN));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetHidden(stranger, true));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetLeftRight(stranger, 0, 0, PUnit.PX));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetLeftWidth(stranger, 0, 0, PUnit.PX));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetRightWidth(stranger, 0, 0, PUnit.PX));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetTopBottom(stranger, 0, 0, PUnit.PX));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetTopHeight(stranger, 0, 0, PUnit.PX));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetBottomHeight(stranger, 0, 0, PUnit.PX));
        assertTrue(writer.isEmpty());
    }
}
