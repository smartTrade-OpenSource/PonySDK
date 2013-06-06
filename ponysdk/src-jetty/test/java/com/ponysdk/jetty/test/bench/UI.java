
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

import com.ponysdk.jetty.test.bench.mock.UIMock;
import com.ponysdk.jetty.test.bench.mock.UIMockScheduler;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.Dictionnary.WIDGETTYPE;
import com.ponysdk.ui.terminal.WidgetType;

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
    public Map<Long, UIMock> uiObjectByID = new HashMap<Long, UIMock>();
    public AtomicLong seqnum = new AtomicLong();
    public long viewID;

    protected List<Listener> listeners = new ArrayList<UI.Listener>();

    protected final Client client;

    public UI(final Client client) {
        this.client = client;
    }

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
            final String type = instruction.getString(TYPE.KEY);
            if (TYPE.KEY_.CREATE.equals(type)) {
                final WidgetType widgetType = WidgetType.values()[instruction.getInt(WIDGETTYPE.KEY)];
                if (WidgetType.SCHEDULER.equals(widgetType)) {
                    final Long pid = instruction.getLong(PROPERTY.OBJECT_ID);
                    uiObjectByID.put(pid, new UIMockScheduler(pid));
                }
            } else if (TYPE.KEY_.UPDATE.equals(type)) {
                final Long pid = instruction.getLong(PROPERTY.OBJECT_ID);
                final UIMock uiMock = uiObjectByID.get(pid);
                if (uiMock != null) uiMock.update(this, instruction);

                if (instruction.has(PROPERTY.ENSURE_DEBUG_ID)) {
                    final String did = instruction.getString(PROPERTY.ENSURE_DEBUG_ID);

                    final UI.Object o = new UI.Object();
                    o.did = did;
                    o.pid = pid;

                    objectByID.put(did, o);
                }
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

    public void sendToServer(final JSONObject i) throws Exception {
        client.pending.add(i);
        client.flush();
    }

}
