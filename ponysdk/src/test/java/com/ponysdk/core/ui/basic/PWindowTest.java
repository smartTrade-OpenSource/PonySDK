/*
 * Copyright (c) 2018 PonySDK
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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.basic.event.POpenHandler;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent;
import com.ponysdk.core.ui.formatter.TextFunction;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class PWindowTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void capture() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restore() {
        writer.uninstall();
    }

    private boolean wrote(final ServerToClientModel model) {
        return writer.count(model) > 0;
    }

    private boolean wrote(final ServerToClientModel model, final Object value) {
        return writer.valuesOf(model).contains(value);
    }

    @Test
    public void testOpen() {
        final PWindow window = Element.newPWindow(null, null);
        assertFalse(window.isOpened());
        window.open();
        assertTrue(window.isOpened());
    }


    @Test
    public void testOpenHandler() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        window.addOpenHandler(e -> handlerFiredCount.incrementAndGet());
        assertFalse(window.isOpened());
        window.open();
        assertTrue(window.isOpened());
        assertEquals(1, handlerFiredCount.get());
    }


    @Test
    public void testRemoveOpenHandler() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        POpenHandler handler = e -> handlerFiredCount.incrementAndGet();
        window.addOpenHandler(handler);
        assertFalse(window.isOpened());
        window.removeOpenHandler(handler);
        window.open();
        assertTrue(window.isOpened());
        assertEquals(0, handlerFiredCount.get());
    }

    @Test
    public void testClose() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFired = new AtomicInteger(0);
        window.addCloseHandler(e -> handlerFired.incrementAndGet());
        window.open();
        assertTrue(window.isOpened());
        window.close();
        assertFalse(window.isOpened());
        assertEquals(1, handlerFired.get());
    }

    @Test
    public void testRemoveCloseHandler() {
        final PWindow window = Element.newPWindow(null, null);
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        PCloseHandler handler = e -> handlerFiredCount.incrementAndGet();
        window.addCloseHandler(handler);
        assertFalse(window.isOpened());
        window.removeCloseHandler(handler);
        window.open();
        assertTrue(window.isOpened());
        window.close();
        assertFalse(window.isOpened());
        assertEquals(0, handlerFiredCount.get());
    }

    @Test
    public void testAttachToMain() {
        final PWindow window = Element.newPWindow(PWindow.getMain(), "test", null);
        window.open();
        assertEquals(window.getWindow(), PWindow.getMain());
    }

    @Test
    public void testSetTitle() {
        final PWindow window = Element.newPWindow(PWindow.getMain(), "test", null);
        window.setTitle("title test");
        window.open();
        //assertTrue("title test",window.getTitle()); todo nciaravola api incoherent
    }

    @Test
    public void testSubWindow() {
        final PWindow window = Element.newPWindow(PWindow.getMain(), "test", null);
        window.open();
        assertTrue(window.isOpened());

        final PWindow subWindow = Element.newPWindow(window, "test", null);
        subWindow.open();
        assertTrue(subWindow.isOpened());

        assertEquals(window, subWindow.getParent());

        window.close();
        assertFalse(window.isOpened());
        assertFalse(subWindow.isOpened());
    }
    
    @Test
    public void testSubWindowRemoved() {
        final PWindow window = Element.newPWindow(PWindow.getMain(), "test", null);
        window.open();
        assertTrue(window.isOpened());

        final PWindow subWindow = Element.newPWindow(window, "test", null);
        subWindow.open();
        assertFalse(window.getSubWindows().isEmpty());
        assertEquals(subWindow, window.getSubWindows().iterator().next());

        subWindow.close();
        assertTrue(window.getSubWindows().isEmpty());
    }

    @Test
    public void testOpenWritesCreationAndRegistersWindow() {
        final PWindow window = Element.newPWindow("http://url", "name", "features");
        assertEquals("http://url", window.getUrl());
        assertEquals("name", window.getName());
        assertTrue(window.getSubWindows().isEmpty());
        assertNull(window.getParent());

        window.open();

        assertTrue(wrote(ServerToClientModel.TYPE_CREATE, window.getID()));
        assertTrue(wrote(ServerToClientModel.WIDGET_TYPE, WidgetType.WINDOW.getValue()));
        assertTrue(wrote(ServerToClientModel.RELATIVE, false));
        assertTrue(wrote(ServerToClientModel.URL, "http://url"));
        assertTrue(wrote(ServerToClientModel.NAME, "name"));
        assertTrue(wrote(ServerToClientModel.FEATURES, "features"));
        assertTrue(wrote(ServerToClientModel.OPEN));
        // The emulated terminal answers with the url
        assertEquals("url", window.getUrl());
        assertSame(window, PWindowManager.getWindow(window.getID()));
        assertTrue(PWindowManager.getWindows().contains(window));

        writer.clear();
        window.open();
        assertTrue(writer.isEmpty());

        window.close();
        assertNull(PWindowManager.getWindow(window.getID()));
    }

    @Test
    public void testWindowCommands() {
        final PWindow window = Element.newPWindow("w", null);
        window.open();
        writer.clear();

        window.print();
        assertTrue(wrote(ServerToClientModel.PRINT));
        window.resizeBy(1.5f, 2.5f);
        assertTrue(wrote(ServerToClientModel.RESIZE_BY_X, 1.5d));
        assertTrue(wrote(ServerToClientModel.RESIZE_BY_Y, 2.5d));
        window.resizeTo(800, 600);
        assertTrue(wrote(ServerToClientModel.RESIZE_TO_WIDTH, 800));
        assertTrue(wrote(ServerToClientModel.RESIZE_TO_HEIGHT, 600));
        window.moveBy(3f, 4f);
        assertTrue(wrote(ServerToClientModel.MOVE_BY_X, 3d));
        assertTrue(wrote(ServerToClientModel.MOVE_BY_Y, 4d));
        window.moveTo(5f, 6f);
        assertTrue(wrote(ServerToClientModel.MOVE_TO_X, 5d));
        assertTrue(wrote(ServerToClientModel.MOVE_TO_Y, 6d));
        window.focus();
        assertTrue(wrote(ServerToClientModel.FOCUS, true));
        window.reload();
        assertTrue(wrote(ServerToClientModel.RELOAD));
        window.setTitle("title");
        assertTrue(wrote(ServerToClientModel.WINDOW_TITLE, "title"));
        window.getLocation().replace("http://other");
        assertTrue(wrote(ServerToClientModel.WINDOW_LOCATION_REPLACE, "http://other"));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_UPDATE).stream().allMatch(id -> id.equals(window.getID())));
    }

    @Test
    public void testCommandsBeforeOpenAreStacked() {
        final PWindow window = Element.newPWindow("w", null);
        window.setTitle("early");
        assertFalse(wrote(ServerToClientModel.WINDOW_TITLE));
        window.open();
        assertTrue(wrote(ServerToClientModel.WINDOW_TITLE, "early"));
    }

    @Test
    public void testClosedWindowIgnoresCommands() {
        final PWindow window = Element.newPWindow("w", null);
        window.open();
        window.close();
        assertFalse(window.isOpened());
        writer.clear();

        window.open();
        window.close();
        window.print();
        window.add(Element.newPLabel());
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testAddWidgetsByZone() {
        final PWindow window = Element.newPWindow("w", null);
        final PLabel beforeOpen = Element.newPLabel("before");
        window.add(beforeOpen);
        assertFalse(beforeOpen.isInitialized());

        window.open();
        assertTrue(beforeOpen.isInitialized());
        assertSame(window, beforeOpen.getWindow());

        final PLabel inZone = Element.newPLabel("zone");
        window.add("zone", inZone);
        assertTrue(inZone.isInitialized());
        assertSame(window.getPRootPanel("zone"), inZone.getParent());
        assertEquals(1, window.getPRootPanel().getWidgetCount());
        assertTrue(wrote(ServerToClientModel.TEXT, "zone"));

        final String dom = window.dumpDOM();
        assertTrue(dom.startsWith("<body>") && dom.endsWith("</body>"), dom);
        assertTrue(dom.contains("before") && dom.contains("zone"), dom);

        window.clear();
        assertEquals(0, window.getPRootPanel().getWidgetCount());
        assertEquals(0, window.getPRootPanel("zone").getWidgetCount());
    }

    @Test
    public void testVisibilityHandlers() {
        final PWindow window = Element.newPWindow("w", null);
        window.open();
        assertTrue(window.isShown());

        final List<Boolean> received = new ArrayList<>();
        final PVisibilityEvent.PVisibilityHandler handler = event -> received.add(event.getData());
        writer.clear();
        window.addVisibilityHandler(handler);
        // The root panel always reports the document visibility: a window registers no handler with the terminal
        assertTrue(writer.isEmpty());
        window.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_DOCUMENT_VISIBILITY.toStringValue(), false).build());
        assertFalse(window.isShown());
        assertEquals(List.of(false), received);

        writer.clear();
        window.removeVisibilityHandler(handler);
        assertTrue(writer.isEmpty());

        window.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_DOCUMENT_VISIBILITY.toStringValue(), true).build());
        assertTrue(window.isShown());
        assertEquals(1, received.size());
    }

    @Test
    public void testDestroyFromTerminal() {
        final PWindow window = Element.newPWindow("w", null);
        window.open();
        final AtomicInteger destroyed = new AtomicInteger();
        window.addDestroyListener(o -> destroyed.incrementAndGet());
        window.onClientData(Json.createObjectBuilder().addNull(ClientToServerModel.HANDLER_DESTROY.toStringValue()).build());
        assertFalse(window.isOpened());
        assertEquals(1, destroyed.get());
        assertNull(PWindowManager.getWindow(window.getID()));
        assertFalse(PWindowManager.getWindows().contains(window));
    }

    @Test
    public void testTerminalDataGoesToTerminalHandler() {
        final PWindow window = Element.newPWindow("w", null);
        window.open();
        final List<JsonObject> received = new ArrayList<>();
        window.setTerminalHandler(e -> received.add(e.getData()));
        final JsonObject data = Json.createObjectBuilder().add("k", "v").build();
        window.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), data).build());
        assertEquals(List.of(data), received);
    }

    @Test
    public void testPFunctionIsSharedPerTextFunction() {
        final TextFunction function = TextFunction.named(args -> "x", "xFunction");
        final PFunction pFunction = PWindow.getMain().getPFunction(function);
        assertSame(pFunction, PWindow.getMain().getPFunction(function));
        assertSame(function, pFunction.getFunction());
        assertTrue(pFunction.toString().contains("xFunction"));
        assertEquals(WidgetType.FUNCTION, pFunction.getWidgetType());
        assertEquals(List.of("xFunction"), writer.valuesOf(ServerToClientModel.FUNCTION_NAME));
    }

    @Test
    public void testMainWindow() {
        final PWindow main = PWindow.getMain();
        assertSame(main, PWindow.getMain());
        assertTrue(PWindow.isMain(main));
        assertFalse(PWindow.isMain(Element.newPWindow("w", null)));
        assertEquals(WidgetType.BROWSER, main.getWidgetType());
        assertTrue(main.isOpened());
        main.open();
        main.close();
        assertTrue(main.isOpened());
        assertTrue(main.isMainWindow());
        assertTrue(main.toString().contains("name = null"));
    }

    @Test
    public void testParameter() {
        final PWindow.Parameter parameter = new PWindow.Parameter();
        assertEquals(Integer.valueOf(100), parameter.getHeight());
        assertEquals(Integer.valueOf(100), parameter.getWidth());
        parameter.setTitle("t");
        parameter.setFullScreen(1);
        parameter.setHeight(10);
        parameter.setWidth(20);
        parameter.setLeft(1.5);
        parameter.setTop(2.5);
        parameter.setMenuBar(true);
        parameter.setScrollbars(false);
        parameter.setStatus(true);
        parameter.setTitlebar(false);
        parameter.setToolbar(true);
        assertEquals("t", parameter.getTitle());
        assertEquals(Integer.valueOf(1), parameter.getFullScreen());
        assertEquals(Integer.valueOf(10), parameter.getHeight());
        assertEquals(Integer.valueOf(20), parameter.getWidth());
        assertEquals(Double.valueOf(1.5), parameter.getLeft());
        assertEquals(Double.valueOf(2.5), parameter.getTop());
        assertTrue(parameter.getMenuBar());
        assertFalse(parameter.getScrollbars());
        assertTrue(parameter.getStatus());
        assertFalse(parameter.getTitlebar());
        assertTrue(parameter.getToolbar());
    }

    @Test
    public void testWindowManagerCloseAll() {
        final PWindow w1 = Element.newPWindow("w1", null);
        final PWindow w2 = Element.newPWindow("w2", null);
        w1.open();
        w2.open();
        assertSame(PWindowManager.get(), PWindowManager.get());
        assertTrue(PWindowManager.getWindows().containsAll(List.of(w1, w2)));

        // A writer without terminal emulation: the close answer of the terminal is asynchronous in production
        final ModelWriter silent = Mockito.mock(ModelWriter.class);
        Mockito.doReturn(silent).when(UIContext.get()).getWriter();
        try {
            PWindowManager.closeAll();
        } finally {
            Mockito.doReturn(writer).when(UIContext.get()).getWriter();
        }

        Mockito.verify(silent).writeInt(ServerToClientModel.TYPE_UPDATE, w1.getID());
        Mockito.verify(silent).writeInt(ServerToClientModel.TYPE_UPDATE, w2.getID());
        Mockito.verify(silent, Mockito.atLeast(2)).write(ServerToClientModel.CLOSE);
        assertTrue(PWindowManager.getWindows().isEmpty());
        assertNull(PWindowManager.getWindow(w1.getID()));
    }

}
