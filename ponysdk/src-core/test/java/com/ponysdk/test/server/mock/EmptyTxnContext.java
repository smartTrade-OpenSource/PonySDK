
package com.ponysdk.test.server.mock;

import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.stm.TxnContext;

public class EmptyTxnContext implements TxnContext {

    @Override
    public void save(final Instruction instruction) {}

    @Override
    public void flush() throws Exception {}

    @Override
    public void clear() {}
}
