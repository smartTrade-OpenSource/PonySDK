
package com.ponysdk.core.stm;

import com.ponysdk.core.instruction.Instruction;

public interface TxnContext {

    void save(Instruction instruction);

    void flush() throws Exception;

    void clear();
}
