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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ScheduledFuture;

import org.junit.Test;

/**
 * Non-regression tests for {@link ScheduledTaskHandler} — the public cancellation handle wrapping
 * a {@link ScheduledFuture} via a weak reference.
 */
public class ScheduledTaskHandlerTest {

    @Test
    public void testCancel_forwardsToTheFuture() {
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        final ScheduledTaskHandler handler = new ScheduledTaskHandler(future);

        handler.cancel(true);

        verify(future).cancel(true);
    }

    @Test
    public void testCancel_isIdempotent() {
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        final ScheduledTaskHandler handler = new ScheduledTaskHandler(future);

        handler.cancel(false);
        // After the first cancel the weak reference is cleared, so a second cancel must be a safe no-op.
        handler.cancel(false);

        verify(future, times(1)).cancel(false);
    }
}
