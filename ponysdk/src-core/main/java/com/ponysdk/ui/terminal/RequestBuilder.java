
package com.ponysdk.ui.terminal;

public abstract class RequestBuilder {

    protected final RequestCallback callback;

    public RequestBuilder(final RequestCallback callback) {
        this.callback = callback;
    }

    public abstract void send(String s);

}
