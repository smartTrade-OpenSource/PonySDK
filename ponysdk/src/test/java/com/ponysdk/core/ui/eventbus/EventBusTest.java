/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.ui.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.test.PSuite;

import org.junit.jupiter.api.Test;

public class EventBusTest extends PSuite {

    private static final Event.Type CLICK_EVENT_TYPE = new Event.Type();
    private static final Event.Type HOVER_EVENT_TYPE = new Event.Type();

    @Test
    public void testGlobalHandler() {
        final EventBus eventBus = new EventBus();
        final TestEventHandler clickEventHandler = new TestEventHandler();
        eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler);
        final Event<TestEventHandler> hoverEvent = new HoverTestEvent(null);
        eventBus.fireEvent(hoverEvent);
        assertFalse(clickEventHandler.invoked, "Click handler should NOT have been invoked");
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(null);
        eventBus.fireEvent(clickEvent);
        assertTrue(clickEventHandler.invoked, "Click handler should have been invoked");
        eventBus.removeHandler(CLICK_EVENT_TYPE, clickEventHandler);
        clickEventHandler.invoked = false;
        eventBus.fireEvent(clickEvent);
        assertFalse(clickEventHandler.invoked, "Click handler should NOT have been invoked");
    }

    @Test
    public void testSourceHandler() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new EventSource();
        final TestEventHandler clickEventHandler = new TestEventHandler();
        eventSource.addHandler(CLICK_EVENT_TYPE, clickEventHandler);
        final Event<TestEventHandler> hoverEvent = new HoverTestEvent(eventSource);
        eventBus.fireEventFromSource(hoverEvent, eventSource);
        assertFalse(clickEventHandler.invoked, "Click handler should NOT have been invoked");
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(eventSource);
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue(clickEventHandler.invoked, "Click handler should have been invoked");
        eventSource.removeHandler(CLICK_EVENT_TYPE, clickEventHandler);
        clickEventHandler.invoked = false;
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertFalse(clickEventHandler.invoked, "Click handler should NOT have been invoked");
    }

    @Test
    public void testBroadcastHandler() {
        final EventBus eventBus = new EventBus();
        final TestBroadcastHandler broadcastHandler = new TestBroadcastHandler();
        eventBus.addHandler(broadcastHandler);
        assertFalse(broadcastHandler.invoked, "Broadcast handler should NOT have been invoked");
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(null);
        eventBus.fireEvent(clickEvent);
        assertTrue(broadcastHandler.invoked, "Broadcast handler should have been invoked");
        eventBus.removeHandler(broadcastHandler);
        broadcastHandler.invoked = false;
        eventBus.fireEvent(clickEvent);
        assertFalse(broadcastHandler.invoked, "Broadcast handler should NOT have been invoked");
    }

    @Test
    public void testSingleHandlerPerEventType() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new EventSource();
        final TestEventHandler clickEventHandler = new TestEventHandler();
        final TestEventHandler hoverEventHandler = new TestEventHandler();
        eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler);
        eventBus.addHandler(HOVER_EVENT_TYPE, hoverEventHandler);
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(eventSource);
        assertFalse(clickEventHandler.invoked, "Click handler should NOT have been invoked");
        assertFalse(hoverEventHandler.invoked, "Hover handler should NOT have been invoked");
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue(clickEventHandler.invoked, "Click handler should have been invoked");
        assertFalse(hoverEventHandler.invoked, "Hover handler should NOT have been invoked");
    }

    @Test
    public void testMultipleHandlersOfSingleEventType() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new EventSource();
        final TestEventHandler clickEventHandler = new TestEventHandler();
        final TestEventHandler clickEventHandler2 = new TestEventHandler();
        eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler);
        eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler2);
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(eventSource);
        assertFalse(clickEventHandler.invoked, "Click handler 1 should NOT have been invoked");
        assertFalse(clickEventHandler2.invoked, "Click handler 2 should NOT have been invoked");
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue(clickEventHandler.invoked, "Click handler 1 should have been invoked");
        assertTrue(clickEventHandler2.invoked, "Click handler 2 should have been invoked");
    }

    @Test
    public void testAddHandlerFromAnotherHandler() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new EventSource();
        final TestEventHandler clickEventHandler2 = new TestEventHandler();
        final TestEventHandler clickEventHandler = new TestEventHandler() {

            @Override
            void invoke() {
                super.invoke();
                eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler2);
            }
        };
        eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler);
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(eventSource);
        assertFalse(clickEventHandler.invoked, "Click handler 1 should NOT have been invoked");
        assertFalse(clickEventHandler2.invoked, "Click handler 2 should NOT have been invoked");
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue(clickEventHandler.invoked, "Click handler 1 should have been invoked");
        assertFalse(clickEventHandler2.invoked, "Click handler 2 should NOT have been invoked");
    }

    @Test
    public void testRemoveHandlerFromAnotherHandler() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new EventSource();
        final TestEventHandler clickEventHandler2 = new TestEventHandler();
        final TestEventHandler clickEventHandler = new TestEventHandler() {

            @Override
            void invoke() {
                super.invoke();
                eventBus.removeHandler(CLICK_EVENT_TYPE, clickEventHandler2);
            }
        };
        eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler);
        eventBus.addHandler(CLICK_EVENT_TYPE, clickEventHandler2);
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(eventSource);
        assertFalse(clickEventHandler.invoked, "Click handler 1 should NOT have been invoked");
        assertFalse(clickEventHandler2.invoked, "Click handler 2 should NOT have been invoked");
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue(clickEventHandler.invoked, "Click handler 1 should have been invoked");
        assertTrue(clickEventHandler2.invoked, "Click handler 2 should have been invoked");
    }

    @Test
    public void testFailingHandlersAreCollected() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new TinyEventSource();
        final TestEventHandler sourceHandler = new TestEventHandler() {

            @Override
            void invoke() {
                throw new IllegalStateException("source");
            }
        };
        final TestEventHandler globalHandler = new TestEventHandler() {

            @Override
            void invoke() {
                throw new IllegalArgumentException("global");
            }
        };
        final TestEventHandler survivor = new TestEventHandler();
        eventSource.addHandler(CLICK_EVENT_TYPE, sourceHandler);
        eventBus.addHandler(CLICK_EVENT_TYPE, globalHandler);
        eventBus.addHandler(CLICK_EVENT_TYPE, survivor);
        eventBus.addHandler(event -> {
            throw new UnsupportedOperationException("broadcast");
        });

        final UmbrellaException exception = assertThrows(UmbrellaException.class,
            () -> eventBus.fireEventFromSource(new ClickTestEvent(eventSource), eventSource));
        // Every handler ran despite the failures
        assertTrue(survivor.invoked);
        assertEquals(3, exception.getCauses().size());
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("3 exceptions caught: source; global; broadcast", exception.getMessage());
    }

    @Test
    public void testOnlyAFailingBroadcastHandlerIsReported() {
        final EventBus eventBus = new EventBus();
        final TestBroadcastHandler survivor = new TestBroadcastHandler();
        eventBus.addHandler(event -> {
            throw new UnsupportedOperationException("broadcast");
        });
        eventBus.addHandler(survivor);
        final UmbrellaException exception = assertThrows(UmbrellaException.class, () -> eventBus.fireEvent(new ClickTestEvent(null)));
        assertTrue(survivor.invoked);
        assertEquals("Exception caught: broadcast", exception.getMessage());
    }

    @Test
    public void testUmbrellaExceptionMessage() {
        final UmbrellaException single = new UmbrellaException(List.of(new RuntimeException("only")));
        assertEquals("Exception caught: only", single.getMessage());
        final UmbrellaException empty = new UmbrellaException(new ArrayList<>());
        assertNull(empty.getMessage());
        assertNull(empty.getCause());
    }

    @Test
    public void testNullArguments() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new EventSource();
        assertThrows(NullPointerException.class, () -> eventBus.fireEvent(null));
        assertThrows(NullPointerException.class, () -> eventBus.fireEventFromSource(null, eventSource));
        assertThrows(NullPointerException.class, () -> eventBus.fireEventFromSource(new ClickTestEvent(null), null));
        assertThrows(NullPointerException.class, () -> eventSource.addHandler(null, new TestEventHandler()));
        assertThrows(NullPointerException.class, () -> eventSource.addHandler(CLICK_EVENT_TYPE, null));
        assertFalse(eventSource.removeHandler(CLICK_EVENT_TYPE, new TestEventHandler()));
        assertTrue(eventSource.getEventHandlers(CLICK_EVENT_TYPE).isEmpty());
        // Removing a broadcast handler that was never added
        eventBus.removeHandler(new TestBroadcastHandler());
    }

    @Test
    public void testHandlerRegistrationAndTinySource() {
        final EventBus eventBus = new EventBus();
        final TinyEventSource eventSource = new TinyEventSource();
        final TestEventHandler handler = new TestEventHandler();
        final HandlerRegistration registration = eventSource.addHandler(CLICK_EVENT_TYPE, handler);
        assertEquals(1, eventSource.getEventHandlers(CLICK_EVENT_TYPE).size());
        assertFalse(eventSource.removeHandler(HOVER_EVENT_TYPE, handler));
        registration.removeHandler();
        eventBus.fireEventFromSource(new ClickTestEvent(eventSource), eventSource);
        assertFalse(handler.invoked);

        final HandlerRegistration global = eventBus.addHandler(CLICK_EVENT_TYPE, handler);
        global.removeHandler();
        eventBus.fireEvent(new ClickTestEvent(null));
        assertFalse(handler.invoked);
    }

    @Test
    public void testEventAndType() {
        final Object source = new Object();
        final ClickTestEvent event = new ClickTestEvent(source);
        assertSame(source, event.getSource());
        assertNull(event.getData());
        event.setData("payload");
        assertEquals("payload", event.getData());
        assertEquals("ClickTestEvent with data : payload", event.toString());

        final Object other = new Object();
        event.setSource(other);
        assertSame(other, event.getSource());

        assertEquals(CLICK_EVENT_TYPE, CLICK_EVENT_TYPE);
        assertNotEquals(CLICK_EVENT_TYPE, HOVER_EVENT_TYPE);
        assertNotEquals(CLICK_EVENT_TYPE, null);
        assertNotEquals(CLICK_EVENT_TYPE, "type");
        assertNotEquals(CLICK_EVENT_TYPE.hashCode(), HOVER_EVENT_TYPE.hashCode());
        assertEquals("Event type", CLICK_EVENT_TYPE.toString());
    }

    @Test
    public void testBusinessAndSystemEvents() {
        final BusinessEvent<TestEventHandler> business = new BusinessEvent<>(this) {

            @Override
            public Type getAssociatedType() {
                return CLICK_EVENT_TYPE;
            }

            @Override
            protected void dispatch(final TestEventHandler handler) {
                handler.invoke();
            }
        };
        assertEquals(BusinessEvent.Level.INFO, business.getLevel());
        business.setLevel(BusinessEvent.Level.ERROR);
        business.setBusinessMessage("message");
        assertEquals(BusinessEvent.Level.ERROR, business.getLevel());
        assertEquals("message", business.getBusinessMessage());

        final EventBus eventBus = new EventBus();
        final TestEventHandler handler = new TestEventHandler();
        eventBus.addHandler(CLICK_EVENT_TYPE, handler);
        eventBus.fireEvent(business);
        assertTrue(handler.invoked);

        final SystemEvent<TestEventHandler> system = new SystemEvent<>(this) {

            @Override
            public Type getAssociatedType() {
                return HOVER_EVENT_TYPE;
            }

            @Override
            protected void dispatch(final TestEventHandler handler) {
                handler.invoke();
            }
        };
        assertSame(this, system.getSource());
    }

    private static class TestEventHandler implements EventHandler {

        private boolean invoked = false;

        void invoke() {
            invoked = true;
        }
    }

    private static class TestBroadcastHandler implements BroadcastEventHandler {

        private boolean invoked = false;

        @Override
        public void onEvent(final Event<?> event) {
            invoked = true;
        }
    }

    private static abstract class TestEvent extends Event<TestEventHandler> {

        protected TestEvent(final Object source) {
            super(source);
        }

        @Override
        protected void dispatch(final TestEventHandler handler) {
            handler.invoke();
        }

    }

    private static class ClickTestEvent extends TestEvent {

        protected ClickTestEvent(final Object source) {
            super(source);
        }

        @Override
        public Type getAssociatedType() {
            return CLICK_EVENT_TYPE;
        }

    }

    private static class HoverTestEvent extends TestEvent {

        protected HoverTestEvent(final Object source) {
            super(source);
        }

        @Override
        public Type getAssociatedType() {
            return HOVER_EVENT_TYPE;
        }

    }
}
