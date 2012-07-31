
package com.ponysdk.core.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.HandlerRegistration;
import com.ponysdk.ui.server.basic.PPusher;

public abstract class AbstractPushCommand<T> implements PushListener<T>, Command<HandlerRegistration> {

    private final Logger log = LoggerFactory.getLogger(AbstractPushCommand.class);

    private final PPusher pusher;

    private final PushListener<T> listener;

    public AbstractPushCommand(final PushListener<T> listener) {
        this.listener = listener;
        this.pusher = PPusher.get();

        if (this.listener == null) throw new RuntimeException("'listener' cannot be null");
        if (this.pusher == null) throw new RuntimeException("It's not possible to instanciate a push command in a new Thread.");
    }

    @Override
    public void onMessage(final T msg) {
        pusher.begin();
        try {
            listener.onMessage(msg);
            PPusher.get().flush();
        } catch (final Exception exception) {
            log.error("Cannot push data", exception);
        } finally {
            PPusher.get().end();
        }
    }
}
