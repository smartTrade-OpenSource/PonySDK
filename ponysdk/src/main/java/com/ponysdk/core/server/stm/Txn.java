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

import com.ponysdk.core.server.application.UIContext;

import java.util.HashSet;
import java.util.Set;

public class Txn {

    private static final ThreadLocal<Txn> transactions = ThreadLocal.withInitial(Txn::new);

    private final Set<TxnListener> txnListeners = new HashSet<>();

    private UIContext uiContext;

    public static Txn get() {
        return transactions.get();
    }

    //public final ModelWriter getWriter() {
    //    if (uiContext != null) return uiContext.getWriter();
    //    else throw new AlreadyDestroyedApplication("TxnContext destroyed");
    //}

    public void begin(final UIContext uiContext) {
        this.uiContext = uiContext;
    }

    public void commit() {
        if (uiContext == null) throw new RuntimeException("Call begin() before commit() a transaction.");
        fireBeforeFlush();
        flush();
        fireAfterFlush();
        transactions.remove();
    }

    public void rollback() {
        if (uiContext == null) throw new RuntimeException("Call begin() before rollback() a transaction.");
        fireBeforeRollback();
        transactions.remove();
    }

    public void flush() {
        uiContext.flush();
    }

    public void addTxnListener(final TxnListener txnListener) {
        this.txnListeners.add(txnListener);
    }

    private void fireBeforeFlush() {
        txnListeners.forEach(listener -> listener.beforeFlush(this));
    }

    private void fireAfterFlush() {
        txnListeners.forEach(listener -> listener.afterFlush(this));
    }

    private void fireBeforeRollback() {
        txnListeners.forEach(TxnListener::beforeRollback);
    }

}
