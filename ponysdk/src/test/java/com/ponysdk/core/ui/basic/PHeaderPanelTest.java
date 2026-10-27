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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PHeaderPanelTest extends PSuite {

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
        final PHeaderPanel panel = Element.newPHeaderPanel();
        assertEquals(WidgetType.HEADER_PANEL, panel.getWidgetType());
        assertNull(panel.getHeaderWidget());
        assertNull(panel.getContentWidget());
        assertNull(panel.getFooterWidget());
        assertFalse(panel.iterator().hasNext());
    }

    @Test
    public void testAddFillsHeaderContentFooterInOrder() {
        final PHeaderPanel panel = Element.newPHeaderPanel();
        final PLabel header = Element.newPLabel("h");
        final PLabel content = Element.newPLabel("c");
        final PLabel footer = Element.newPLabel("f");
        panel.add(header);
        panel.add(content);
        panel.add(footer);
        assertSame(header, panel.getHeaderWidget());
        assertSame(content, panel.getContentWidget());
        assertSame(footer, panel.getFooterWidget());
        assertSame(panel, content.getParent());
        assertThrows(IllegalStateException.class, () -> panel.add(Element.newPLabel("x")));

        final List<PWidget> children = new ArrayList<>();
        panel.forEach(children::add);
        assertEquals(List.of(header, content, footer), children);

        assertEquals("<div pid=\"" + panel.getID() + "\">" + header.dumpDOM() + content.dumpDOM() + footer.dumpDOM() + "</div>",
            panel.dumpDOM());
    }

    @Test
    public void testChildrenAddedBeforeAttachAreCreatedWithThePanel() {
        final PHeaderPanel panel = Element.newPHeaderPanel();
        final PLabel header = Element.newPLabel("h");
        final PLabel content = Element.newPLabel("c");
        final PLabel footer = Element.newPLabel("f");
        panel.setFooterWidget(footer);
        panel.setContentWidget(content);
        panel.setHeaderWidget(header);

        PWindow.getMain().add(panel);
        assertTrue(header.isInitialized());
        assertTrue(content.isInitialized());
        assertTrue(footer.isInitialized());
        assertEquals(List.of(panel.getID()), writer.valuesAfter(ServerToClientModel.TYPE_ADD, header.getID()));
        assertEquals(List.of(panel.getID()), writer.valuesAfter(ServerToClientModel.TYPE_ADD, content.getID()));
        assertEquals(List.of(panel.getID()), writer.valuesAfter(ServerToClientModel.TYPE_ADD, footer.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.INDEX).containsAll(List.of(0, 1, 2)));
    }

    @Test
    public void testChildSetAfterAttachIsCreated() {
        final PHeaderPanel panel = Element.newPHeaderPanel();
        PWindow.getMain().add(panel);
        final PLabel header = Element.newPLabel("h");
        panel.setHeaderWidget(header);
        assertTrue(header.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(header.getID()));
    }

    @Test
    public void testSetMovesWidgetFromPreviousParent() {
        final PFlowPanel previous = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel("l");
        previous.add(label);
        final PHeaderPanel panel = Element.newPHeaderPanel();
        panel.setContentWidget(label);
        assertEquals(0, previous.getWidgetCount());
        assertSame(panel, label.getParent());
    }

    @Test
    public void testRemove() {
        final PHeaderPanel panel = Element.newPHeaderPanel();
        final PLabel header = Element.newPLabel("h");
        final PLabel content = Element.newPLabel("c");
        final PLabel footer = Element.newPLabel("f");
        panel.add(header);
        panel.add(content);
        panel.add(footer);
        PWindow.getMain().add(panel);

        writer.clear();
        assertTrue(panel.remove(content));
        assertNull(panel.getContentWidget());
        assertNull(content.getParent());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(content.getID()));

        assertTrue(panel.remove(header));
        assertNull(panel.getHeaderWidget());
        assertTrue(panel.remove(footer));
        assertNull(panel.getFooterWidget());
        assertFalse(panel.remove(footer));
        assertFalse(panel.iterator().hasNext());
    }

    @Test
    public void testResize() {
        final PHeaderPanel panel = Element.newPHeaderPanel();
        PWindow.getMain().add(panel);
        writer.clear();
        panel.resize();
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_UPDATE).contains(panel.getID()));
        assertEquals(1, writer.count(ServerToClientModel.RESIZE));
    }
}
