
package com.ponysdk.core.terminal;

import org.timepedia.exporter.client.Exportable;

public interface StartupListener extends Exportable {

    void onPonySDKLoaded();
}
