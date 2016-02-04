
package com.ponysdk.core.driver;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.DomHandlerType;

public class PonyDriver {

    static Logger log = LoggerFactory.getLogger(PonyDriver.class);

    protected String driver_url;
    private long lastReceivedSeqNum = -1;
    private boolean useSocket;

    protected final HttpClient httpClient;
    protected String jsessionID;
    protected WebSocketClient websocketClient;
    protected WebSocket.Connection websocketConnection;

    protected Map<Long, JSONObject> elements = new HashMap<Long, JSONObject>();
    protected Map<Long, List<String>> itemList = new HashMap<Long, List<String>>();
    protected Map<String, Long> elementsByDebugID = new HashMap<String, Long>();
    protected List<JSONObject> responceStack = new ArrayList<JSONObject>();
    public AtomicLong seqnum = new AtomicLong();
    public AtomicLong totalBytesReceived = new AtomicLong();
    public long viewID;

    private WebSocketClientFactory factory;

    public PonyDriver() throws Exception {
        this(600000, true);
    }

    public PonyDriver(final boolean usesocket) throws Exception {
        this(600000, usesocket);
    }

    public PonyDriver(final long sessiontimeout) throws Exception {
        this(sessiontimeout, true);
    }

    public PonyDriver(final long sessiontimeout, final boolean usesocket) throws Exception {
        this.httpClient = new HttpClient();
        this.httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        this.httpClient.setTimeout(sessiontimeout);
        this.useSocket = usesocket;
    }

    public boolean isUseSocket() {
        return useSocket;
    }

    public void setUseSocket(final boolean useSocket) {
        this.useSocket = useSocket;
    }

    public void get(final String url) {
        this.driver_url = url;
        startSession();
    }

    public void startSession() {
        try {
            startserver();
        } catch (final Exception e) {
            throw new RuntimeException("Error when starting the application ", e);
        }

        // don't use webscoket
        if (!useSocket) {
            log.error("This application doesn't use the webSocket protocol");
            return;
        }

        try {
            startWebsocket();
        } catch (final Exception e) {
            throw new RuntimeException("Error when starting the websocket application ", e);
        }
    }

    public void close() {
        try {
            stopSession();
            log.info("Application closed " + jsessionID + " : " + totalBytesReceived.get());
        } catch (final Exception e) {
            log.error("This application doesn't close correctly ");
        }
    }

    public void awaitAllReceived() {
        awaitAllReceived(5000);
    }

    public void awaitAllReceived(final long time) {
        final long await = System.currentTimeMillis();
        while (((System.currentTimeMillis() - await) < time) && (lastReceivedSeqNum < seqnum.get())) {
            try {
                Thread.sleep(1L);
            } catch (final InterruptedException e) {}
        }
    }

    // check if the sequence of response received is correct
    public synchronized void checkUpdate(final JSONObject response) throws JSONException {
        final long receivedSeqNum = response.getLong(APPLICATION.SEQ_NUM);

        if ((lastReceivedSeqNum + 1) != receivedSeqNum) {
            log.warn(" Added response in stack of message received #" + response);
            lastReceivedSeqNum++;
            responceStack.add(response);
        } else {
            lastReceivedSeqNum++;
            responceStack.add(response);
            for (int i = 0; i < responceStack.size(); i++) {
                update(responceStack.get(i));
            }
            responceStack.clear();
        }
    }

    public long getObjectIDByID(final String id) {
        Long objId = null;
        if (id.startsWith("gwt-debug-")) {
            objId = elementsByDebugID.get(id.substring(10));
        }
        // else {
        // throw new JSONException(" Json id not supported for now");
        // }
        if (objId == null) return -1;
        return objId;
    }

    public long getElementByCSS(final String cssSelector) {
        try {

            final String[] selector = cssSelector.split("\\.");
            JSONObject jsonObject = null;
            final int length = selector.length;
            // values
            if (length > 1) {
                // css contains values
                if (selector[length - 1].contains(":contains(")) {
                    final String[] contains = selector[length - 1].split(":");
                    // must be only two
                    if (contains.length == 2) {
                        final String ContainsValue = contains[1].substring("contains('".length(), contains[1].length() - 2);
                        selector[length - 1] = contains[0];
                        final List<JSONObject> parseCSS = parseCSS(selector);
                        // research of text value
                        int i = 0;

                        while (i < parseCSS.size()) {
                            if (parseCSS.get(i).has(PROPERTY.TEXT) && (parseCSS.get(i).getString(PROPERTY.TEXT).compareTo(ContainsValue) == 0)) {
                                jsonObject = parseCSS.get(i);
                                // stop research
                                break;
                            }
                            i++;
                        }

                    } else {
                        // error
                    }
                } else {
                    final List<JSONObject> parseCSS = parseCSS(selector);
                    if (!parseCSS.isEmpty()) jsonObject = parseCSS.get(0);
                }

            }
            // return object ID
            if (jsonObject != null) {
                return jsonObject.getLong(PROPERTY.OBJECT_ID);
            } else {
                return -1;
                // throw new JSONException("Json object with css selector : " + cssSelector + " not found");
            }
        } catch (final Exception e) {
            return -1;
        }
    }

