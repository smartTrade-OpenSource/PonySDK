
package com.ponysdk.ui.terminal.socket;

public class WebSocketClient {

    final WebSocketCallback callback;

    public WebSocketClient(final WebSocketCallback callback) {
        this.callback = callback;
    }

    private final void onopen() {
        callback.connected();
    }

    private final void onclose() {
        callback.disconnected();
    }

    private final void onmessage(final String message) {
        callback.message(message);
    }

    public static native boolean isSupported()/*-{
                                              if (!$wnd.WebSocket) return false;
                                              return true;
                                              }-*/;

    public native void connect(String server) /*-{
                                              var that = this;
                                              if (!$wnd.WebSocket) {
                                              alert("WebSocket connections not supported by this browser");
                                              return;
                                              }
                                              that._ws=new $wnd.WebSocket(server);

                                              that._ws.onopen = function() {
                                              if(!that._ws) {
                                              return;
                                              }
                                              that.@com.ponysdk.ui.terminal.socket.WebSocketClient::onopen()();
                                              };


                                              that._ws.onmessage = function(response) {
                                              if (response.data) {
                                              that.@com.ponysdk.ui.terminal.socket.WebSocketClient::onmessage(Ljava/lang/String;)( response.data );
                                              }
                                              };

                                              that._ws.onclose = function(m) {
                                              that.@com.ponysdk.ui.terminal.socket.WebSocketClient::onclose()();
                                              };
                                              }-*/;

    public native void send(String message) /*-{
                                            if (this._ws) {
                                            this._ws.send(message);
                                            } else {
                                            alert("not connected!" + this._ws);
                                            }
                                            }-*/;

    public native void close() /*-{
                               this._ws.close();
                               }-*/;

}
