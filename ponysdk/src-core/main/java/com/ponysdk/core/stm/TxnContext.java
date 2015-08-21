
package com.ponysdk.core.stm;

import com.ponysdk.core.Parser;

public interface TxnContext {

    void flush();

    Parser getParser();
}
