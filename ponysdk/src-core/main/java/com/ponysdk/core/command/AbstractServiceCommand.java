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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.command.event.ServiceFailedEvent;
import com.ponysdk.core.event.EventBus;

public abstract class AbstractServiceCommand<T> implements AsyncCallback<T>, Command {

    private static final Logger log = LoggerFactory.getLogger(AbstractServiceCommand.class.getName());

    private final Set<AsyncCallback<T>> callbacks = new HashSet<AsyncCallback<T>>();

    private final EventBus eventBus;

    public AbstractServiceCommand() {
        this(PonySession.getRootEventBus());
    }

    public AbstractServiceCommand(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onSuccess(T result) {
        doAfterSuccess(result);
        fireSuccess(result);
        doFinally();
    }

    @Override
    public void onFailure(Throwable caught) {
        log.error("Command " + this.getClass() + " failed.", caught);
        fireFailure(caught);
        doAfterFailure(caught);
        doFinally();
    }

    @Override
    public void execute() {
        try {
            final T result = execute0();
            onSuccess(result);
        } catch (final Exception e) {
            log.error("service command execution failed", e);
            onFailure(e);
        }
    }

    protected abstract T execute0() throws Exception;

    protected abstract void doAfterSuccess(T result);

    protected void doAfterFailure(final Throwable caught) {
        final ServiceFailedEvent event = new ServiceFailedEvent(AbstractServiceCommand.this, caught);
        eventBus.fireEvent(event);
    }

    protected void doFinally() {
        // Do nothing
    }

    public void addAsyncCallback(AsyncCallback<T> callback) {
        callbacks.add(callback);
    }

    private void fireSuccess(T result) {
        for (final AsyncCallback<T> callback : callbacks) {
            callback.onSuccess(result);
        }
    }

    private void fireFailure(Throwable caught) {
        for (final AsyncCallback<T> callback : callbacks) {
            callback.onFailure(caught);
        }
    }

}
