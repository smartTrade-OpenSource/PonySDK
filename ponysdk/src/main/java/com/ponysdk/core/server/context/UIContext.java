package com.ponysdk.core.server.context;

import com.ponysdk.core.server.concurrent.Scheduler;
import com.ponysdk.core.ui.eventbus2.EventBus;

import java.time.Duration;

public interface UIContext {
    /**
     * Gets the unique identifier of the UI context.
     *
     * @return int representing the unique identifier.
     */
    int getID();

    /**
     * Starts the UI context.
     */
    void start();

    /**
     * Stops the UI context.
     */
    void stop();

    /**
     * Checks if the UI context is currently running.
     *
     * @return true if the context is active, otherwise false.
     */
    boolean isRunning();

    /**
     * Gets the event bus associated with the UI context.
     *
     * @return EventBus associated with the context.
     */
    EventBus getEventBus();

    /**
     * Processes the given instructions for the UI context.
     *
     * @param instructions The instructions to be processed.
     */
    void processInstruction(String instructions);

    /**
     * Adds a listener to monitor changes in the life cycle of the UI context.
     *
     * @param listener The listener to be added.
     */
    void addLifeCycleListener(UIContextLifeCycle listener);

    /**
     * Executes a task asynchronously.
     *
     * @param task The task to be executed.
     */
    void executeAsync(final Runnable task);

    /**
     * Executes a task asynchronously after a specified delay.
     *
     * @param delay The delay before the task is executed.
     * @param task  The task to be executed.
     * @return A handler for the scheduled task.
     */
    Scheduler.ScheduledTaskHandler executeLaterAsync(Duration delay, Runnable task);

    /**
     * Schedules a task to be executed asynchronously at fixed intervals.
     *
     * @param period The period between successive executions.
     * @param task   The task to be executed.
     * @return A handler for the scheduled task.
     */
    Scheduler.ScheduledTaskHandler scheduleAsync(Duration period, Runnable task);
}
