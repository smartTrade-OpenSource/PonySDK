
package com.ponysdk.ui.terminal.request;

public abstract class RequestBuilder {

    protected final RequestCallback callback;

    public RequestBuilder(final RequestCallback callback) {
        this.callback = callback;
    }

    public abstract void send(String s);

    public abstract void sendHeartbeat();

}
