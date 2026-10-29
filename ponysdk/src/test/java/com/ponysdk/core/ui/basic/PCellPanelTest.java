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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PCellPanelTest extends PSuite {

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
    public void testBorderAndSpacingSetBeforeAttachTravelInsideTheCreate() {
        final PVerticalPanel panel = Element.newPVerticalPanel();
        panel.setBorderWidth(2);
        panel.setSpacing(5);
        assertEquals(2, panel.getBorderWidth());
        assertEquals(5, panel.getSpacing());
        writer.clear();

        PWindow.getMain().add(panel);
        assertEquals(List.of(2), writer.valuesOf(ServerToClientModel.BORDER_WIDTH));
        assertEquals(List.of(5), writer.valuesOf(ServerToClientModel.SPACING));
        assertEquals(1, writer.count(ServerToClientModel.TYPE_CREATE));
    }

    @Test
    public void testBorderAndSpacingNotSentWhenUnset() {
        writer.clear();
        PWindow.getMain().add(Element.newPHorizontalPanel());
        assertEquals(0, writer.count(ServerToClientModel.BORDER_WIDTH));
        assertEquals(0, writer.count(ServerToClientModel.SPACING));
    }

    @Test
    public void testBorderAndSpacingUpdatedOnlyWhenChanged() {
        final PVerticalPanel panel = Element.newPVerticalPanel();
        PWindow.getMain().add(panel);
        writer.clear();
        panel.setBorderWidth(1);
        panel.setBorderWidth(1);
        panel.setSpacing(3);
        panel.setSpacing(3);
        assertEquals(List.of(1), writer.valuesOf(ServerToClientModel.BORDER_WIDTH));
        assertEquals(List.of(3), writer.valuesOf(ServerToClientModel.SPACING));
    }

    @Test
    public void testCellProperties() {
        final PVerticalPanel panel = Element.newPVerticalPanel();
        final PLabel label = Element.newPLabel();
        panel.add(label);
        PWindow.getMain().add(panel);
        writer.clear();

        panel.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_RIGHT);
        panel.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_BOTTOM);
        panel.setCellHeight(label, "10px");
        panel.setCellWidth(label, "20px");

        assertEquals(List.of(PHorizontalAlignment.ALIGN_RIGHT.getValue()),
            writer.valuesOf(ServerToClientModel.WIDGET_HORIZONTAL_ALIGNMENT));
        assertEquals(List.of(PVerticalAlignment.ALIGN_BOTTOM.getValue()), writer.valuesOf(ServerToClientModel.WIDGET_VERTICAL_ALIGNMENT));
        assertEquals(List.of("10px"), writer.valuesOf(ServerToClientModel.CELL_HEIGHT));
        assertEquals(List.of("20px"), writer.valuesOf(ServerToClientModel.CELL_WIDTH));
        assertEquals(List.of(label.getID(), label.getID(), label.getID(), label.getID()),
            writer.valuesOf(ServerToClientModel.WIDGET_ID));
    }

}
