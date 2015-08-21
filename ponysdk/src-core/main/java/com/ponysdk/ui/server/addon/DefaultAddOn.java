
package com.ponysdk.ui.server.addon;

import javax.json.JsonObject;

import com.ponysdk.ui.server.basic.PAddOn;

public class DefaultAddOn extends PAddOn {

    public DefaultAddOn() {
        super(null, null, null);
    }

    public DefaultAddOn(final JsonObject params) {
        super(null, null, params);
    }

}
