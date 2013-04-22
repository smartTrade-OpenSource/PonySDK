
package com.ponysdk.core.stm;

public interface TxnObjectListener {

    void beforeFlush(TxnObject<?> txnObject);

}
