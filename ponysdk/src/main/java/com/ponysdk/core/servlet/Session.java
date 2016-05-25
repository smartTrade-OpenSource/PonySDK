
package com.ponysdk.core.servlet;

import com.ponysdk.core.stm.TxnSocketContext;

public interface Session {

	public String getId();

	public void setAttribute(String name, Object value);

	public Object getAttribute(String name);

	public void invalidate();

	public void setSocketContext(TxnSocketContext context);

	public TxnSocketContext getSocketContext();

}