    public List<JSONObject> parseCSS(final String[] selector) throws Exception {
        final List<JSONObject> csslist = new ArrayList<JSONObject>();
        JSONObject jsonObject;
        if (selector[0].trim().isEmpty()) {

            final Collection<JSONObject> values = elements.values();
            final Iterator<JSONObject> iterator = values.iterator();

            while (iterator.hasNext()) {
                jsonObject = iterator.next();

                if (jsonObject.has(PROPERTY.ADD_STYLE_NAME)) {
                    final Object object = jsonObject.get(PROPERTY.ADD_STYLE_NAME);
                    if (containsStyle(object, selector)) {
                        csslist.add(jsonObject);
                    }

                }
            }
        } else {
            final List<JSONObject> tags = getElementsByTagName(selector[0]);
            int i = 0;

            while ((i < tags.size())) {
                jsonObject = tags.get(i);

                if (jsonObject.has(PROPERTY.ADD_STYLE_NAME)) {
                    final Object object = jsonObject.get(PROPERTY.ADD_STYLE_NAME);
                    if (containsStyle(object, selector)) {
                        csslist.add(jsonObject);
                    }
                }
                i++;
            }

        }
        return csslist;
    }

    public List<JSONObject> getElementsByTagName(final String tagName) {
        final List<JSONObject> tags = new ArrayList<JSONObject>();
        for (final Entry<Long, JSONObject> element : elements.entrySet()) {
            try {
                final JSONObject value = element.getValue();
                if (value.has(PROPERTY.TAG) && value.getString(PROPERTY.TAG).equalsIgnoreCase(tagName)) {
                    tags.add(value);
                }
            } catch (final Exception e) {}
        }
        return tags;
    }

    private boolean containsStyle(final Object object, final String[] selector) throws JSONException {
        int foundCount = 1;
        // begin at 1 because is the tag name, we don't must use it for search a style
        for (int i = 1; i < selector.length; i++) {
            if (object instanceof JSONArray) {
                if (containsElement(((JSONArray) object), selector[i])) {
                    foundCount++;
                }
            } else {
                if (selector[i].compareTo((String) object) == 0) {
                    foundCount++;
                }
            }
        }
        return (foundCount == selector.length);
    }

    private boolean containsElement(final JSONArray array, final String element) throws JSONException {

        for (int i = 0; i < array.length(); i++) {
            if (array.getString(i).compareTo(element) == 0) return true;
        }
        return false;
    }

    public String getText(final long objectID) throws Exception {
        try {
            final JSONObject json = elements.get(objectID);
            if (json.has(PROPERTY.TEXT)) {
                return json.getString(PROPERTY.TEXT);
            } else if (json.has(PROPERTY.INNER_HTML)) { return json.getString(PROPERTY.INNER_HTML); }
            return null;
        } catch (final Exception e) {
            throw new Exception("unable to find the text of Json object with the id : " + objectID, e);
        }
    }

    public String getValue(final long objectID) throws Exception {
        try {
            final JSONObject json = elements.get(objectID);
            return json.getString(PROPERTY.VALUE);
        } catch (final Exception e) {
            throw new Exception("unable to find the value of Json object with the id : " + objectID, e);
        }
    }

    public String getSelectedValue(final long objectID) throws Exception {
        try {
            final JSONObject jsonObject = elements.get(objectID);
            final List<String> list = itemList.get(objectID);
            return list.get(jsonObject.getInt(PROPERTY.VALUE));
        } catch (final Exception e) {
            throw new Exception("unable to find the selected value of Json object with the id : " + objectID, e);
        }
    }

