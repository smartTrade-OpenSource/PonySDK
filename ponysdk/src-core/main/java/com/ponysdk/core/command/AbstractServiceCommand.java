/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.command;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.command.event.ServiceFailedEvent;
import com.ponysdk.core.event.EventBus;

public abstract class AbstractServiceCommand<T> implements AsyncCallback<T>, Command<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractServiceCommand.class.getName());

    private final Set<AsyncCallback<T>> callbacks = new HashSet<AsyncCallback<T>>();

    private final EventBus eventBus;

    private Throwable caught;

    protected long executionTime = 0;

    public AbstractServiceCommand() {
        this(UIContext.getRootEventBus());
    }

    public AbstractServiceCommand(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onSuccess(final T result) {
        doAfterSuccess(result);
        fireSuccess(result);
    }

    @Override
    public void onFailure(final Throwable caught) {
        log.error("Command " + this.getClass() + " failed.", caught);
        this.caught = caught;

        fireFailure(caught);
        doAfterFailure(caught);
    }

    @Override
    public T execute() {

        final long start = System.nanoTime();
        try {
            final T result = execute0();
            onSuccess(result);
            return result;
        } catch (final Throwable e) {
            onFailure(e);
            return null;
        } finally {
            executionTime = (System.nanoTime() - start);
            if (log.isDebugEnabled()) log.debug("execution time = " + (executionTime * 0.000000001f) + " ms ");
        }
    }

    protected abstract T execute0() throws Exception;

    protected void doAfterSuccess(final T result) {
        // Do nothing
    }

    protected void doAfterFailure(final Throwable caught) {
        final ServiceFailedEvent event = new ServiceFailedEvent(AbstractServiceCommand.this, caught);
        eventBus.fireEvent(event);
    }

    public void addAsyncCallback(final AsyncCallback<T> callback) {
        callbacks.add(callback);
    }

    private void fireSuccess(final T result) {
        for (final AsyncCallback<T> callback : callbacks) {
            callback.onSuccess(result);
        }
    }

    private void fireFailure(final Throwable caught) {
        for (final AsyncCallback<T> callback : callbacks) {
            callback.onFailure(caught);
        }
    }

    public Throwable getCaught() {
        return caught;
    }

    public boolean isSuccessfull() {
        return (caught == null);
    }

    public long getExecutionTime(final TimeUnit timeUnit) {
        return timeUnit.convert(executionTime, TimeUnit.NANOSECONDS);
    }

}
