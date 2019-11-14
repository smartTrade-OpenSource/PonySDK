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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ponysdk.test.PSuite;

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
        assertFalse("Click handler should NOT have been invoked", clickEventHandler.invoked);
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(null);
        eventBus.fireEvent(clickEvent);
        assertTrue("Click handler should have been invoked", clickEventHandler.invoked);
        eventBus.removeHandler(CLICK_EVENT_TYPE, clickEventHandler);
        clickEventHandler.invoked = false;
        eventBus.fireEvent(clickEvent);
        assertFalse("Click handler should NOT have been invoked", clickEventHandler.invoked);
    }

    @Test
    public void testSourceHandler() {
        final EventBus eventBus = new EventBus();
        final EventSource eventSource = new EventSource();
        final TestEventHandler clickEventHandler = new TestEventHandler();
        eventSource.addHandler(CLICK_EVENT_TYPE, clickEventHandler);
        final Event<TestEventHandler> hoverEvent = new HoverTestEvent(eventSource);
        eventBus.fireEventFromSource(hoverEvent, eventSource);
        assertFalse("Click handler should NOT have been invoked", clickEventHandler.invoked);
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(eventSource);
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue("Click handler should have been invoked", clickEventHandler.invoked);
        eventSource.removeHandler(CLICK_EVENT_TYPE, clickEventHandler);
        clickEventHandler.invoked = false;
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertFalse("Click handler should NOT have been invoked", clickEventHandler.invoked);
    }

    @Test
    public void testBroadcastHandler() {
        final EventBus eventBus = new EventBus();
        final TestBroadcastHandler broadcastHandler = new TestBroadcastHandler();
        eventBus.addHandler(broadcastHandler);
        assertFalse("Broadcast handler should NOT have been invoked", broadcastHandler.invoked);
        final Event<TestEventHandler> clickEvent = new ClickTestEvent(null);
        eventBus.fireEvent(clickEvent);
        assertTrue("Broadcast handler should have been invoked", broadcastHandler.invoked);
        eventBus.removeHandler(broadcastHandler);
        broadcastHandler.invoked = false;
        eventBus.fireEvent(clickEvent);
        assertFalse("Broadcast handler should NOT have been invoked", broadcastHandler.invoked);
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
        assertFalse("Click handler should NOT have been invoked", clickEventHandler.invoked);
        assertFalse("Hover handler should NOT have been invoked", hoverEventHandler.invoked);
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue("Click handler should have been invoked", clickEventHandler.invoked);
        assertFalse("Hover handler should NOT have been invoked", hoverEventHandler.invoked);
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
        assertFalse("Click handler 1 should NOT have been invoked", clickEventHandler.invoked);
        assertFalse("Click handler 2 should NOT have been invoked", clickEventHandler2.invoked);
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue("Click handler 1 should have been invoked", clickEventHandler.invoked);
        assertTrue("Click handler 2 should have been invoked", clickEventHandler2.invoked);
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
        assertFalse("Click handler 1 should NOT have been invoked", clickEventHandler.invoked);
        assertFalse("Click handler 2 should NOT have been invoked", clickEventHandler2.invoked);
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue("Click handler 1 should have been invoked", clickEventHandler.invoked);
        assertFalse("Click handler 2 should NOT have been invoked", clickEventHandler2.invoked);
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
        assertFalse("Click handler 1 should NOT have been invoked", clickEventHandler.invoked);
        assertFalse("Click handler 2 should NOT have been invoked", clickEventHandler2.invoked);
        eventBus.fireEventFromSource(clickEvent, eventSource);
        assertTrue("Click handler 1 should have been invoked", clickEventHandler.invoked);
        assertTrue("Click handler 2 should have been invoked", clickEventHandler2.invoked);
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
