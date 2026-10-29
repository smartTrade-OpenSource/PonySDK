/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.impl.spring.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;

/** The scope, on screens as the server creates them, with a socket that goes nowhere. */
class UIScopeTest {

    private final UIScope scope = new UIScope();
    private final List<String> destroyed = new ArrayList<>();
    private UIContext first;
    private UIContext second;

    private static UIContext screen() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        return new UIContext(socket, new TxnContext(socket), new ApplicationConfiguration(), new UpgradeRequestData(null, null, null));
    }

    @BeforeEach
    void screens() {
        first = screen();
        second = screen();
    }

    @AfterEach
    void noScreen() {
        UIContext.remove();
    }

    @Test
    void aScreenGetsItsOwnInstanceAndKeepsIt() {
        UIContext.setCurrent(first);
        final Object a = scope.get("bean", Object::new);
        assertSame(a, scope.get("bean", () -> {
            throw new AssertionError("created once");
        }));
        assertNotSame(a, scope.get("other", Object::new));

        UIContext.setCurrent(second);
        assertNotSame(a, scope.get("bean", Object::new));
    }

    @Test
    void theInstancesAndTheirDestructionCallbacksEndWithTheirScreen() {
        UIContext.setCurrent(first);
        scope.get("a", Object::new);
        scope.registerDestructionCallback("a", () -> destroyed.add("a"));
        UIContext.setCurrent(second);
        scope.get("b", Object::new);
        scope.registerDestructionCallback("b", () -> destroyed.add("b"));

        first.destroy();

        assertEquals(List.of("a"), destroyed, "only the screen that ended");
        UIContext.setCurrent(second);
        assertNotNull(scope.get("b", Object::new));
        assertEquals(List.of("a"), destroyed);
    }

    @Test
    void aBeanRemovedIsCreatedAgainAndNoLongerDestroyedWithTheScreen() {
        UIContext.setCurrent(first);
        final Object a = scope.get("a", Object::new);
        scope.registerDestructionCallback("a", () -> destroyed.add("a"));

        assertSame(a, scope.remove("a"));
        assertNull(scope.remove("a"));
        assertNotSame(a, scope.get("a", Object::new));

        first.destroy();
        assertEquals(List.of(), destroyed, "the callback went with the removal");
    }

    @Test
    void theConversationIsTheScreenAndThereIsNoContextualObject() {
        UIContext.setCurrent(first);
        assertEquals(String.valueOf(first.getID()), scope.getConversationId());
        UIContext.setCurrent(second);
        assertNotEquals(String.valueOf(first.getID()), scope.getConversationId());
        assertNull(scope.resolveContextualObject("anything"));
    }

    @Test
    void outsideAScreenItSaysSo() {
        UIContext.remove();
        final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> scope.get("bean", Object::new));
        assertTrue(failure.getMessage().contains("\"ui\""), failure.getMessage());
        assertThrows(IllegalStateException.class, () -> scope.registerDestructionCallback("bean", () -> {
        }));
        assertThrows(IllegalStateException.class, scope::getConversationId);
    }
}
