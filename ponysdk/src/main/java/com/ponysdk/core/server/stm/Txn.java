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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Txn {

    private static final ThreadLocal<Txn> transactions = new ThreadLocal<>();

    private final Set<TxnListener> txnListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        txnContext.flush();
    }

    public void addTxnListener(final TxnListener txnListener) {
        this.txnListeners.add(txnListener);
    }

    private void fireBeforeFlush() {
        txnListeners.forEach(listener -> listener.beforeFlush(txnContext));
    }

    private void fireAfterFlush() {
        txnListeners.forEach(listener -> listener.afterFlush(txnContext));
    }

    private void fireBeforeRollback() {
        txnListeners.forEach(TxnListener::beforeRollback);
    }

}
