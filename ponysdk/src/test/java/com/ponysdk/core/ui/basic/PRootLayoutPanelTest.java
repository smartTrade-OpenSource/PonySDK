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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PRootLayoutPanelTest extends PSuite {

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
    public void testOnePanelPerWindowAndId() {
        final PWindow main = PWindow.getMain();
        final PRootLayoutPanel root = PRootLayoutPanel.get(main);
        assertEquals(WidgetType.ROOT_LAYOUT_PANEL, root.getWidgetType());
        assertSame(main, root.getWindow());
        assertSame(root, PRootLayoutPanel.get(main));
        assertSame(root, PRootLayoutPanel.get(main, null));

        final PRootLayoutPanel zone = PRootLayoutPanel.get(main, "zone");
        assertNotSame(root, zone);
        assertSame(zone, PRootLayoutPanel.get(main, "zone"));

        final PWindow other = Element.newPWindow(null, null);
        assertNotSame(root, PRootLayoutPanel.get(other));
    }

    @Test
    public void testAddChild() {
        final PRootLayoutPanel root = PRootLayoutPanel.get(PWindow.getMain(), "children");
        final PLabel label = Element.newPLabel("l");
        root.add(label);
        assertSame(root, label.getParent());
        assertEquals(1, root.getWidgetCount());
    }

    @Test
    public void testPanelAndChildrenAreCreated() {
        final PRootLayoutPanel root = PRootLayoutPanel.get(PWindow.getMain(), "created");
        final PLabel label = Element.newPLabel("l");
        root.add(label);
        assertTrue(root.isInitialized());
        assertTrue(label.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(root.getID()));
    }
}
