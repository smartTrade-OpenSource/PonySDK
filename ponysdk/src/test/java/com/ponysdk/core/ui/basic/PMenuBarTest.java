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
import com.ponysdk.core.server.LogCapture;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PMenuBarTest extends PSuite {

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
        final PMenuBar menuBar = Element.newPMenuBar();
        assertEquals(WidgetType.MENU_BAR, menuBar.getWidgetType());
        assertFalse(menuBar.isVertical());
        assertTrue(Element.newPMenuBar(true).isVertical());
        assertFalse(menuBar.isAnimationEnabled());
    }

    @Test
    public void testVerticalWrittenOnCreation() {
        writer.clear();
        PWindow.getMain().add(Element.newPMenuBar(true));
        assertTrue(writer.valuesOf(ServerToClientModel.VERTICAL).contains(true));
    }

    @Test
    public void testAddItems() {
        final PMenuBar menuBar = Element.newPMenuBar();
        final Runnable cmd = () -> {
        };
        final PMenuBar subMenu = Element.newPMenuBar(true);

        final PMenuItem text = menuBar.addItem("text");
        final PMenuItem html = menuBar.addItem("<b>html</b>", true, cmd);
        final PMenuItem htmlSub = menuBar.addItem("<i>sub</i>", true, Element.newPMenuBar());
        final PMenuItem withCmd = menuBar.addItem("cmd", cmd);
        final PMenuItem withSub = menuBar.addItem("sub", subMenu);

        assertSame(text, menuBar.getItem(0));
        assertEquals("text", text.getText());
        assertEquals("<b>html</b>", html.getHTML());
        assertNull(html.getText());
        assertSame(cmd, html.getCmd());
        assertNotNull(htmlSub.getSubMenu());
        assertSame(cmd, withCmd.getCmd());
        assertSame(subMenu, withSub.getSubMenu());

        final List<PMenuSubElement> iterated = new ArrayList<>();
        menuBar.forEach(iterated::add);
        assertEquals(List.of(text, html, htmlSub, withCmd, withSub), iterated);
    }

    @Test
    public void testItemsAttachedWithMenuBar() {
        final PMenuBar menuBar = Element.newPMenuBar();
        final PMenuItem item = menuBar.addItem("item");
        assertFalse(item.isInitialized());

        writer.clear();
        PWindow.getMain().add(menuBar);
        assertTrue(item.isInitialized());
        assertSame(PWindow.getMain(), item.getWindow());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(item.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID).contains(menuBar.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TEXT).contains("item"));
    }

    @Test
    public void testAddOnAttachedMenuBar() {
        final PMenuBar menuBar = Element.newPMenuBar();
        PWindow.getMain().add(menuBar);
        writer.clear();
        final PMenuItem item = menuBar.addItem(Element.newPMenuItem("late"));
        assertTrue(item.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(item.getID()));
    }

    @Test
    public void testInsertElement() {
        final PMenuBar menuBar = Element.newPMenuBar();
        PWindow.getMain().add(menuBar);
        final PMenuItem first = menuBar.addItem("first");
        writer.clear();
        final PMenuItem inserted = menuBar.insertElement(Element.newPMenuItem("inserted"), 0);
        assertSame(inserted, menuBar.getItem(0));
        assertSame(first, menuBar.getItem(1));
        assertTrue(writer.valuesOf(ServerToClientModel.BEFORE_INDEX).contains(0));
    }

    @Test
    public void testInsertElementOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            Element.newPMenuBar().insertElement(Element.newPMenuItem("x"), 3);
    
        });
    }

    @Test
    public void testRemoveItem() {
        final PMenuBar menuBar = Element.newPMenuBar();
        PWindow.getMain().add(menuBar);
        final PMenuItem a = menuBar.addItem("a");
        final PMenuItem b = menuBar.addItem("b");
        writer.clear();

        assertTrue(menuBar.removeItem(0));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(a.getID()));
        assertSame(b, menuBar.getItem(0));

        assertTrue(menuBar.removeItem(b));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(b.getID()));
        assertFalse(menuBar.removeItem(b));
        assertFalse(menuBar.iterator().hasNext());
    }

    @Test
    public void testSeparatorAndClear() {
        final PMenuBar menuBar = Element.newPMenuBar();
        PWindow.getMain().add(menuBar);
        menuBar.addItem("a");
        menuBar.addSeparator();
        assertTrue(menuBar.getItem(1) instanceof PMenuItemSeparator);

        writer.clear();
        menuBar.clearItems();
        assertFalse(menuBar.iterator().hasNext());
        assertTrue((writer.count(ServerToClientModel.CLEAR) > 0));
    }

    @Test
    public void testAnimationEnabled() {
        final PMenuBar menuBar = Element.newPMenuBar();
        PWindow.getMain().add(menuBar);
        writer.clear();
        menuBar.setAnimationEnabled(true);
        menuBar.setAnimationEnabled(true);
        assertTrue(menuBar.isAnimationEnabled());
        assertEquals(1, writer.count(ServerToClientModel.ANIMATION));
    }

    @Test
    public void testDestroyPropagatesToItems() {
        final PMenuBar menuBar = Element.newPMenuBar();
        PWindow.getMain().add(menuBar);
        final PMenuItem item = menuBar.addItem("a");
        final boolean[] destroyed = new boolean[1];
        item.addDestroyListener(o -> destroyed[0] = true);
        menuBar.onDestroy();
        assertTrue(destroyed[0]);
        assertNull(item.getWindow());
    }

    @Test
    public void anItemFailingToBeDestroyedIsLoggedAndTheNextIsDestroyed() {
        final PMenuBar menuBar = Element.newPMenuBar();
        PWindow.getMain().add(menuBar);
        menuBar.addItem(new PMenuItem("failing") {

            @Override
            public void onDestroy() {
                throw new IllegalStateException("failing item");
            }
        });
        final PMenuItem next = menuBar.addItem("next");

        try (LogCapture log = LogCapture.of(PMenuBar.class)) {
            menuBar.onDestroy();
            assertEquals(List.of("ERROR An error occurred while trying to process the destroy event"), log.events());
        }
        assertNull(next.getWindow());
    }

    @Test
    public void testDumpDOM() {
        final PMenuBar menuBar = Element.newPMenuBar();
        final PMenuItem item = menuBar.addItem("a");
        menuBar.addSeparator();
        final PMenuItemSeparator separator = (PMenuItemSeparator) menuBar.getItem(1);
        assertEquals("<ul pid=\"" + menuBar.getID() + "\"><li pid=\"" + item.getID() + "\">a</li><li pid=\"" + separator.getID()
                + "\" separator></li></ul>",
            menuBar.dumpDOM());
    }

}
