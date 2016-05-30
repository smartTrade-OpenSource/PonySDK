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
import com.ponysdk.core.ParserImpl;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.servlet.PRequest;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.core.useragent.UserAgent;

public class TxnContext implements TxnListener {

	private static final Logger log = LoggerFactory.getLogger(TxnContext.class);

	private WebSocket socket;

	private boolean flushNow = false;

	private Parser parser;

	private Application application;

	private final Map<String, Object> parameters = new HashMap<>();

	private PRequest request;

	private UIContext uiContext;

	public void setSocket(final WebSocket socket) {
		this.socket = socket;
		this.parser = new ParserImpl(socket);
	}

	public void flush() {
		parser.reset();
	}

	public void flushNow() {
		flushNow = true;
		Txn.get().addTnxListener(this);
	}

	@Override
	public void beforeFlush(final TxnContext txnContext) {
		if (!flushNow)
			return;

		flushNow = false;

		Txn.get().flush();
	}

	@Override
	public void beforeRollback() {
	}

	@Override
	public void afterFlush(final TxnContext txnContext) {
	}

	public Parser getParser() {
		return parser;
	}

	public void setRequest(final PRequest request) {
		this.request = request;
	}

	public JsonObject getJsonObject() {
		try {
			return Json.createReader(request.getReader()).readObject();
		} catch (final IOException e) {
			log.error("Cannot build reader from HTTP request", e);
		}

		return null;
	}

	public UserAgent getUserAgent() {
		return null;
	}

	public String getRemoteAddr() {
		return null;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(final Application application) {
		this.application = application;
	}

	public void setAttribute(final String name, final Object value) {
		parameters.put(name, value);
	}

	public Object getAttribute(final String name) {
		return parameters.get(name);
	}

	public int getSeqNum() {
		return 0;
	}

	public String getHistoryToken() {
		return null;
	}

	public UIContext getUIContext() {
		return uiContext;
	}

	public void setUIContext(final UIContext uiContext) {
		this.uiContext = uiContext;
	}

	public void sendHeartBeat() {
		socket.sendHeartBeat();
	}

}
