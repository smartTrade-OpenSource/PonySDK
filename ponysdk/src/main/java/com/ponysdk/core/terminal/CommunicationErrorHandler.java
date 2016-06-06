
package com.ponysdk.core.terminal;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportClosure;
import org.timepedia.exporter.client.Exportable;

@Export
@ExportClosure
public interface CommunicationErrorHandler extends Exportable {

    void onCommunicationError(String code, String message);
}