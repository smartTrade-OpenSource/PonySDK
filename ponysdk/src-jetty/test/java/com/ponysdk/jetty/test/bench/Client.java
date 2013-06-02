
package com.ponysdk.jetty.test.bench;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.jetty.test.bench.action.Action;
import com.ponysdk.jetty.test.bench.action.ClickAction;
import com.ponysdk.jetty.test.bench.action.SetTextAction;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;
import com.ponysdk.ui.terminal.Dictionnary.HISTORY;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;

public class Client {

    private static Logger log = LoggerFactory.getLogger(Client.class);

    protected final HttpClient httpClient;
    protected final String url;

    protected List<JSONObject> pending = new ArrayList<JSONObject>();
    protected UI ui;
    protected String jsessionID;
    protected WebSocketClient websocketClient;
    protected WebSocket.Connection websocketConnection;

    public Client(final String url) throws Exception {
        this.url = url;

        this.httpClient = new HttpClient();
        this.httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        this.httpClient.setTimeout(10000);
        this.httpClient.start();

        this.ui = new UI();
    }

    public void start() throws Exception {

        final JSONExchange exchange = new JSONExchange(url);

        final JSONObject init = new JSONObject();
        init.put(APPLICATION.KEY, APPLICATION.KEY_.START);
        init.put(APPLICATION.SEQ_NUM, ui.seqnum.getAndIncrement());
        init.put(HISTORY.TOKEN, "");
        init.put(PROPERTY.COOKIES, new JSONArray());
        exchange.setContent(init);

        httpClient.send(exchange);

        final JSONObject response = exchange.waitResponse();
        jsessionID = exchange.getSessionID();

        log.info("Connected to #" + url);

        ui.init(response);
    }

    public void startWebsocket() throws Exception {
        if (ui == null) throw new IllegalAccessError("start() must be called before");

        final WebSocketClientFactory factory = new WebSocketClientFactory();
        factory.start();

        final String sessionID = jsessionID.split("=")[1];

        websocketClient = factory.newWebSocketClient();
        websocketClient.getCookies().put("JSESSIONID", sessionID);

        final URI uri = new URI(url.replaceFirst("http", "ws") + "/ws" + "?" + APPLICATION.VIEW_ID + "=" + ui.viewID);
        websocketConnection = websocketClient.open(uri, new WebSocket.OnTextMessage() {

            @Override
            public void onOpen(final Connection connection) {
                log.info("Wesocket connection succeeded");
            }

            @Override
            public void onClose(final int closeCode, final String message) {
                log.info("Wesocket connection lost");
            }

            @Override
            public void onMessage(final String data) {
                try {
                    if (data == null || data.isEmpty()) return;
                    final JSONObject jsonObject = new JSONObject(data);
                    ui.update(jsonObject);
                } catch (final Exception e) {
                    log.error("Failed to process websocket message #" + data, e);
                }
            }
        }).get(5, TimeUnit.SECONDS);
    }

    public void add(final Action action) throws Exception {
        action.setUI(ui);
        pending.add(action.asJSON());
    }

    public void flush() throws Exception {
        final JSONObject update = new JSONObject();
        update.put(APPLICATION.VIEW_ID, ui.viewID);
        update.put(APPLICATION.INSTRUCTIONS, new JSONArray(pending));
        update.put(APPLICATION.ERRORS, new JSONArray());
        update.put(APPLICATION.SEQ_NUM, ui.seqnum.getAndIncrement());

        final JSONExchange exchange = new JSONExchange(url);
        exchange.setContent(update);
        exchange.addRequestHeader("Cookie", jsessionID);

        httpClient.send(exchange);

        final JSONObject response = exchange.waitResponse();
        if (response == null) return;

        ui.update(response);
    }

    public UI getUi() {
        return ui;
    }

    public static void main(final String[] args) {
        try {
            final Client client = new Client("http://localhost:8081/trading");
            client.start();

            client.startWebsocket();

            client.add(new SetTextAction("login", "toto"));
            client.add(new SetTextAction("password", "totopwd"));
            client.add(new ClickAction("signin"));
            client.flush();

        } catch (final Exception e) {
            log.error("", e);
        }
    }

}
