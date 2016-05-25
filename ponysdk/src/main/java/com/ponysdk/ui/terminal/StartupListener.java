
package com.ponysdk.ui.terminal;

import org.timepedia.exporter.client.Exportable;

public interface StartupListener extends Exportable {

    public void onPonySDKLoaded();
}
