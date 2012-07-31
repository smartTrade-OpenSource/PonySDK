
package com.ponysdk.ui.terminal;

import com.google.gwt.json.client.JSONObject;

public interface RequestCallback {

    public void onDataReceived(JSONObject data);

    public void onError(Throwable exception);
}
