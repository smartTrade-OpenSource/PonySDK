
package com.ponysdk.core.statistic;

import javax.json.JsonObject;

import com.ponysdk.ui.server.basic.PObject;

public interface TerminalDataReceiver {

    void onDataReceived(PObject object, JsonObject instruction);
}
