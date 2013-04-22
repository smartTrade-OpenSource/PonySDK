
package com.ponysdk.core.stm;

public class TxnLong extends TxnObjectImpl<Long> {

    private static final Long ZERO = 0L;

    public TxnLong() {
        super(ZERO);
    }

    public Long incrementAndGet() {
        final Long value = get() + 1;
        set(value);
        return value;
    }
}
