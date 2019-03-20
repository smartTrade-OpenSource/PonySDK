package com.ponysdk.core.server.context;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.concurrent.ScheduledTaskHandler;
import com.ponysdk.core.server.monitoring.Monitor;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;

import java.time.Duration;

/**
 * <p>
 * Provides a way to identify a user across more than one page request or visit to a Web site and to
 * store information about that user.
 * </p>
 * <p>There is ONE unique UIContext for each screen displayed. Each UIContext is bound to the current {@link Application}.</p>
 */
public interface UIContext {

    int getID();

    void execute(final Runnable task);

    ScheduledTaskHandler execute(final Runnable task, Duration delay);

    ScheduledTaskHandler repeat(final Runnable task, Duration period);

    PObject getObject(final int objectID);

    void fireEvent(final Event<? extends EventHandler> event);

    void fireEventFromSource(final Event<? extends EventHandler> event, final Object source);

    void setAttribute(final String name, final Object value);

    Object removeAttribute(final String name);

    <T> T getAttribute(final String name);

    public PCookies getCookies();

    public Monitor getMonitor();

    public History getHistory();
}