
package com.ponysdk.jetty.test.bench;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONExchange extends ContentExchange {

    private static Logger log = LoggerFactory.getLogger(JSONExchange.class);
    private String sessionID = "";

    public JSONExchange(final String url) {
        setMethod("POST");
        setURL(url + "/ponyterminal/p");
        setRequestContentType("application/json;charset=UTF-8");
    }

    public void setContent(final JSONObject jso) {
        try {
            setRequestContent(new ByteArrayBuffer(jso.toString(), "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            log.error("Failed to set request content", e);
        }
    }

    @Override
    protected synchronized void onResponseHeader(final Buffer name, final Buffer value) throws IOException {
        super.onResponseHeader(name, value);
        if (name.toString().equals("Set-Cookie")) {
            final String cookies = value.toString();
            final String[] split = cookies.split(";");
            for (final String c : split) {
                if (c.startsWith("JSESSIONID")) {
                    sessionID = c;
                    break;
                }
            }
        }
    }

    public JSONObject waitResponse() {
        try {
            final int exchangeState = waitForDone();
            if (exchangeState != HttpExchange.STATUS_COMPLETED) { throw new RuntimeException("Failed to get response. Error code: " + exchangeState); }
            final String responseContent = getResponseContent();
            if (responseContent == null || responseContent.isEmpty()) return null;
            return new JSONObject(responseContent);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getSessionID() {
        return sessionID;
    }

}
