
package com.ponysdk.core.stm;

public interface TxnListener {

    void beforeFlush(TxnContext txnContext);

    void beforeRollback();

    void afterFlush(TxnContext txnContext);

}
