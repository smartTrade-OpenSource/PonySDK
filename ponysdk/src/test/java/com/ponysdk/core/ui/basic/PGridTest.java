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

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PGridTest extends PSuite {

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
        final PGrid grid = Element.newPGrid();
        assertEquals(WidgetType.GRID, grid.getWidgetType());
        assertEquals(0, grid.getRows());
        assertEquals(0, grid.getColumns());
        assertNotNull(grid.getCellFormatter());
    }

    @Test
    public void testCreationWithSize() {
        final PGrid grid = Element.newPGrid(3, 4);
        assertEquals(3, grid.getRows());
        assertEquals(4, grid.getColumns());
        PWindow.getMain().add(grid);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(grid.getID()));
        assertEquals(List.of(3), writer.valuesOf(ServerToClientModel.ROW));
        assertEquals(List.of(4), writer.valuesOf(ServerToClientModel.COLUMN));
    }

    @Test
    public void testCreationWithoutSize() {
        final PGrid grid = Element.newPGrid();
        PWindow.getMain().add(grid);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(grid.getID()));
        assertEquals(0, writer.count(ServerToClientModel.ROW));
        assertEquals(0, writer.count(ServerToClientModel.COLUMN));
    }

    @Test
    public void testCellFormatter() {
        final PGrid grid = Element.newPGrid(2, 2);
        PWindow.getMain().add(grid);
        grid.getCellFormatter().addStyleName(1, 1, "cell");
        assertEquals(List.of("cell"), writer.valuesOf(ServerToClientModel.CELL_FORMATTER_ADD_STYLE_NAME));
    }
}
