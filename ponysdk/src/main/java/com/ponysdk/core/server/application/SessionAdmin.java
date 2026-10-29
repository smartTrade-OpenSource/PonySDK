/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.servlet.SessionManager;

/**
 * Server-side administration of the live sessions (ADR-0001): lists them, drops a connection, holds a reconnection,
 * destroys a session. Bound to a caller, typically the {@link UIContext} of an admin screen:
 *
 * <pre>
 * SessionAdmin admin = SessionAdmin.of(UIContext.get());
 * admin.holdReconnection(id, true);
 * admin.dropConnection(id);
 * </pre>
 *
 * Every call goes through the caller's {@link SessionAccessPolicy}, by default its own HTTP session only. A session
 * the caller may not {@link SessionAction#VIEW} is handled as an unknown id, so that ids cannot be probed. Actions,
 * and refusals, are logged on the {@code com.ponysdk.audit.SessionAdmin} logger. Nothing here is reachable from a
 * client. Safe from any thread: the caller never takes a target's lock.
 * <p>
 * Covers the contexts of the HTTP sessions known to {@link SessionManager}: a server without session handler has none.
 */
public final class SessionAdmin {

    private static final Logger audit = LoggerFactory.getLogger("com.ponysdk.audit.SessionAdmin");

    private final UIContext caller;
    private final SessionAccessPolicy policy;

    private SessionAdmin(final UIContext caller) {
        this.caller = caller;
        this.policy = caller.getConfiguration().getSessionAccessPolicy();
    }

    public static SessionAdmin of(final UIContext caller) {
        return new SessionAdmin(Objects.requireNonNull(caller, "caller"));
    }

    /** The live sessions the caller may see, by id. */
    public List<SessionInfo> sessions() {
        return live().filter(this::visible).map(UIContext::snapshot).sorted((a, b) -> Integer.compare(a.id(), b.id()))
            .toList();
    }

    /** The session {@code id}, if it is live and the caller may see it. */
    public Optional<SessionInfo> session(final int id) {
        return find(id).map(UIContext::snapshot);
    }

    /**
     * Closes the session's WebSocket as a network failure would: its client reconnects. Without transparent
     * reconnection, the session is destroyed, as on any lost connection.
     */
    public void dropConnection(final int id) {
        act(id, SessionAction.DROP, "", UIContext::dropConnection);
    }

    /**
     * While held, the session's reconnection attempts are refused and its client retries every 2 s; releasing lets
     * the next attempt resume it. The reconnection window and the recording cap still apply.
     */
    public void holdReconnection(final int id, final boolean hold) {
        act(id, SessionAction.HOLD, hold ? " on" : " off", target -> target.setReconnectionHeld(hold));
    }

    /** Destroys the session, asynchronously: a connected client is told and reloads. */
    public void destroy(final int id) {
        act(id, SessionAction.DESTROY, "", UIContext::destroyByAdministrator);
    }

    private void act(final int id, final SessionAction action, final String detail, final Consumer<UIContext> effect) {
        final UIContext target = find(id).orElseThrow(() -> new NoSuchElementException("No session #" + id));
        if (!policy.allows(caller, target, action)) {
            audit.warn("UIContext #{} {}{} #{}: denied", caller.getID(), action, detail, id);
            throw new SecurityException(action + " on session #" + id + " denied");
        }
        effect.accept(target);
        audit.info("UIContext #{} {}{} #{}: done", caller.getID(), action, detail, id);
    }

    private Optional<UIContext> find(final int id) {
        final Optional<UIContext> target = live().filter(c -> c.getID() == id).findFirst();
        if (target.isPresent() && !visible(target.get())) {
            audit.warn("UIContext #{} VIEW #{}: denied", caller.getID(), id);
            return Optional.empty();
        }
        return target;
    }

    private boolean visible(final UIContext target) {
        return policy.allows(caller, target, SessionAction.VIEW);
    }

    private static Stream<UIContext> live() {
        return SessionManager.get().getApplications().stream().flatMap(application -> application.getUIContexts().stream())
            .filter(UIContext::isAlive);
    }
}
