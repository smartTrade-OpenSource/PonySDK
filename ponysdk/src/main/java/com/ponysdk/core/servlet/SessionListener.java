
package com.ponysdk.core.servlet;

public interface SessionListener {

	void onSessionCreated(PSession session);

	void onSessionDestroyed(PSession session);

}
