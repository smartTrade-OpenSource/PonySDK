
package com.ponysdk.ui.terminal.request;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.ponysdk.ui.terminal.model.Model;

public class ParentWindowRequest extends RequestBuilder {

    private final String windowID;

    public ParentWindowRequest(final String windowID, final RequestCallback callback) {
        super(callback);
        this.windowID = windowID;

        exportOnDataReceived();
    }

    @Override
    public void send(final String s) {
        final JSONObject jsoObject = new JSONObject();
        jsoObject.put(Model.DATA.getKey(), new JSONString(s));
        sendToParent(windowID, jsoObject.getJavaScriptObject());
    }

    public void onDataReceived(final String text) {
        callback.onDataReceived(JSONParser.parseStrict(text).isObject());
    }

    public static native void sendToParent(final String objectID, final JavaScriptObject data) /*-{$wnd.opener.sendDataToServer(objectID, data);}-*/;

    public native void exportOnDataReceived() /*-{
                                              var that = this;
                                              $wnd.onDataReceived = function(text) {
                                              $entry(that.@com.ponysdk.ui.terminal.request.ParentWindowRequest::onDataReceived(Ljava/lang/String;)(text));
                                              }
                                              }-*/;

    @Override
    public void sendHeartbeat() {
        // TODO Auto-generated method stub

    }

}
