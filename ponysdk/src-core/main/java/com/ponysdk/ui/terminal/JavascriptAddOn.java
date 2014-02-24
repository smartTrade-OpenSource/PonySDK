
package com.ponysdk.ui.terminal;

import com.google.gwt.core.client.JavaScriptObject;

public class JavascriptAddOn extends JavaScriptObject {

    protected JavascriptAddOn() {}

    public final native void update(JavaScriptObject json) /*-{ this.update(json); }-*/;

    public final native void onAttach(boolean attached) /*-{ this.onAttach(attached); }-*/;
}
