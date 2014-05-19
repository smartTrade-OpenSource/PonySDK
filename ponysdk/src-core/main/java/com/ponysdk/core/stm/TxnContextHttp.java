
package com.ponysdk.core.stm;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class TxnContextHttp implements TxnContext {

    private static final JsonFactory factory = new JsonFactory();

    private final ThreadLocal<JsonGenerator> generators = new ThreadLocal<JsonGenerator>();

    private List<Instruction> stacker;

    private final boolean startMode;

    public TxnContextHttp(final boolean startMode, final Request request, final Response response) throws IOException {
        this.startMode = startMode;
        this.generators.set(factory.createGenerator(response.getOutputStream()));
    }

    @Override
    public void init() {
        final JsonGenerator generator = generators.get();
        try {
            generator.writeStartObject();
            if (startMode) generator.writeNumberField(APPLICATION.VIEW_ID, UIContext.get().getUiContextID());
            generator.writeNumberField(APPLICATION.SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            generator.writeArrayFieldStart(APPLICATION.INSTRUCTIONS);
        } catch (final IOException e) {
            throw new RuntimeException("Cannot write instruction");
        }
    }

    @Override
    public void save(final Instruction instruction) {
        if (stacker != null) {
            stacker.add(instruction);
            return;
        }

        final JsonGenerator generator = generators.get();
        try {
            generator.writeObject(instruction);
        } catch (final IOException e) {
            throw new RuntimeException("Cannot write instruction");
        }
    }

    @Override
    public void flush() throws IOException {
        final JsonGenerator generator = generators.get();

        generator.writeEndArray();
        generator.writeEndObject();
        generator.close();
    }

    @Override
    public void setCurrentStacker(final List<Instruction> stacker) {
        this.stacker = stacker;
    }

    @Override
    public void removeCurrentStacker() {
        this.stacker = null;
    }

}
