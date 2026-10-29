/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.servlet.SessionManager;

/**
 * Moves the pages of a server that is about to stop to the other servers: each live context is destroyed as
 * {@link SessionAdmin#destroy} does, its client told; the page reloads at its history token, and the load balancer,
 * which no longer sends anything to a server it drains, sends it to another one. The contexts go one by one over
 * most of a window, so that the other servers take them in waves rather than all at once; one created meanwhile
 * goes too.
 * <p>
 * What survives a move is what a reload keeps: the history token (the page's address), the browser's cookies and
 * storage. {@code PonySDKServer.setDrainMillis} runs it when the JVM is asked to stop (SIGTERM).
 */
public final class Drain {

    private static final Logger log = LoggerFactory.getLogger(Drain.class);

    private Drain() {
    }

    /**
     * Moves the live contexts over four fifths of the window, then waits for them to be gone until its end.
     *
     * @return how many contexts were moved
     */
    public static int run(final Duration window) throws InterruptedException {
        final long start = System.nanoTime();
        final long deadline = start + window.toNanos();
        final Set<UIContext> moved = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<UIContext> first = live();
        log.info("Drain: {} live contexts to move over {} ms", first.size(), window.toMillis());
        final long pace = first.isEmpty() ? 0 : window.toNanos() * 4 / 5 / first.size();
        long next = start;
        UIContext context;
        while ((context = unmoved(moved)) != null && System.nanoTime() < deadline) {
            moved.add(context);
            context.destroyByAdministrator();
            next += pace;
            if (unmoved(moved) != null) sleepUntil(Math.min(next, deadline)); // a pace between two, none after the last
        }
        while (!live().isEmpty() && System.nanoTime() < deadline) Thread.sleep(10); // the clients told, the sockets closed
        log.info("Drain: {} contexts moved in {} ms", moved.size(), Duration.ofNanos(System.nanoTime() - start).toMillis());
        return moved.size();
    }

    private static UIContext unmoved(final Set<UIContext> moved) {
        return live().stream().filter(c -> !moved.contains(c)).findFirst().orElse(null);
    }

    /** A time already passed does not sleep: {@code Thread.sleep} returns at once for a negative duration. */
    private static void sleepUntil(final long nanoTime) throws InterruptedException {
        Thread.sleep(Duration.ofNanos(nanoTime - System.nanoTime()));
    }

    /** The live contexts of every application of this server. */
    static List<UIContext> live() {
        return SessionManager.get().getApplications().stream().flatMap(application -> application.getUIContexts().stream())
            .filter(UIContext::isAlive).toList();
    }
}
