/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.writer;

import java.lang.ref.WeakReference;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.websocket.WebsocketEncoder;
import com.ponysdk.core.ui.basic.PWindow;

public class ModelWriter {

    private WebsocketEncoder encoder;

    private WeakReference<PWindow> currentWindow;

    /** The UIContext this writer belongs to (set by the UIContext constructor). */
    private UIContext owner;

    public void bindTo(final UIContext owner) {
        this.owner = owner;
    }

    // ---- Coalesced ("last value wins") property updates ----

    /**
     * Models whose update is a pure assignment: setting the property again before the flush makes the
     * previous value irrelevant (the browser applies a whole frame before painting anyway).
     * Anything with side effects (method calls, eval, addon calls, add/remove...) is never deferred.
     */
    private static final boolean[] COALESCIBLE = new boolean[ServerToClientModel.values().length];
    static {
        for (final ServerToClientModel m : new ServerToClientModel[] { ServerToClientModel.TEXT, ServerToClientModel.HTML,
                ServerToClientModel.WIDGET_TITLE, ServerToClientModel.WIDGET_WIDTH, ServerToClientModel.WIDGET_HEIGHT,
                ServerToClientModel.PUT_STYLE_KEY, ServerToClientModel.PUT_ATTRIBUTE_KEY, ServerToClientModel.PUT_PROPERTY_KEY }) {
            COALESCIBLE[m.ordinal()] = true;
        }
    }

    private boolean coalescing;
    private PendingUpdates pending;
    private boolean draining;

    /** Enables update coalescing (see {@link #deferUpdate}). Off by default; turned on by the UIContext. */
    public void setCoalescing(final boolean coalescing) {
        if (!coalescing) drainPendingUpdates();
        this.coalescing = coalescing;
    }

    public boolean isCoalescing() {
        return coalescing;
    }

    /**
     * Defers a single-property update of a live object until the next block or flush. Setting the same
     * property of the same object again replaces the pending value; consecutive deferred updates of one
     * object share a single update block on the wire.
     * <p>
     * Ordering is preserved: any other write (structural change, method call, non-deferrable update,
     * even of another object) first writes everything pending.
     *
     * @return false if the update was not deferred and must be written immediately by the caller
     */
    public boolean deferUpdate(final UpdateTarget target, final ServerToClientModel model, final Object value) {
        if (!coalescing || !COALESCIBLE[model.ordinal()] || draining || !canWrite()) return false;
        pending().put(target, model, null, null, value);
        return true;
    }

    /** Keyed variant (style / attribute / property name + value): coalesced per object and key. */
    public boolean deferUpdate(final UpdateTarget target, final ServerToClientModel keyModel, final String key,
                               final ServerToClientModel valueModel, final Object value) {
        if (!coalescing || !COALESCIBLE[keyModel.ordinal()] || key == null || draining || !canWrite()) return false;
        pending().put(target, keyModel, key, valueModel, value);
        return true;
    }

    private PendingUpdates pending() {
        if (pending == null) pending = new PendingUpdates();
        return pending;
    }

    /** Writes every deferred update now. Called before any other block and before each flush. */
    public void drainPendingUpdates() {
        final PendingUpdates p = pending;
        if (p == null || p.isEmpty() || draining) return;
        draining = true;
        try {
            p.drainTo(this);
        } finally {
            draining = false;
        }
    }

    /** Number of updates currently deferred (diagnostics / tests). */
    public int pendingUpdateCount() {
        return pending == null ? 0 : pending.size();
    }

    /**
     * Writes are only performed for a live context. Fast path: the owner's lock is held by this thread
     * (the normal case, under execute()/acquire()), which avoids a ThreadLocal lookup on every field;
     * otherwise falls back to the current-thread context, exactly as before.
     */
    private boolean canWrite() {
        final UIContext o = owner;
        if (o != null && o.isHeldByCurrentThread()) return o.isAlive();
        final UIContext ctx = UIContext.get();
        return ctx != null && ctx.isAlive();
    }

    public ModelWriter(final WebsocketEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Swaps the underlying encoder — used during transparent WebSocket reconnection
     * to reattach a live UIContext to a new WebSocket session.
     */
    public void setEncoder(final WebsocketEncoder encoder) {
        this.encoder = encoder;
        this.currentWindow = null; // force WINDOW_ID re-emission on next write
    }

    public void beginObject(final PWindow window) {
        if (pending != null && !draining && !pending.isEmpty()) drainPendingUpdates(); // keep write order
        encoder.beginObject();

        if (currentWindow == null || currentWindow.get() != window) {
            currentWindow = new WeakReference<>(window);
            encoder.encodeInt(ServerToClientModel.WINDOW_ID, window.getID());
        }
    }

    public void write(final ServerToClientModel model) {
        write(model, null);
    }

    /**
     * @param value The type can be primitives, String or Object[]
     */
    public void write(final ServerToClientModel model, final Object value) {
        if (canWrite()) {
            encoder.encode(model, value);
        }
    }

    /** Primitive variant of {@link #write(ServerToClientModel, Object)} for int values (no boxing). */
    public void writeInt(final ServerToClientModel model, final int value) {
        if (canWrite()) {
            encoder.encodeInt(model, value);
        }
    }

    public void endObject() {
        if (canWrite()) {
            encoder.endObject();
        }
    }

    public PWindow getCurrentWindow() {
        return currentWindow != null ? currentWindow.get() : null;
    }

}
