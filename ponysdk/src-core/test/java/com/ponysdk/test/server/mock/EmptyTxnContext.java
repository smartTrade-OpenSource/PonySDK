
package com.ponysdk.test.server.mock;

import com.ponysdk.core.Parser;
import com.ponysdk.core.stm.TxnContext;

public class EmptyTxnContext implements TxnContext {

    @Override
    public void flush() {}

    @Override
    public Parser getParser() {
        return null;
    }

}
