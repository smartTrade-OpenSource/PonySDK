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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Test;

import com.ponysdk.core.server.application.UIContext;

/**
 * Non-regression tests for {@link UIDelegator}: a {@link Consumer} wrapper that re-dispatches the
 * callback onto its owning {@link UIContext} <em>via {@link PScheduler}</em> (so the consumer runs
 * under the UIContext lock, on the scheduler's thread).
 *
 * <p>No production refactor needed: the test mocks a live {@link UIContext} whose {@code execute}
 * runs the submitted runnable, and awaits the asynchronous dispatch with a latch.
 */
public class UIDelegatorTest {

    private static UIContext liveContextRunningInline() {
        final UIContext ui = mock(UIContext.class);
        when(ui.isAlive()).thenReturn(true);
        doAnswer(inv -> {
            inv.getArgument(0, Runnable.class).run();
            return true;
        }).when(ui).execute(any(Runnable.class));
        return ui;
    }

    @Test
    public void testAccept_dispatchesConsumerOntoUIContext() throws InterruptedException {
        final UIContext ui = liveContextRunningInline();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> received = new AtomicReference<>();

        final UIDelegator<String> delegator = new UIDelegator<>(v -> {
            received.set(v);
            latch.countDown();
        }, ui);

        delegator.accept("payload");

        assertTrue("consumer must be dispatched asynchronously via PScheduler", latch.await(5, SECONDS));
        assertEquals("payload", received.get());
        verify(ui).execute(any(Runnable.class));
    }

    @Test
    public void testPSchedulerDelegate_buildsAWorkingDelegator() throws InterruptedException {
        final UIContext ui = liveContextRunningInline();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> received = new AtomicReference<>();

        final Consumer<String> delegate = PScheduler.delegate(s -> {
            received.set(s);
            latch.countDown();
        }, ui);
        delegate.accept("hello");

        assertTrue("PScheduler.delegate must produce a UIContext-dispatching consumer", latch.await(5, SECONDS));
        assertEquals("hello", received.get());
    }
}
