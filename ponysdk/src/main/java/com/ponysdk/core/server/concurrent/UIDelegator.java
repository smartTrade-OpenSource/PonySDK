/*
 * Copyright (c) 2016 PonySDK
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

package com.ponysdk.core.server.concurrent;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;

public class UIDelegator<R> {

    public static interface Callback<R> {

        public void onSuccess(R result);

        public void onError(R result, Exception exception);
    }

    private final UIContext uiContext;
    private final ScheduledExecutorService service = Executors
        .newSingleThreadScheduledExecutor(new NamingThreadFactory(getClass().getName()));

    private ScheduledFuture<R> schedule;

    public UIDelegator() {
        this.uiContext = UIContext.get();
    }

    public ScheduledFuture<R> delegate(final Callable<R> businnesMethod, final Callback<R> uiCallback) {
        return delegate(businnesMethod, uiCallback, Duration.ofMillis(0));
    }

    public ScheduledFuture<R> delegate(final Callable<R> businnesMethod, final Callback<R> uiCallback, final Duration delay) {
        if (schedule != null) {
            schedule.cancel(true);
            if (schedule.isCancelled()) schedule = null;
        }

        schedule = service.schedule(() -> execute(businnesMethod, uiCallback), delay.toMillis(), TimeUnit.MILLISECONDS);

        return schedule;
    }

    public R execute(final Callable<R> businnesMethod, final Callback<R> uiCallback) throws Exception {
        R businessResult = null;
        try {
            final Thread currentThread = Thread.currentThread();

            if (!currentThread.isInterrupted()) businessResult = businnesMethod.call();

            if (!currentThread.isInterrupted()) onResult(businessResult, uiCallback);

            return businessResult;
        } catch (final Exception e) {
            throw e;
        }
    }

    private void onResult(final R businessResult, final Callback<R> uiCallback) {
        uiContext.begin();

        try {
            final Txn txn = Txn.get();
            txn.begin(uiContext.getContext());
            try {
                uiCallback.onSuccess(businessResult);
                txn.commit();
            } catch (final Exception e) {
                txn.rollback();
                uiCallback.onError(businessResult, e);
            }
        } finally {
            uiContext.end();
        }
    }

}
