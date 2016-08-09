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

package com.ponysdk.core.terminal.socket;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.request.WebSocketRequestBuilder;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.ArrayBuffer;
import elemental.html.WebSocket;
import elemental.html.Window;

public class WebSocketClient implements MessageSender {

	private static final Logger log = Logger.getLogger(WebSocketClient.class
			.getName());

	private final WebSocket webSocket;
	private final UIBuilder uiBuilder;

	public WebSocketClient(final String url, final UIBuilder uiBuilder,
			final WebSocketDataType webSocketDataType) {
		this.uiBuilder = uiBuilder;

		final Window window = Browser.getWindow();
		webSocket = window.newWebSocket(url);
		webSocket.setBinaryType(webSocketDataType.getName());

		final MessageReader messageReader;
		if (WebSocketDataType.ARRAYBUFFER.equals(webSocketDataType)) {
			messageReader = new ArrayBufferReader(this);
		} else if (WebSocketDataType.BLOB.equals(webSocketDataType)) {
			messageReader = new BlobReader(this);
		} else {
			throw new IllegalArgumentException("Wrong reader type : "
					+ webSocketDataType);
		}

		webSocket.setOnopen(new EventListener() {

			@Override
			public void handleEvent(final Event event) {
				if (log.isLoggable(Level.INFO))
					log.info("WebSoket connected");
			}
		});
		webSocket.setOnclose(new EventListener() {

			@Override
			public void handleEvent(final Event event) {
				if (log.isLoggable(Level.INFO))
					log.info("WebSoket disconnected");
				uiBuilder.onCommunicationError(new Exception(
						"Websocket connection lost."));
			}
		});
		// webSocket.setOnerror(this);
		webSocket.setOnmessage(new EventListener() {

			/**
			 * Message from server to Main terminal
			 */
			@Override
			public void handleEvent(final Event event) {
				messageReader.read((MessageEvent) event);
			}
		});
	}

	@Override
	public void read(final ArrayBuffer arrayBuffer) {
		try {
			final ReaderBuffer buffer = new ReaderBuffer(arrayBuffer);
			// Get the first element on the message, always a key of element of
			// the Model enum
			final BinaryModel type = buffer.readBinaryModel();

			if (type.getModel() == ServerToClientModel.HEARTBEAT) {
				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "Heart beat");
				send(ClientToServerModel.HEARTBEAT.toStringValue());
			} else if (type.getModel() == ServerToClientModel.UI_CONTEXT_ID) {
				PonySDK.uiContextId = type.getIntValue();
				uiBuilder
				.init(new WebSocketRequestBuilder(WebSocketClient.this));
			} else if (type.getModel() == ServerToClientModel.BEGIN_OBJECT) {
				try {
					uiBuilder.updateMainTerminal(buffer);
				} catch (final Exception e) {
					log.log(Level.SEVERE, "Error while processing the "
							+ buffer, e);
				}
			} else {
				log.severe("Unknown model : " + type.getModel());
			}
		} catch (final Exception e) {
			log.log(Level.SEVERE, "Cannot parse " + arrayBuffer, e);
		}
	}

	public void send(final String message) {
		webSocket.send(message);
	}

	public void close() {
		webSocket.close();
	}

	public enum WebSocketDataType {

		ARRAYBUFFER("arraybuffer"), BLOB("blob");

		private String name;

		private WebSocketDataType(final String name) {
			this.name = name;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return getName();
		}
	}

}
