/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.stm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.Parser;
import com.ponysdk.core.Parser2;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.Request;
import com.ponysdk.core.servlet.Response;
import com.ponysdk.core.useragent.UserAgent;

/**
 * @deprecated Useless
 */
@Deprecated
public class TxnContextHttp implements TxnContext {

    private static final Logger log = LoggerFactory.getLogger(TxnContextHttp.class);

    private Request request;
    private Response response;

    private Parser2 parser;

    private UserAgent userAgent;

    private Application application;

    private final Map<String, Object> parameters = new HashMap<>();

    private UIContext uiContext;

    @Override
    public void flush() {
        response.flush();
        request = null;
        response = null;
    }

    @Override
    public Parser getParser() {
        return parser;
    }

    @Override
    public JsonObject getJsonObject() {
        try {
            return Json.createReader(request.getReader()).readObject();
        } catch (final IOException e) {
            log.error("Cannot build reader from HTTP request", e);
        }

        return null;
    }

    @Override
    public void setRequest(final Request request) {
        this.request = request;
        this.userAgent = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
    }

    @Override
    public void setResponse(final Response response) {
        this.response = response;
        try {
            this.parser = new Parser2(response.getWriter());
        } catch (final IOException e) {
            log.error("Cannot initialize Parser", e);
        }
    }

    @Override
    public UserAgent getUserAgent() {
        return userAgent;
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public void setApplication(final Application application) {
        this.application = application;
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        parameters.put(name, value);
    }

    @Override
    public Object getAttribute(final String name) {
        return parameters.get(name);
    }

    @Override
    public int getSeqNum() {
        return 0;
    }

    @Override
    public String getHistoryToken() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UIContext getUIContext() {
        return uiContext;
    }

    @Override
    public void setUIContext(final UIContext uiContext) {
        this.uiContext = uiContext;
    }

    @Override
    public void sendHeartBeat() {
        // TODO Auto-generated method stub

    }

}
