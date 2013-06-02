
package com.ponysdk.jetty.test.bench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;

public class UI {

    public static class Object {

        public long pid;
        public String did;

        @Override
        public String toString() {
            return "UI.Object [did=" + did + ", pid=" + pid + "]";
        }
    }

    public static interface Listener {

        public void onMessage(JSONObject message);
    }

    private static Logger log = LoggerFactory.getLogger(UI.class);

    private long lastReceivedSeqNum = -1;
    public Map<String, UI.Object> objectByID = new HashMap<String, UI.Object>();
    public AtomicLong seqnum = new AtomicLong();
    public long viewID;

    protected List<Listener> listeners = new ArrayList<UI.Listener>();

    public void init(final JSONObject response) throws Exception {

        viewID = response.getLong(APPLICATION.VIEW_ID);

        update(response);

    }

    public void update(final JSONObject response) throws Exception {
        final long receivedSeqNum = response.getLong(APPLICATION.SEQ_NUM);
        if (lastReceivedSeqNum + 1 != receivedSeqNum) {
            log.warn("Desynchronized seqnum. Expected: " + (lastReceivedSeqNum + 1) + ". Received: " + receivedSeqNum + ".");
        }
        lastReceivedSeqNum = receivedSeqNum;

        fireOnMessage(response);

        final JSONArray instructions = response.getJSONArray(APPLICATION.INSTRUCTIONS);
        for (int i = 0; i < instructions.length(); i++) {
            final JSONObject instruction = instructions.getJSONObject(i);
            if (instruction.has(PROPERTY.ENSURE_DEBUG_ID)) {
                final String did = instruction.getString(PROPERTY.ENSURE_DEBUG_ID);
                final long pid = instruction.getLong(PROPERTY.OBJECT_ID);

                final UI.Object o = new UI.Object();
                o.did = did;
                o.pid = pid;

                objectByID.put(did, o);
            }
        }
    }

    private void fireOnMessage(final JSONObject response) {
        for (final Listener l : listeners) {
            l.onMessage(response);
        }
    }

    public void addListener(final Listener listener) {
        listeners.add(listener);
    }
}
