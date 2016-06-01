package com.ponysdk.core.writer;

import java.io.IOException;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.server.model.ServerBinaryModel;

public class ModelWriter implements AutoCloseable {
    private final Parser parser;

    public ModelWriter(final Parser parser) {
        this.parser = parser;
    }

    public void writeModel(final ServerBinaryModel model) {
        if (model == null)
            return;
        writeModel(model.getKey(), model.getValue());
    }

    public void writeModel(final ServerToClientModel model, final Object value) {
        if (parser.getPosition() == 0) {
            parser.beginObject();
        }
        parser.parse(model, value);
    }

    @Override
    public void close() throws IOException {
        parser.endObject();
    }
}