    /**
     * send a json instruction on http client protocol
     *
     * @param instruction
     *            json instruction
     * @throws JSONException
     * @throws IOException
     * @throws Exception
     */
    private void sendInstruction(final JSONObject instruction) throws JSONException {

        final JSONObject update = new JSONObject();
        update.put(APPLICATION.VIEW_ID, viewID);
        update.put(APPLICATION.INSTRUCTIONS, new JSONArray().put(instruction));
        update.put(APPLICATION.ERRORS, new JSONArray());

        update.put(APPLICATION.SEQ_NUM, seqnum.getAndIncrement());

        final PonyExchange exchange = new PonyExchange(driver_url);
        exchange.setContent(update);
        exchange.addRequestHeader("Cookie", jsessionID);

        try {
            httpClient.send(exchange);
        } catch (final IOException e) {
            throw new JSONException(e.getMessage());
        }

        final String responseValue = exchange.waitResponse();
        if (responseValue == null) return;
        // add bytes received
        totalBytesReceived.addAndGet(responseValue.getBytes().length);

        final JSONObject response = new JSONObject(responseValue);
        checkUpdate(response);
    }

    public void type(final long objectID, final String value) throws Exception {
        try {
            final JSONObject instruction = new JSONObject();
            instruction.put(PROPERTY.OBJECT_ID, elements.get(objectID).getLong(PROPERTY.OBJECT_ID));
            instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            instruction.put(HANDLER.KEY, HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER);
            instruction.put(PROPERTY.VALUE, value);
            sendInstruction(instruction);
        } catch (final Exception e) {
            throw new Exception("unable to send typing action for Json object with the id : " + objectID, e);
        }
    }

    public void click(final long objectID) throws Exception {
        try {
            final JSONObject instruction = new JSONObject();
            instruction.put(HANDLER.KEY, elements.get(objectID).getString(HANDLER.KEY));
            instruction.put(PROPERTY.OBJECT_ID, elements.get(objectID).getLong(PROPERTY.OBJECT_ID));
            instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            instruction.put(PROPERTY.DOM_HANDLER_TYPE, DomHandlerType.CLICK.ordinal());
            instruction.put(PROPERTY.X, 0);
            instruction.put(PROPERTY.Y, 0);
            instruction.put(PROPERTY.CLIENT_X, 0);
            instruction.put(PROPERTY.CLIENT_Y, 0);
            instruction.put(PROPERTY.NATIVE_BUTTON, 0);
            instruction.put(PROPERTY.SOURCE_ABSOLUTE_LEFT, 0d);
            instruction.put(PROPERTY.SOURCE_ABSOLUTE_TOP, 0d);
            instruction.put(PROPERTY.SOURCE_OFFSET_HEIGHT, 0d);
            instruction.put(PROPERTY.SOURCE_OFFSET_WIDTH, 0d);
            sendInstruction(instruction);
        } catch (final Exception e) {
            throw new Exception("unable to send click action for Json object with the id : " + objectID, e);
        }
    }

    public void doubleClick(final long objectID) throws Exception {
        try {
            final JSONObject instruction = new JSONObject();
            instruction.put(HANDLER.KEY, elements.get(objectID).getString(HANDLER.KEY));
            instruction.put(PROPERTY.OBJECT_ID, elements.get(objectID).getLong(PROPERTY.OBJECT_ID));
            instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            instruction.put(PROPERTY.DOM_HANDLER_TYPE, DomHandlerType.DOUBLE_CLICK.ordinal());
            instruction.put(PROPERTY.X, 0);
            instruction.put(PROPERTY.Y, 0);
            instruction.put(PROPERTY.CLIENT_X, 0);
            instruction.put(PROPERTY.CLIENT_Y, 0);
            instruction.put(PROPERTY.NATIVE_BUTTON, 0);
            instruction.put(PROPERTY.SOURCE_ABSOLUTE_LEFT, 0d);
            instruction.put(PROPERTY.SOURCE_ABSOLUTE_TOP, 0d);
            instruction.put(PROPERTY.SOURCE_OFFSET_HEIGHT, 0d);
            instruction.put(PROPERTY.SOURCE_OFFSET_WIDTH, 0d);
            sendInstruction(instruction);
        } catch (final Exception e) {
            throw new Exception("unable to send click action for Json object with the id : " + objectID, e);
        }
    }

    public void select(final long objectID, final String value) throws Exception {
        try {
            final JSONObject instruction = new JSONObject();
            instruction.put(PROPERTY.OBJECT_ID, elements.get(objectID).getLong(PROPERTY.OBJECT_ID));
            instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            instruction.put(HANDLER.KEY, HANDLER.KEY_.CHANGE_HANDLER);
            instruction.put(PROPERTY.VALUE, itemList.get(objectID).indexOf(value));
            sendInstruction(instruction);
        } catch (final Exception e) {
            throw new Exception("unable to send selected action for Json object with the id : " + objectID, e);
        }
    }

