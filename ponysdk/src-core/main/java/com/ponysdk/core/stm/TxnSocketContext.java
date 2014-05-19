
package com.ponysdk.core.stm;

import java.io.IOException;
import java.util.List;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class TxnSocketContext implements TxnContext, TxnListener {

    private static final JsonFactory factory = new JsonFactory();

    private final ThreadLocal<JsonGenerator> generators = new ThreadLocal<JsonGenerator>();

    private List<Instruction> stacker;

    private WebSocket socket;

    private boolean polling = false;

    private boolean flushNow = false;

    public TxnSocketContext() {}

    public void setSocket(final WebSocket socket) {
        this.socket = socket;

        final JsonGenerator generator = generators.get();
        try {
            generator.writeStartObject();
            generator.writeNumberField(APPLICATION.SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            generator.writeArrayFieldStart(APPLICATION.INSTRUCTIONS);
        } catch (final IOException e) {
            throw new RuntimeException("Cannot write instruction");
        }

        // socket.send(data.toString());
        // } catch (final Throwable t) {
        // throw new RuntimeException("Cannot save instruction");
        // }
    }

    @Override
    public void init() {}

    @Override
    public void save(final Instruction instruction) {
        try {
            instructions.add(instruction);
            // if ((instructions.size() % 100) == 0) {
            // System.err.println("Flush forced");
            // flush();
            // }
        } catch (final Throwable t) {
            throw new RuntimeException("Cannot save instruction");
        }
    }

    @Override
    public void flush() throws Exception {
        if (polling) return;

        if (instructions.isEmpty()) return;
        final JSONObject data = new JSONObject();
        data.put(APPLICATION.INSTRUCTIONS, instructions);
        data.put(APPLICATION.SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
        socket.send(data.toString());
        instructions.clear();
    }

    @Override
    public void setCurrentStacker(final List<Instruction> stacker) {
        this.stacker = stacker;
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

        for (final Instruction instruction : instructions) {
            Txn.get().getTxnContext().save(instruction);
        }
        instructions.clear();
    }

    @Override
    public void beforeRollback() {}

    @Override
    public void afterFlush(final TxnContext txnContext) {}
}
