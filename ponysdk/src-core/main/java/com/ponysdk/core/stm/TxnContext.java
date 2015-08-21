
package com.ponysdk.core.stm;

import com.ponysdk.core.instruction.Parser;

public interface TxnContext {

    void flush();

    Parser getParser();
}
