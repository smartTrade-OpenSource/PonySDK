
package com.ponysdk.ui.server.addon;

import javax.json.JsonObject;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAddOn;
import com.ponysdk.ui.server.basic.PWidget;

public class DefaultUIAddOn extends PAddOn {

    public DefaultUIAddOn(final IsPWidget w) {
        super(null, w.asWidget(), null);
    }

    public DefaultUIAddOn(final IsPWidget w, final JsonObject params) {
        super(null, w.asWidget(), params);
    }

    public DefaultUIAddOn(final PWidget w) {
        super(null, w, null);
    }

    public DefaultUIAddOn(final PWidget w, final JsonObject params) {
        super(null, w, params);
    }

}
