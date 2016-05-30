
package com.ponysdk.core.servlet;

import javax.servlet.http.HttpSession;

import com.ponysdk.core.stm.TxnContext;

public class PSession {

	private final HttpSession session;

	private TxnContext socketContext;

	public PSession(final HttpSession session) {
		this.session = session;
	}

	public HttpSession getHttpSession() {
		return session;
	}

	public String getId() {
		return session.getId();
	}

	public void setAttribute(final String name, final Object value) {
		session.setAttribute(name, value);
	}

	public Object getAttribute(final String name) {
		return session.getAttribute(name);
	}

	public void invalidate() {
		session.invalidate();
	}

	public void setSocketContext(final TxnContext context) {
		this.socketContext = context;
	}

	public TxnContext getSocketContext() {
		return socketContext;
	}

}
