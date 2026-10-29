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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PContextMenuEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickEvent;
import com.ponysdk.core.ui.basic.event.PDoubleClickHandler;
import com.ponysdk.core.ui.basic.event.PDragEndEvent;
import com.ponysdk.core.ui.basic.event.PDragEndHandler;
import com.ponysdk.core.ui.basic.event.PDragEnterEvent;
import com.ponysdk.core.ui.basic.event.PDragEnterHandler;
import com.ponysdk.core.ui.basic.event.PDragLeaveEvent;
import com.ponysdk.core.ui.basic.event.PDragOverEvent;
import com.ponysdk.core.ui.basic.event.PDragStartEvent;
import com.ponysdk.core.ui.basic.event.PDragStartHandler;
import com.ponysdk.core.ui.basic.event.PDropEvent;
import com.ponysdk.core.ui.basic.event.PDropHandler;
import com.ponysdk.core.ui.basic.event.PFocusEvent;
import com.ponysdk.core.ui.basic.event.PFocusHandler;
import com.ponysdk.core.ui.basic.event.PKeyDownEvent;
import com.ponysdk.core.ui.basic.event.PKeyPressEvent;
import com.ponysdk.core.ui.basic.event.PKeyPressHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.basic.event.PMouseDownEvent;
import com.ponysdk.core.ui.basic.event.PMouseDownHandler;
import com.ponysdk.core.ui.basic.event.PMouseEvent;
import com.ponysdk.core.ui.basic.event.PMouseOutEvent;
import com.ponysdk.core.ui.basic.event.PMouseOutHandler;
import com.ponysdk.core.ui.basic.event.PMouseOverEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpEvent;
import com.ponysdk.core.ui.basic.event.PMouseUpHandler;
import com.ponysdk.core.ui.basic.event.PMouseWhellEvent;
import com.ponysdk.core.ui.basic.event.PVisibilityEvent;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.HandlerRegistration;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.ApplicationCode;
import com.ponysdk.testutil.CapturingModelWriter;
import static org.junit.jupiter.api.Assertions.*;