    private void startserver() throws Exception {
        this.httpClient.start();
        this.lastReceivedSeqNum = -1;
        this.seqnum.set(0);
        final PonyExchange exchange = new PonyExchange(driver_url);

        final JSONObject init = new JSONObject();
        init.put(APPLICATION.KEY, APPLICATION.KEY_.START);
        init.put(APPLICATION.SEQ_NUM, seqnum.getAndIncrement());
        init.put(HISTORY.TOKEN, "");
        init.put(PROPERTY.COOKIES, new JSONArray());
        exchange.setContent(init);
        httpClient.send(exchange);
        final String responseValue = exchange.waitResponse();
        // add bytes received
        totalBytesReceived.addAndGet(responseValue.getBytes().length);
        final JSONObject response = new JSONObject(responseValue);
        jsessionID = exchange.getSessionID();
        viewID = response.getLong(APPLICATION.VIEW_ID);
        log.info("Connected to #" + driver_url + " Application ViewID#" + viewID + " SessionID#" + jsessionID);
        checkUpdate(response);
    }

    private void startWebsocket() throws Exception {

        factory = new WebSocketClientFactory();
        factory.start();

        final String sessionID = jsessionID.split("=")[1];
        String ws = "";

        websocketClient = factory.newWebSocketClient();
        websocketClient.getCookies().put("JSESSIONID", sessionID);
        websocketClient.setMaxTextMessageSize(Integer.MAX_VALUE);
        websocketClient.setMaxBinaryMessageSize(Integer.MAX_VALUE);

        if (driver_url.endsWith("/")) {
            ws = "ws";
        } else {
            ws = "/ws";
        }
        final URI uri = new URI(driver_url.replaceFirst("http", "ws") + ws + "?" + APPLICATION.VIEW_ID + "=" + viewID);

        websocketConnection = websocketClient.open(uri, new WebSocket.OnTextMessage() {

            @Override
            public void onOpen(final Connection connection) {
                log.info("Wesocket connection succeeded");
                scheduleHeartbeat();
            }

            @Override
            public void onClose(final int closeCode, final String message) {
                log.info("Wesocket connection lost");
            }

            @Override
            public void onMessage(final String data) {
                try {
                    if (data == null || data.isEmpty()) return;
                    // add bytes received
                    totalBytesReceived.addAndGet(data.getBytes().length);
                    final JSONObject jsonObject = new JSONObject(data);
                    checkUpdate(jsonObject);
                } catch (final Exception e) {
                    log.error("Failed to process websocket message #" + data, e);
                }
            }
        }).get(1, TimeUnit.SECONDS);
    }

