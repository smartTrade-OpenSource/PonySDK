
package com.ponysdk.core.stm;

public class TxnObjectImpl<T> implements TxnObject<T>, TxnListener {

    protected T initialValue;

    protected T workingValue;

    protected boolean set;

    protected Txn transaction;

    private TxnObjectListener listener;

    public TxnObjectImpl() {}

    public TxnObjectImpl(final T initialValue) {
        this.initialValue = initialValue;
        this.workingValue = initialValue;
    }

    @Override
    public boolean set(final T object) {
        if (transaction == null) {
            transaction = Txn.get();
            if (transaction == null) {
                if (isEquals(initialValue, object)) return false;
                initialValue = object;
                workingValue = object;
                return true;
            } else {
                transaction.addTnxListener(this);
            }
        }

        set = true;

        if (isEquals(initialValue, object)) return false;

        workingValue = object;

        return true;
    }

    @Override
    public final T get() {
        if (transaction == null) return initialValue;
        return workingValue;
    }

    @Override
    public void commit() {
        reset(workingValue);
    }

    @Override
    public void rollback() {
        reset(initialValue);
    }

    public void setListener(final TxnObjectListener listener) {
        this.listener = listener;
    }

    @Override
    public void reset(final T initialValue) {
        this.initialValue = initialValue;
        this.workingValue = initialValue;
        this.set = false;
        this.transaction = null;
    }

    @Override
    public void beforeCommit() {
        if ((initialValue == null && workingValue == null) || (initialValue != null && initialValue.equals(workingValue))) {
            this.set = false;
            this.transaction = null;
        } else {
            transaction.attach(this);
        }
    }

    @Override
    public void beforeFlush(final TxnContext txnContext) {
        if (listener == null) return;
        listener.beforeFlush(this);
    }

    private static boolean isEquals(final Object initialValue, final Object workingValue) {
        if (initialValue == null && workingValue == null) return true;
        if (initialValue != null && initialValue.equals(workingValue)) return true;
        return false;
    }

    @Override
    public void beforeRollback() {
        // nothing to do
    }

    @Override
    public void afterFlush(final TxnContext txnContext) {
        // nothing to do
    }

}
