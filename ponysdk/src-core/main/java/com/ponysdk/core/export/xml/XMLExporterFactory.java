
package com.ponysdk.core.export.xml;

import com.ponysdk.core.export.Exporter;

public class XMLExporterFactory {

    public static <T> Exporter<T> newXMLExporter(final String rootName, final String fileName) {
        return new XMLExporter<>(rootName, fileName);
    }
}
