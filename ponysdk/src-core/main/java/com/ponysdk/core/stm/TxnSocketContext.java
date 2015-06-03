
package com.ponysdk.core.stm;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class TxnSocketContext implements TxnContext, TxnListener {

    private WebSocket socket;

    private List<Instruction> instructions = new ArrayList<Instruction>();

    private boolean polling = false;

    private boolean flushNow = false;

    public TxnSocketContext() {}

    public void setSocket(final WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void save(final Instruction instruction) {
        instructions.add(instruction);
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
    public List<Instruction> setCurrentStacker(final List<Instruction> stacker) {
        final List<Instruction> list = instructions;
        instructions = stacker;
        return list;
    }

    public void switchToPollingMode() {
        polling = true;
    }

    public void flushNow() {
        flushNow = true;
        Txn.get().addTnxListener(this);
    }

    @Override
    public void clear() {
        if (polling) return;

        instructions.clear();
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

    public int getInstructionsSize() {
        return instructions.size();
    }

}
