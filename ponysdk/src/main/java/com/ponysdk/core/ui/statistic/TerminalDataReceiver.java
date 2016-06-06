
package com.ponysdk.core.ui.statistic;

import javax.json.JsonObject;

import com.ponysdk.core.ui.basic.PObject;

public interface TerminalDataReceiver {

    void onDataReceived(PObject object, JsonObject instruction);
}
