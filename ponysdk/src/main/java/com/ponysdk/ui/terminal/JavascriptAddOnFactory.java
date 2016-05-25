
package com.ponysdk.ui.terminal;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportClosure;
import org.timepedia.exporter.client.Exportable;

import com.google.gwt.core.client.JavaScriptObject;

@Export
@ExportClosure
public interface JavascriptAddOnFactory extends Exportable {

    public JavascriptAddOn newAddOn(JavaScriptObject params);

}
