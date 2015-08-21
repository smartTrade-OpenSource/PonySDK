
package com.ponysdk.core;

import javax.json.JsonObject;

import com.ponysdk.ui.server.basic.PObject;

public interface ClientDataOutput {

    void onClientData(PObject object, JsonObject instruction);
}
