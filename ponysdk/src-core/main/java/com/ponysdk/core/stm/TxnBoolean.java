
package com.ponysdk.core.stm;

public class TxnBoolean extends TxnObjectImpl<Boolean> {

    public TxnBoolean() {
        super(Boolean.FALSE);
    }

    public TxnBoolean(final Boolean initialValue) {
        super(initialValue);
    }
}
