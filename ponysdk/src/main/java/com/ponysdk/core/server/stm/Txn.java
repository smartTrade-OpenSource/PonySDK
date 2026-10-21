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

package com.ponysdk.core.server.stm;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Txn {

    private static final Logger log = LoggerFactory.getLogger(Txn.class);

    private static final ThreadLocal<Txn> transactions = new ThreadLocal<>();

    // Lazily created: listeners live for a single transaction (commit/rollback drop them), so an
    // eager set was allocated on every transaction although almost none register a listener.
    // Concurrent set kept so a listener may register another one while listeners are being fired.
    private Set<TxnListener> txnListeners;

    private TxnContext txnContext;

    public static Txn get() {
        Txn txn = transactions.get();

        if (txn == null) {
            txn = new Txn();
            transactions.set(txn);
        }
        return txn;
    }

    /** True between {@link #begin} and commit / rollback. */
    public boolean isActive() {
        return txnContext != null;
    }

    /**
     * Makes {@code txn} this thread's transaction and returns the previous one: a transaction on another context
     * opened from inside an active one suspends it, then puts it back.
     */
    public static Txn replace(final Txn txn) {
        final Txn previous = transactions.get();
        transactions.set(txn);
        return previous;
    }

    public void begin(final TxnContext txnContext) {
        this.txnContext = txnContext;
    }

    public void commit() {
        if (txnContext == null) throw new RuntimeException("Call begin() before commit() a transaction.");
        fireBeforeFlush();
        flush();
        fireAfterFlush();
        end();
    }

    /**
     * Ends a transaction that failed. What it wrote is sent all the same: the objects it changed keep their changes,
     * and the terminal must see them (a block the failure interrupted is ended where it stopped).
     */
    public void rollback() {
        if (txnContext == null) throw new RuntimeException("Call begin() before rollback() a transaction.");
        try {
            fireBeforeRollback();
            flush();
        } catch (final Throwable e) {
            log.error("Cannot send what a failed transaction wrote", e);
        } finally {
            end();
        }
    }

    /** Ends the transaction but keeps the instance as this thread's next one: no allocation per execute(). */
    private void end() {
        txnContext = null;
        txnListeners = null;
    }

    public void flush() {
        txnContext.flush();
    }

    public void addTxnListener(final TxnListener txnListener) {
        if (txnListeners == null) txnListeners = ConcurrentHashMap.newKeySet();
        this.txnListeners.add(txnListener);
    }

    private void fireBeforeFlush() {
        if (txnListeners != null) txnListeners.forEach(listener -> listener.beforeFlush(txnContext));
    }

    private void fireAfterFlush() {
        if (txnListeners != null) txnListeners.forEach(listener -> listener.afterFlush(txnContext));
    }

    private void fireBeforeRollback() {
        if (txnListeners != null) txnListeners.forEach(TxnListener::beforeRollback);
    }

}
