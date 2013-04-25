
package com.ponysdk.core.stm;

import java.util.List;

import com.ponysdk.core.instruction.Instruction;

public interface TxnContext {

    void save(Instruction instruction);

    void flush() throws Exception;

    List<Instruction> setCurrentStacker(List<Instruction> instructions);

    void clear();
}
