/*============================================================================
 *
 * Copyright (c) 2000-2008 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.core.driver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PonyExchange extends ContentExchange {

    private static Logger log = LoggerFactory.getLogger(PonyExchange.class);
    private String sessionID = "";

    public PonyExchange(final String url) {
        setMethod("POST");
        if (url.endsWith("/")) {
            setURL(url + "p");
        } else {
            setURL(url + "/p");
        }
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

    public String waitResponse() {
        try {
            final int exchangeState = waitForDone();
            if (exchangeState != HttpExchange.STATUS_COMPLETED) { throw new RuntimeException("Failed to get response. Error code: " + exchangeState); }
            final String responseContent = getResponseContent();
            if (responseContent == null || responseContent.isEmpty()) return null;
            return responseContent;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getSessionID() {
        return sessionID;
    }

}
