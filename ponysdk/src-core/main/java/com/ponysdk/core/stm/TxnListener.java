
package com.ponysdk.core.stm;

public interface TxnListener {

    void beforeCommit();

    void beforeFlush(TxnContext txnContext);

    void beforeRollback();

    void afterFlush(TxnContext txnContext);

}