public class PWidgetTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PLabel attachedLabel() {
        final PLabel label = Element.newPLabel();
        PWindow.getMain().add(label);
        return label;
    }

    private static JsonObject domEvent(final DomHandlerType type) {
        return Json.createObjectBuilder().add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue()).build();
    }

    private static JsonObject keyEvent(final DomHandlerType type, final int key) {
        return Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue())
            .add(ClientToServerModel.VALUE_KEY.toStringValue(), key)
            .build();
    }

    private static JsonArray position() {
        return Json.createArrayBuilder().add(10).add(20).add(30).add(40).build();
    }

    private static JsonObject mouseEvent(final DomHandlerType type) {
        final JsonArray info = Json.createArrayBuilder().add(1).add(2).add(3).add(4).add(2).add(true).add(false).add(true).add(false)
            .build();
        return Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue())
            .add(ClientToServerModel.EVENT_INFO.toStringValue(), info)
            .add(ClientToServerModel.WIDGET_POSITION.toStringValue(), position())
            .build();
    }

    private Object lastValueOf(final ServerToClientModel model) {
        final List<Object> values = writer.valuesOf(model);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    @Test
    public void stateSetBeforeAttachIsWrittenInTheCreate() {
        final PLabel label = Element.newPLabel();
        label.setTitle("title");
        label.setWidth("10px");
        label.setHeight("20px");
        label.setStyleName("main");
        label.addStyleName("s1");
        assertTrue(writer.isEmpty());
        assertFalse(label.isInitialized());

        PWindow.getMain().add(label);

        assertTrue(label.isInitialized());
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_CREATE));
        assertEquals(0, writer.count(ServerToClientModel.TYPE_UPDATE));
        assertEquals(List.of("title"), writer.valuesOf(ServerToClientModel.WIDGET_TITLE));
        assertEquals(List.of("10px"), writer.valuesOf(ServerToClientModel.WIDGET_WIDTH));
        assertEquals(List.of("20px"), writer.valuesOf(ServerToClientModel.WIDGET_HEIGHT));
        assertEquals(List.of("main"), writer.valuesOf(ServerToClientModel.STYLE_NAME));
        assertEquals(List.of("s1"), writer.valuesOf(ServerToClientModel.ADD_STYLE_NAME));
        assertSame(PWindow.getMain().getPRootPanel(), label.getParent());
    }

    @Test
    public void sizeAndTitleUpdatesAreSentOnce() {
        final PLabel label = attachedLabel();
        writer.clear();

        label.setTitle("t");
        label.setTitle("t");
        label.setWidth("1px");
        label.setWidth("1px");
        label.setHeight("2px");
        label.setHeight("2px");
        assertEquals(List.of("t"), writer.valuesOf(ServerToClientModel.WIDGET_TITLE));
        assertEquals(List.of("1px"), writer.valuesOf(ServerToClientModel.WIDGET_WIDTH));
        assertEquals(List.of("2px"), writer.valuesOf(ServerToClientModel.WIDGET_HEIGHT));
        assertEquals("t", label.getTitle());
        assertEquals("1px", label.getWidth());
        assertEquals("2px", label.getHeight());

        label.setSizeFull();
        assertEquals("100%", label.getWidth());
        assertEquals("100%", label.getHeight());
        assertEquals(1, writer.count(ServerToClientModel.WIDGET_FULL_SIZE));
    }

    @Test
    public void visibilityIsSentOnlyWhenChanged() {
        final PLabel label = attachedLabel();
        writer.clear();
        assertTrue(label.isVisible());
        label.setVisible(true);
        assertEquals(0, writer.count(ServerToClientModel.WIDGET_VISIBLE));
        label.setVisible(false);
        assertFalse(label.isVisible());
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.WIDGET_VISIBLE));
    }

    @Test
    public void styleNamesAreAddedAndRemovedOnce() {
        final PLabel label = attachedLabel();
        writer.clear();

        label.addStyleName("a");
        label.addStyleName("a");
        label.addStyleName("");
        label.addStyleName(null);
        label.addStyleName("b");
        assertEquals(List.of("a", "b"), writer.valuesOf(ServerToClientModel.ADD_STYLE_NAME));
        assertTrue(label.hasStyleName("a"));
        assertFalse(label.hasStyleName(""));
        assertEquals(Set.of("a", "b"), label.getStyleNames().collect(Collectors.toSet()));

        label.removeStyleName("a");
        label.removeStyleName("a");
        label.removeStyleName("");
        label.removeStyleName(null);
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.REMOVE_STYLE_NAME));
        assertFalse(label.hasStyleName("a"));
    }

    @Test
    public void setStyleNameReplacesAllStyleNames() {
        final PLabel label = attachedLabel();
        label.addStyleName("a");
        label.addStyleName("b");
        writer.clear();

        label.setStyleName("main");
        assertEquals("main", label.getStyleName());
        assertEquals(Set.of("main"), label.getStyleNames().collect(Collectors.toSet()));
        assertEquals(List.of("main"), writer.valuesOf(ServerToClientModel.STYLE_NAME));

        label.setStyleName("main");
        assertEquals(1, writer.count(ServerToClientModel.STYLE_NAME));

        label.setStyleName(null);
        assertEquals("", label.getStyleName());
        assertEquals(0, label.getStyleNames().count());
        assertEquals(List.of("main", ""), writer.valuesOf(ServerToClientModel.STYLE_NAME));

        label.setStyleName("");
        assertEquals(2, writer.count(ServerToClientModel.STYLE_NAME), "already no style name");
    }

    @Test
    public void setStyleNameAgainDropsTheNamesAddedSince() {
        final PLabel label = attachedLabel();
        label.setStyleName("main");
        label.addStyleName("extra");
        writer.clear();
        label.setStyleName("main");
        assertEquals(List.of("main"), writer.valuesOf(ServerToClientModel.STYLE_NAME));
        assertEquals(Set.of("main"), label.getStyleNames().collect(Collectors.toSet()));

        label.setStyleName("");
        label.addStyleName("extra");
        label.setStyleName("");
        assertEquals(0, label.getStyleNames().count());
        assertEquals(List.of("main", "", ""), writer.valuesOf(ServerToClientModel.STYLE_NAME));
    }

    @Test
    public void styleProperties() {
        final PLabel label = attachedLabel();
        writer.clear();
        label.setStyleProperty("color", "red");
        label.setStyleProperty("color", "red");
        label.setStyleProperty("unset", null);
        assertEquals(List.of("red"), writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "color"));
        assertEquals(1, writer.count(ServerToClientModel.PUT_STYLE_KEY));

        label.setStyleProperty("color", "blue");
        assertEquals(List.of("red", "blue"), writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "color"));

        label.removeStyleProperty("color");
        label.removeStyleProperty("color");
        label.removeStyleProperty("unset");
        assertEquals(List.of("color"), writer.valuesOf(ServerToClientModel.REMOVE_STYLE_KEY));
    }

    @Test
    public void propertiesAndDomId() {
        final PLabel label = attachedLabel();
        writer.clear();

        label.setProperty("p", "v");
        label.setProperty("p", "v");
        label.setProperty("n", null);
        assertEquals(List.of("v"), writer.valuesAfter(ServerToClientModel.PUT_PROPERTY_KEY, "p"));
        assertEquals(1, writer.count(ServerToClientModel.PUT_PROPERTY_KEY));
        label.setProperty("p", "w");
        assertEquals("w", label.getProperty("p"));
        assertNull(label.getProperty("n"));

        label.forceDomId();
        label.forceDomId();
        assertEquals(1, writer.count(ServerToClientModel.FORCE_DOM_ID));
        assertEquals(String.valueOf(label.getID()), label.getProperty("id"));
    }

    @Test
    public void attributes() {
        final PLabel label = attachedLabel();
        writer.clear();
        assertFalse(label.hasAttribute("a"));
        assertNull(label.getAttribute("a"));
        assertTrue(label.getAttributes().isEmpty());

        label.setAttribute(null, "x");
        label.setAttribute("n", null);
        assertTrue(writer.isEmpty());

        label.setAttribute("a", "1");
        label.setAttribute("a", "1");
        label.setAttribute("flag");
        assertEquals(List.of("1"), writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "a"));
        assertEquals(List.of(""), writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "flag"));
        assertTrue(label.hasAttribute("flag"));
        assertEquals("1", label.getAttribute("a"));
        assertTrue(label.getAttributes().contains(Map.entry("a", "1")));

        label.setAttribute("a", "2");
        assertEquals(List.of("1", "2"), writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "a"));

        label.removeAttribute("a");
        label.removeAttribute("a");
        label.removeAttribute("n");
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.REMOVE_ATTRIBUTE_KEY));
        assertFalse(label.hasAttribute("a"));
    }

    @Test
    public void attributesSetBeforeAttachAreNotSentAsUpdates() {
        final PLabel label = Element.newPLabel();
        label.setAttribute("a", "1");
        label.setProperty("p", "v");
        label.setStyleProperty("color", "red");
        label.removeAttribute("a");
        assertTrue(writer.isEmpty());
        PWindow.getMain().add(label);
        assertEquals(0, writer.count(ServerToClientModel.PUT_ATTRIBUTE_KEY));
        assertEquals(List.of("v"), writer.valuesAfter(ServerToClientModel.PUT_PROPERTY_KEY, "p"));
        assertEquals(List.of("red"), writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "color"));
    }

    @Test
    public void preventAndStopEventsAreSentOnce() {
        final PLabel label = attachedLabel();
        writer.clear();
        label.preventEvent(PEventType.ONCLICK);
        label.preventEvent(PEventType.ONCLICK);
        label.stopEvent(PEventType.ONKEYUP);
        label.stopEvent(PEventType.ONKEYUP);
        assertEquals(List.of(PEventType.ONCLICK.getCode()), writer.valuesOf(ServerToClientModel.PREVENT_EVENT));
        assertEquals(List.of(PEventType.ONKEYUP.getCode()), writer.valuesOf(ServerToClientModel.STOP_EVENT));
    }

    @Test
    public void focusBlurAndTabindex() {
        final PLabel label = attachedLabel();
        writer.clear();
        label.focus();
        label.blur();
        assertEquals(List.of(true, false), writer.valuesOf(ServerToClientModel.FOCUS));
        label.focusPreventScroll();
        assertEquals(1, writer.count(ServerToClientModel.FOCUS_PREVENT_SCROLL));

        label.setTabindex(PWidget.TabindexMode.TABULABLE);
        label.setTabindex(0);
        assertEquals(0, label.getTabindex());
        label.setTabindex(PWidget.TabindexMode.FOCUSABLE);
        assertEquals(-1, label.getTabindex());
        assertEquals(List.of(0, -1), writer.valuesOf(ServerToClientModel.TABINDEX));
    }

    @Test
    public void ensureDebugIdOnlyInDebugMode() {
        final PLabel notDebug = Element.newPLabel();
        notDebug.ensureDebugId("ignored");
        assertNull(notDebug.getDebugID());

        Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(true);
        try {
            final PLabel stacked = Element.newPLabel();
            stacked.ensureDebugId("before");
            PWindow.getMain().add(stacked);
            assertEquals(List.of("before"), writer.valuesOf(ServerToClientModel.ENSURE_DEBUG_ID));

            final PLabel label = attachedLabel();
            writer.clear();
            label.ensureDebugId("dbg");
            label.ensureDebugId("dbg");
            assertEquals("dbg", label.getDebugID());
            assertEquals(List.of("dbg"), writer.valuesOf(ServerToClientModel.ENSURE_DEBUG_ID));
        } finally {
            Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(false);
        }
    }

    @Test
    public void inDebugModeAWidgetTellsItsClassAndWhereTheApplicationCreatedIt() {
        PWindow.getMain().add(ApplicationCode.newLabel());
        assertEquals(0, writer.count(ServerToClientModel.DEBUG_INFO), "not in debug mode");

        Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(true);
        try {
            writer.clear();
            PWindow.getMain().add(ApplicationCode.newLabel());
            final List<Object> infos = writer.valuesOf(ServerToClientModel.DEBUG_INFO);
            assertEquals(1, infos.size());
            assertTrue(((String) infos.get(0)).matches("PLabel ApplicationCode\\.java:\\d+"), String.valueOf(infos));
        } finally {
            Mockito.when(UIContext.get().getConfiguration().isDebugMode()).thenReturn(false);
        }
    }

    @Test
    public void domHandlerIsRegisteredOncePerType() {
        final PLabel label = attachedLabel();
        writer.clear();
        final List<PClickEvent> clicks = new ArrayList<>();
        final PClickHandler h1 = clicks::add;
        final PClickHandler h2 = clicks::add;
        label.addDomHandler(h1, PClickEvent.TYPE);
        label.addDomHandler(h2, PClickEvent.TYPE);
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_DOM_CLICK.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));

        label.onClientData(domEvent(DomHandlerType.CLICK));
        assertEquals(2, clicks.size());

        label.removeDomHandler(h1, PClickEvent.TYPE);
        label.onClientData(domEvent(DomHandlerType.CLICK));
        assertEquals(3, clicks.size());

        // unknown handler: only logged
        label.removeDomHandler(h1, PClickEvent.TYPE);
        Element.newPLabel().removeDomHandler(h1, PClickEvent.TYPE);
        assertEquals(3, clicks.size());
    }

    @Test
    public void domHandlerAddedBeforeAttachIsSentOnInit() {
        final PLabel label = Element.newPLabel();
        label.addDomHandler((PClickHandler) e -> {
        }, PClickEvent.TYPE);
        assertEquals(0, writer.count(ServerToClientModel.TYPE_ADD_HANDLER));
        PWindow.getMain().add(label);
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
    }

    @Test
    public void keyHandlersReceiveTheKeyCode() {
        final PLabel label = attachedLabel();
        writer.clear();
        final List<Integer> keys = new ArrayList<>();

        label.addKeyPressHandler(e -> keys.add(e.getKeyCode()));
        label.addKeyDownHandler(e -> keys.add(-e.getKeyCode()));
        assertEquals(0, writer.count(ServerToClientModel.KEY_FILTER));
        label.addKeyUpHandler(PKeyUpHandler.newEnterKeyUp(e -> keys.add(1000 + e.getKeyCode())));
        assertArrayEquals(new Integer[] { PKeyCodes.ENTER.getCode() }, (Integer[]) lastValueOf(ServerToClientModel.KEY_FILTER));

        label.onClientData(keyEvent(DomHandlerType.KEY_PRESS, 65));
        label.onClientData(keyEvent(DomHandlerType.KEY_DOWN, 66));
        label.onClientData(keyEvent(DomHandlerType.KEY_UP, 13));
        assertEquals(List.of(65, -66, 1013), keys);
    }

    @Test
    public void keyHandlersSendTheirKeyFilter() {
        final PLabel label = attachedLabel();
        writer.clear();
        label.addKeyPressHandler(new PKeyPressHandler() {

            @Override
            public void onKeyPress(final PKeyPressEvent event) {
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] { PKeyCodes.ESCAPE };
            }
        });
        assertArrayEquals(new Integer[] { PKeyCodes.ESCAPE.getCode() }, (Integer[]) lastValueOf(ServerToClientModel.KEY_FILTER));

        label.addKeyDownHandler(new PKeyDownEvent.Handler() {

            @Override
            public void onKeyDown(final PKeyDownEvent event) {
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] { PKeyCodes.TAB };
            }
        });
        assertArrayEquals(new Integer[] { PKeyCodes.TAB.getCode() }, (Integer[]) lastValueOf(ServerToClientModel.KEY_FILTER));

        label.addKeyUpHandler(e -> {
        });
        assertEquals(3, writer.count(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(2, writer.count(ServerToClientModel.KEY_FILTER));
    }

    @Test
    public void mouseEventsCarryTheClientInfo() {
        final PLabel label = attachedLabel();
        final List<Event<?>> events = new ArrayList<>();
        label.addDomHandler((PClickHandler) events::add, PClickEvent.TYPE);
        label.addDomHandler((PDoubleClickHandler) events::add, PDoubleClickEvent.TYPE);
        label.addDomHandler((PMouseOverEvent.Handler) events::add, PMouseOverEvent.TYPE);
        label.addDomHandler((PMouseOutHandler) events::add, PMouseOutEvent.TYPE);
        label.addDomHandler((PMouseDownHandler) events::add, PMouseDownEvent.TYPE);
        label.addDomHandler((PMouseUpHandler) events::add, PMouseUpEvent.TYPE);

        label.onClientData(mouseEvent(DomHandlerType.CLICK));
        label.onClientData(mouseEvent(DomHandlerType.DOUBLE_CLICK));
        label.onClientData(mouseEvent(DomHandlerType.MOUSE_OVER));
        label.onClientData(mouseEvent(DomHandlerType.MOUSE_OUT));
        label.onClientData(mouseEvent(DomHandlerType.MOUSE_DOWN));
        label.onClientData(mouseEvent(DomHandlerType.MOUSE_UP));

        assertEquals(List.of(PClickEvent.class, PDoubleClickEvent.class, PMouseOverEvent.class, PMouseOutEvent.class,
            PMouseDownEvent.class, PMouseUpEvent.class), events.stream().map(Object::getClass).collect(Collectors.toList()));
        for (final Event<?> e : events) {
            final PMouseEvent<?> event = (PMouseEvent<?>) e;
            assertSame(label, event.getSource());
            assertEquals(1, event.getX());
            assertEquals(2, event.getY());
            assertEquals(3, event.getClientX());
            assertEquals(4, event.getClientY());
            assertEquals(PMouseEvent.NativeButton.BUTTON_RIGHT, event.getNativeButton());
            assertTrue(event.isControlKeyDown());
            assertFalse(event.isAltKeyDown());
            assertTrue(event.isShiftKeyDown());
            assertFalse(event.isMetaKeyDown());
            assertEquals(10, event.getSourceAbsoluteLeft());
            assertEquals(20, event.getSourceAbsoluteTop());
            assertEquals(30, event.getSourceOffsetHeight());
            assertEquals(40, event.getSourceOffsetWidth());
        }
    }

    @Test
    public void mouseEventWithoutInfoKeepsDefaults() {
        final PLabel label = attachedLabel();
        final List<PClickEvent> events = new ArrayList<>();
        label.addDomHandler((PClickHandler) events::add, PClickEvent.TYPE);
        label.onClientData(domEvent(DomHandlerType.CLICK));
        assertEquals(1, events.size());
        assertNull(events.get(0).getNativeButton());
        assertEquals(0, events.get(0).getX());
    }

    @Test
    public void mouseWheelEventCarriesTheDelta() {
        final PLabel label = attachedLabel();
        final List<PMouseWhellEvent.Event> events = new ArrayList<>();
        label.addDomHandler((PMouseWhellEvent.Handler) events::add, PMouseWhellEvent.TYPE);

        final JsonArray info = Json.createArrayBuilder().add(5).add(6).add(7).add(8).add(1).add(-120).build();
        label.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.MOUSE_WHELL.getValue())
            .add(ClientToServerModel.EVENT_INFO.toStringValue(), info)
            .add(ClientToServerModel.WIDGET_POSITION.toStringValue(), position())
            .build());
        label.onClientData(domEvent(DomHandlerType.MOUSE_WHELL));

        assertEquals(2, events.size());
        final PMouseWhellEvent.Event event = events.get(0);
        assertEquals(5, event.getX());
        assertEquals(6, event.getY());
        assertEquals(7, event.getClientX());
        assertEquals(8, event.getClientY());
        assertEquals(PMouseEvent.NativeButton.BUTTON_LEFT, event.getNativeButton());
        assertEquals(-120, event.getDeltaY());
        assertEquals(10, event.getSourceAbsoluteLeft());
        assertEquals(20, event.getSourceAbsoluteTop());
        assertEquals(30, event.getSourceOffsetHeight());
        assertEquals(40, event.getSourceOffsetWidth());
        assertEquals(0, events.get(1).getDeltaY());
    }

    @Test
    public void otherDomEventsAreDispatchedByType() {
        final PLabel label = attachedLabel();
        final List<Event<?>> events = new ArrayList<>();
        label.addDomHandler((PFocusHandler) events::add, PFocusEvent.TYPE);
        label.addDomHandler((PBlurHandler) events::add, PBlurEvent.TYPE);
        label.addDomHandler((PDragStartHandler) events::add, PDragStartEvent.TYPE);
        label.addDomHandler((PDragEndHandler) events::add, PDragEndEvent.TYPE);
        label.addDomHandler((PDragEnterHandler) events::add, PDragEnterEvent.TYPE);
        label.addDomHandler((PDragLeaveEvent.Handler) events::add, PDragLeaveEvent.TYPE);
        label.addDomHandler((PDragOverEvent.Handler) events::add, PDragOverEvent.TYPE);
        label.addDomHandler((PContextMenuEvent.Handler) events::add, PContextMenuEvent.TYPE);

        label.onClientData(domEvent(DomHandlerType.FOCUS));
        label.onClientData(domEvent(DomHandlerType.BLUR));
        label.onClientData(domEvent(DomHandlerType.DRAG_START));
        label.onClientData(domEvent(DomHandlerType.DRAG_END));
        label.onClientData(domEvent(DomHandlerType.DRAG_ENTER));
        label.onClientData(domEvent(DomHandlerType.DRAG_LEAVE));
        label.onClientData(domEvent(DomHandlerType.DRAG_OVER));
        label.onClientData(domEvent(DomHandlerType.CONTEXT_MENU));
        // not dispatched by PWidget, only logged
        label.onClientData(domEvent(DomHandlerType.CHANGE_HANDLER));

        assertEquals(List.of(PFocusEvent.class, PBlurEvent.class, PDragStartEvent.class, PDragEndEvent.class, PDragEnterEvent.class,
            PDragLeaveEvent.class, PDragOverEvent.class, PContextMenuEvent.class),
            events.stream().map(Object::getClass).collect(Collectors.toList()));
    }

    @Test
    public void dropEventResolvesTheDragSource() {
        final PLabel target = attachedLabel();
        final PLabel source = attachedLabel();
        final List<PDropEvent> drops = new ArrayList<>();
        target.addDomHandler((PDropHandler) drops::add, PDropEvent.TYPE);

        target.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.DROP.getValue())
            .add(ClientToServerModel.DRAG_SRC.toStringValue(), source.getID())
            .build());
        target.onClientData(domEvent(DomHandlerType.DROP));

        assertEquals(2, drops.size());
        assertSame(source, drops.get(0).getDragSource());
        assertNull(drops.get(1).getDragSource());
    }

    @Test
    public void hiddenWidgetIgnoresClientData() {
        final PLabel label = attachedLabel();
        final AtomicInteger clicks = new AtomicInteger();
        label.addDomHandler((PClickHandler) e -> clicks.incrementAndGet(), PClickEvent.TYPE);
        label.setVisible(false);
        label.onClientData(domEvent(DomHandlerType.CLICK));
        assertEquals(0, clicks.get());
    }

    @Test
    public void fireEventWithoutHandlerDoesNothing() {
        final PLabel label = Element.newPLabel();
        label.fireEvent(new PClickEvent(label));
        label.fireKeyEvent(new PKeyUpEvent(label, 1));
        assertTrue(writer.isEmpty());
    }

    @Test
    public void visibilityHandlers() {
        final PLabel label = attachedLabel();
        writer.clear();
        final List<Boolean> received = new ArrayList<>();
        final PVisibilityEvent.PVisibilityHandler handler = e -> received.add(e.getData());
        label.addVisibilityHandler(handler);
        label.addVisibilityHandler(null);
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_VISIBILITY.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));

        assertFalse(label.isShown());
        label.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(label.isShown());
        label.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), false).build());
        assertFalse(label.isShown());
        assertEquals(List.of(true, false), received);

        label.removeVisibilityHandler(handler);
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
        label.removeVisibilityHandler(null);
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));

        final PLabel noHandler = attachedLabel();
        noHandler.removeVisibilityHandler(handler);
        noHandler.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), false).build());
        assertFalse(noHandler.isShown());
        noHandler.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(noHandler.isShown());
    }

    @Test
    public void terminalDataFallsBackToTheObject() {
        final PLabel label = attachedLabel();
        final List<JsonObject> received = new ArrayList<>();
        label.setTerminalHandler(e -> received.add(e.getData()));
        final JsonObject data = Json.createObjectBuilder().add("k", "v").build();
        label.onClientData(Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), data).build());
        assertEquals(List.of(data), received);
    }

    @Test
    public void removeFromParent() {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PLabel label = Element.newPLabel();
        label.removeFromParent();
        panel.add(label);
        assertSame(panel, label.getParent());
        label.removeFromParent();
        assertEquals(0, panel.getWidgetCount());
        assertNull(label.getParent());
    }

    @Test
    public void removeFromParentThatIsNotAContainerFails() {
        final PLabel label = Element.newPLabel();
        label.setParent(Element.newPLabel());
        assertThrows(IllegalStateException.class, label::removeFromParent);
    }

    @Test
    public void asWidget() {
        final PLabel label = Element.newPLabel();
        assertSame(label, label.asWidget());
        assertSame(label, PWidget.asWidgetOrNull(label));
        assertNull(PWidget.asWidgetOrNull(null));
    }

    @Test
    public void addonsAreDestroyedWithTheWidget() {
        final PLabel label = Element.newPLabel();
        assertNull(label.getAddons());
        final AtomicInteger destroyed = new AtomicInteger();
        final PAddOn addon = new PAddOn() {

            @Override
            public void onDestroy() {
                destroyed.incrementAndGet();
                super.onDestroy();
            }
        };
        final PAddOn failing = new PAddOn() {

            @Override
            public void onDestroy() {
                throw new IllegalStateException("expected");
            }
        };
        label.bindAddon(addon);
        label.bindAddon(failing);
        assertEquals(Set.of(addon, failing), label.getAddons());

        label.onDestroy();
        assertEquals(1, destroyed.get());
    }

    @Test
    public void destroyedWidgetIgnoresHandlersAndUpdates() {
        final PLabel label = attachedLabel();
        label.onDestroy();
        writer.clear();
        assertNull(label.addDomHandler((PClickHandler) e -> {
        }, PClickEvent.TYPE));
        label.removeDomHandler((PClickHandler) e -> {
        }, PClickEvent.TYPE);
        label.setTitle("x");
        label.focus();
        label.setTabindex(3);
        assertTrue(writer.isEmpty());
    }

    @Test
    public void tabindexMode() {
        assertEquals(-1, PWidget.TabindexMode.FOCUSABLE.getTabIndex());
        assertEquals(0, PWidget.TabindexMode.TABULABLE.getTabIndex());
    }

    @Test
    public void handlerRegistrationRemovesTheHandler() {
        final PLabel label = attachedLabel();
        final AtomicInteger clicks = new AtomicInteger();
        final HandlerRegistration registration = label.addDomHandler((PClickHandler) e -> clicks.incrementAndGet(),
            PClickEvent.TYPE);
        registration.removeHandler();
        label.onClientData(domEvent(DomHandlerType.CLICK));
        assertEquals(0, clicks.get());
    }
}
