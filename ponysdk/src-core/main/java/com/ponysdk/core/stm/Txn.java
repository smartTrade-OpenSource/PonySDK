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

package com.ponysdk.core.stm;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Txn {

    private static ThreadLocal<Txn> transactions = new ThreadLocal<>();

    private final Set<TxnListener> txnListener = Collections.newSetFromMap(new ConcurrentHashMap<TxnListener, Boolean>());
    private final Set<ClientLoopListener> clientLoopListener = Collections
            .newSetFromMap(new ConcurrentHashMap<ClientLoopListener, Boolean>());

    private TxnContext txnContext;

    public static Txn get() {
        Txn txn = transactions.get();

        if (txn == null) {
            txn = new Txn();
            transactions.set(txn);
        }
        return txn;
    }

    public void begin(final TxnContext txnContext) {
        this.txnContext = txnContext;
    }

    public void commit() {
        final Txn txn = transactions.get();
        if (txn.txnContext == null) throw new RuntimeException("Call begin() before commit() a transaction.");
        fireClientLoopEnd();
        fireBeforeFlush();
        flush();
        fireAfterFlush();
        transactions.remove();
    }

    public void rollback() {
        final Txn txn = transactions.get();
        if (txn.txnContext == null) throw new RuntimeException("Call begin() before rollback() a transaction.");
        fireBeforeRollback();
        transactions.remove();
    }

    public void flush() {
        try {
            txnContext.flush();
        } catch (final Exception e) {
            // final String msg = "Cannot send instructions to the browser, Session ID #" +
            // uiContext.getSession().getId();
            // log.error(msg, e);
            // throw new RuntimeException(msg);
            throw new RuntimeException("TMP", e);
        }
    }

    public TxnContext getTxnContext() {
        return txnContext;
    }

    public void addTnxListener(final TxnListener txnListener) {
        this.txnListener.add(txnListener);
    }

    public void addClientLoopListener(final ClientLoopListener listener) {
        clientLoopListener.add(listener);
    }

    private void fireClientLoopEnd() {
        for (final ClientLoopListener listener : clientLoopListener) {
            listener.onLoopEnd();
        }
    }

    private void fireBeforeFlush() {
        for (final TxnListener txnListener : txnListener) {
            txnListener.beforeFlush(txnContext);
        }
    }

    private void fireAfterFlush() {
        for (final TxnListener txnListener : txnListener) {
            txnListener.afterFlush(txnContext);
        }
    }

    private void fireBeforeRollback() {
        for (final TxnListener txnListener : txnListener) {
            txnListener.beforeRollback();
        }
    }

}
