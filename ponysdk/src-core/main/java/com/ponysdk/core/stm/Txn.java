
package com.ponysdk.core.stm;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;

public class Txn {

    private static final Logger log = LoggerFactory.getLogger(Txn.class);

    private static ThreadLocal<Txn> transactions = new ThreadLocal<>();

    private final Set<TxnListener> txnListnener = Collections.newSetFromMap(new ConcurrentHashMap<TxnListener, Boolean>());
    private final Set<ClientLoopListener> clientLoopListnener = Collections.newSetFromMap(new ConcurrentHashMap<ClientLoopListener, Boolean>());

    private TxnContext txnContext;

    private final UIContext uiContext;

    private Txn(final UIContext uiContext) {
        this.uiContext = uiContext;
    }

    public static Txn get() {
        Txn txn = transactions.get();

        if (txn == null) {
            txn = new Txn(UIContext.get());
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
            throw new RuntimeException("TMP");
        }
    }

    public TxnContext getTxnContext() {
        return txnContext;
    }

    public void addTnxListener(final TxnListener txnListener) {
        txnListnener.add(txnListener);
    }

    public void addClientLoopListener(final ClientLoopListener listener) {
        clientLoopListnener.add(listener);
    }

    private void fireClientLoopEnd() {
        for (final ClientLoopListener listener : clientLoopListnener) {
            listener.onLoopEnd();
        }
    }

    private void fireBeforeFlush() {
        for (final TxnListener txnListener : txnListnener) {
            txnListener.beforeFlush(txnContext);
        }
    }

    private void fireAfterFlush() {
        for (final TxnListener txnListener : txnListnener) {
            txnListener.afterFlush(txnContext);
        }
    }

    private void fireBeforeRollback() {
        for (final TxnListener txnListener : txnListnener) {
            txnListener.beforeRollback();
        }
    }

}
