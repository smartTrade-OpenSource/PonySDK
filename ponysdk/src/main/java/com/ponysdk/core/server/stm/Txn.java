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

import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.writer.ModelWriter;

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

    public static ModelWriter getWriter() {
        return get().getWriter0();
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
        try {
            txnContext.flush();
        } catch (final Exception e) {
            // final String msg = "Cannot send instructions to the browser,
            // Session ID #" +
            // uiContext.getSession().getId();
            // throw new RuntimeException(msg);
            // throw new RuntimeException("TMP", e);
        }
    }

    private ModelWriter getWriter0() {
        return txnContext.getWriter();
    }

    public Parser getParser() {
        return txnContext.getParser();
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
