
package com.ponysdk.core.stm;

import java.io.IOException;

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.ui.terminal.model.Model;

public class TxnContextHttp implements TxnContext {

    private final Response response;
    private final boolean startMode;

    private final Parser parser;

    public TxnContextHttp(final boolean startMode, final Request request, final Response response) throws IOException {
        this.response = response;
        this.startMode = startMode;
        this.parser = new Parser(response.getWriter());
    }

    @Override
    public void flush() {
        if (startMode) {
            parser.parse(Model.APPLICATION_VIEW_ID, UIContext.get().getUiContextID());
        }

        response.flush();
    }

    @Override
    public Parser getParser() {
        return parser;
    }
}
