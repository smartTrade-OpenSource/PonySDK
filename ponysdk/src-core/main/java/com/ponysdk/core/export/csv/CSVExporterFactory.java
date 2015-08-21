
package com.ponysdk.core.export.csv;

import com.ponysdk.core.export.Exporter;

public class CSVExporterFactory {

    public static <T> Exporter<T> newCSVExporter(final String fileName) {
        return new CSVExporter<>(fileName);
    }
}