    protected void scheduleHeartbeat() {
        new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        sleep(2000);
                        final int timeStamp = (int) (new Date().getTime() * .001);
                        final JSONObject jso = new JSONObject();
                        jso.put(Dictionnary.APPLICATION.PING, timeStamp);
                        jso.put(Dictionnary.APPLICATION.VIEW_ID, viewID);
                        sendInstruction(jso);
                    } catch (final Throwable throwable) {
                        log.error("Cannot send ping : ", throwable);
                    }
                }
            };
        }.start();
    }

    private void stopSession() throws Exception {
        if (websocketConnection != null) websocketConnection.close();
        if (factory != null) factory.stop();
        httpClient.stop();
        elements.clear();
        elementsByDebugID.clear();
        itemList.clear();
    }

    private void removeList(final List<Long> removeList) throws JSONException {
        for (int i = 0; i < removeList.size(); i++) {
            final JSONObject element = elements.remove(removeList.get(i));
            if (element != null) {
                if (element.has(PROPERTY.ENSURE_DEBUG_ID)) {
                    elementsByDebugID.remove(element.getString(PROPERTY.ENSURE_DEBUG_ID));
                }
            }
        }
    }

    private List<Long> buildRemoveList(final JSONObject instruction) {
        final List<Long> excepted = new ArrayList<Long>();
        buildRemoveList(instruction, excepted);
        return excepted;
    }

    private void buildRemoveList(final JSONObject instruction, final List<Long> excepted) {
        try {
            final Long objectID = instruction.getLong(PROPERTY.OBJECT_ID);

            if (excepted.contains(objectID)) return;

            excepted.add(objectID);
            for (final Entry<Long, JSONObject> element : elements.entrySet()) {
                final JSONObject jsonObject = element.getValue();

                if (jsonObject.has(PROPERTY.PARENT_ID) && (jsonObject.getLong(PROPERTY.PARENT_ID) == objectID)) {
                    buildRemoveList(jsonObject, excepted);
                }
            }

        } catch (final Exception e) {
            log.error("Error when build the revome list with element#" + instruction);
        }
    }

    private void cleanByDebugID() {
        try {
            for (final Entry<Long, JSONObject> jEntry : elements.entrySet()) {
                final Long key = jEntry.getKey();
                final JSONObject jsonObject = jEntry.getValue();
                if (jsonObject.has(PROPERTY.ENSURE_DEBUG_ID)) {
                    final String oldID = jsonObject.getString(PROPERTY.ENSURE_DEBUG_ID);

                    final Long objID = elementsByDebugID.get(oldID);
                    if (objID == null) {
                        elementsByDebugID.put(oldID, key);
                    } else {
                        if (key > objID) {
                            elementsByDebugID.put(oldID, key);
                        }
                    }
                }
            }
            // Clear the elements who doesn't have the same object ID that the elements in elementsBydegugID
            final java.lang.Object[] keys = elements.keySet().toArray();
            for (int i = 0; i < keys.length; i++) {
                final JSONObject jObject = elements.get(keys[i]);
                if (jObject.has(PROPERTY.ENSURE_DEBUG_ID)) {
                    if (elementsByDebugID.get(jObject.getString(PROPERTY.ENSURE_DEBUG_ID)) != jObject.getLong(PROPERTY.OBJECT_ID)) {
                        elements.remove(keys[i]);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Error when clean element by ensure debug ID");
        }
    }

    private void addJson(final JSONObject instruction) {
        try {
            final long key = instruction.getLong(PROPERTY.OBJECT_ID);
            JSONObject object = elements.get(key);
            if (object == null) object = new JSONObject();
            @SuppressWarnings("unchecked")
            final Iterator<String> keys = instruction.keys();
            // set the item list for all select tag
            if (object.has(PROPERTY.MULTISELECT) && instruction.has(PROPERTY.INDEX)) {
                final long objID = key;
                List<String> items = itemList.get(objID);

                if (instruction.getLong(PROPERTY.INDEX) == 0) {
                    items = new ArrayList<String>();
                    while (keys.hasNext()) {
                        final String id = keys.next();
                        java.lang.Object obj;
                        obj = instruction.get(id);

                        if (!(obj instanceof JSONArray)) {
                            if (id.compareTo("X") == 0) {
                                object.accumulate(id, obj);
                            } else {
                                object.put(id, obj);
                            }
                        }
                    }
                }
                final String itemText = instruction.getString(PROPERTY.ITEM_TEXT);
                if (itemText != null) {
                    items.add(instruction.getInt(PROPERTY.INDEX), itemText);
                }
                itemList.put(objID, items);

            } else {
                while (keys.hasNext()) {

                    final String id = keys.next();
                    java.lang.Object obj;
                    obj = instruction.get(id);

                    if (!(obj instanceof JSONArray)) {
                        if (id.compareTo("X") == 0) {
                            object.accumulate(id, obj);
                        } else {
                            object.put(id, obj);
                        }
                    }

                }
            }
            elements.put(key, object);
        } catch (final Exception e) {
            log.error("unable to add element#" + instruction);
        }
    }

    private void update(final JSONObject response) throws JSONException {
        final JSONArray instructions = response.getJSONArray(APPLICATION.INSTRUCTIONS);
        for (int i = 0; i < instructions.length(); i++) {
            final JSONObject instruction = instructions.getJSONObject(i);
            final String type = instruction.getString(TYPE.KEY);

            if (TYPE.KEY_.CREATE.equals(type)) {
                addJson(instruction);

            } else if (TYPE.KEY_.UPDATE.equals(type)) {
                if (instruction.has(PROPERTY.POPUP_HIDE) && !instruction.getBoolean(PROPERTY.POPUP_HIDE)) {
                    final List<Long> removeList = buildRemoveList(instruction);
                    removeList(removeList);
                } else addJson(instruction);

            } else if (TYPE.KEY_.GC.equals(type)) {
                final List<Long> removeList = buildRemoveList(instruction);
                removeList(removeList);

            } else if (TYPE.KEY_.REMOVE.equals(type)) {
                final List<Long> removeList = buildRemoveList(instruction);
                removeList(removeList);

            } else if (TYPE.KEY_.ADD_HANDLER.equals(type)) {
                addJson(instruction);

            } else if (TYPE.KEY_.ADD.equals(type)) {
                addJson(instruction);

            }

        }
        cleanByDebugID();
    }
}
