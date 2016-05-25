
package com.ponysdk.ui.terminal;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportClosure;
import org.timepedia.exporter.client.Exportable;

@Export
@ExportClosure
public interface CommunicationErrorHandler extends Exportable {

    public void onCommunicationError(String code, String message);
}