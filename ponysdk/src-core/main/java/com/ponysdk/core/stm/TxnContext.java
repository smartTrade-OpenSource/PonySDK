
package com.ponysdk.core.stm;

import java.util.List;

import com.ponysdk.core.instruction.Instruction;

public interface TxnContext {

    void init();

    void save(Instruction instruction);

    void flush() throws Exception;

    void setCurrentStacker(List<Instruction> instructions);

    void removeCurrentStacker();
}
