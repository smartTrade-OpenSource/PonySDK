
package com.ponysdk.core;

import org.json.JSONObject;

import com.ponysdk.ui.server.basic.PObject;

public interface ClientDataOutput {

    void onClientData(PObject object, JSONObject instruction);
}
