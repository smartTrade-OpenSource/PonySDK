
package com.ponysdk.core.stm;

public class TxnInteger extends TxnObjectImpl<Integer> {

    private static final Integer ZERO = 0;

    public TxnInteger(final Integer initialValue) {
        super(initialValue);
    }

    public TxnInteger() {
        super(ZERO);
    }

    public Integer incrementAndGet() {
        final Integer value = get() + 1;
        set(value);
        return value;
    }

}
