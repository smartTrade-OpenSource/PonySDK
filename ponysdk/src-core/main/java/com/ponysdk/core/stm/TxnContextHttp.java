
package com.ponysdk.core.stm;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class TxnContextHttp implements TxnContext {

    private final Response response;
    private List<Instruction> instructions = new ArrayList<Instruction>();
    private final boolean startMode;

    public TxnContextHttp(final boolean startMode, final Request request, final Response response) {
        this.response = response;
        this.startMode = startMode;
    }

    @Override
    public void save(final Instruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public void flush() throws Exception {
        if (instructions.isEmpty()) return;
        final JSONObject data = new JSONObject();
        if (startMode) data.put(APPLICATION.VIEW_ID, UIContext.get().getUiContextID());
        data.put(APPLICATION.INSTRUCTIONS, instructions);
        data.put(APPLICATION.SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
        response.write(data.toString());
        response.flush();
        instructions.clear();
    }

    @Override
    public List<Instruction> setCurrentStacker(final List<Instruction> stacker) {
        final List<Instruction> list = instructions;
        instructions = stacker;
        return list;
    }

    @Override
    public void clear() {
        instructions.clear();
    }
}
