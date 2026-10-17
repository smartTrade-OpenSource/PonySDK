/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ponysdk.core.server.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.ponysdk.core.server.application.UIContext;

/**
 * Non-regression tests for {@link UIDelegate}: a {@link java.util.function.Consumer} wrapper that
 * re-dispatches the callback onto its owning {@link UIContext} (so the consumer runs under the
 * UIContext lock).
 */
public class UIDelegateTest {

    @Test
    public void testAccept_runsConsumerInsideUIContextExecute() {
        final UIContext uiContext = mock(UIContext.class);
        // Make execute(Runnable) actually run the runnable, as the real UIContext would.
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        }).when(uiContext).execute(any(Runnable.class));

        final AtomicReference<String> received = new AtomicReference<>();
        final UIDelegate<String> delegate = new UIDelegate<>(received::set, uiContext);

        delegate.accept("payload");

        verify(uiContext).execute(any(Runnable.class));
        assertEquals("consumer must receive the value, dispatched via UIContext.execute", "payload", received.get());
    }

    @Test
    public void testAccept_doesNotRunConsumerWhenUIContextDoesNotExecute() {
        final UIContext uiContext = mock(UIContext.class);
        // execute() is a no-op (returns false): the consumer must NOT run on the calling thread.
        final AtomicReference<String> received = new AtomicReference<>();
        final UIDelegate<String> delegate = new UIDelegate<>(received::set, uiContext);

        delegate.accept("payload");

        verify(uiContext).execute(any(Runnable.class));
        assertNull("consumer must only run through UIContext.execute, never directly", received.get());
    }
}
