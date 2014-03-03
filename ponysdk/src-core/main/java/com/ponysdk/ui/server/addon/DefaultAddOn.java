
package com.ponysdk.ui.server.addon;

import org.json.JSONObject;

import com.ponysdk.ui.server.basic.PAddOn;

public class DefaultAddOn extends PAddOn {

    public DefaultAddOn() {
        super(null, null, null);
    }

    public DefaultAddOn(final JSONObject params) {
        super(null, null, params);
    }

}
