
package com.ponysdk.core.stm;

import com.ponysdk.core.Parser;
import com.ponysdk.core.socket.WebSocket;

public class TxnSocketContext implements TxnContext, TxnListener {

    private WebSocket socket;

    private boolean polling = false;

    private boolean flushNow = false;

    private Parser parser;

    public TxnSocketContext() {}

    public void setSocket(final WebSocket socket) {
        this.socket = socket;
        this.parser = new Parser(socket.getByteBuffer());
    }

    @Override
    public void flush() {
        if (polling) return;
        socket.flush();
    }

    public void switchToPollingMode() {
        polling = true;
    }

    public void flushNow() {
        flushNow = true;
        Txn.get().addTnxListener(this);
    }

    @Override
    public void beforeFlush(final TxnContext txnContext) {
        if (!flushNow) return;

        flushNow = false;

        Txn.get().getTxnContext().flush();
    }

    @Override
    public void beforeRollback() {}

    @Override
    public void afterFlush(final TxnContext txnContext) {}

    @Override
    public Parser getParser() {
        return parser;
    }

}
