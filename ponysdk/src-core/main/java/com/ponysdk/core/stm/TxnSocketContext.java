
package com.ponysdk.core.stm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Instruction;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class TxnSocketContext implements TxnContext {

    private static final Logger log = LoggerFactory.getLogger(TxnSocketContext.class);

    private WebSocket socket;

    private List<Instruction> instructions = new ArrayList<Instruction>();

    private boolean polling = false;
    private final int maxIdleTime; // ms
    private long lastPoll = System.currentTimeMillis();

    private boolean flushNow = false;

    public TxnSocketContext(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setSocket(final WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void save(final Instruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public void flush() throws Exception {
        if (polling) {
            final long timeElapsed = System.currentTimeMillis() - lastPoll;
            if (timeElapsed > maxIdleTime) {
                log.error(TimeUnit.MILLISECONDS.toSeconds(timeElapsed) + " seconds elapsed since last poll. Closing session.");
                instructions.clear();
                PPusher.get().onClose();
                PPusher.get().getUiContext().getSession().invalidate();
                return;
            }
            if (!flushNow) return;
        }

        flushNow = false;
        lastPoll = System.currentTimeMillis();
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
    }

    @Override
    public void clear() {
        instructions.clear();
    }
}
