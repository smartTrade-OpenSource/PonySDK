
package com.ponysdk.core.export.csv;

import com.ponysdk.core.export.Exporter;

public class CSVExporterFactory {

    public static <T> Exporter<T> newCSVExporter(String fileName) {
        return new CSVExporter<T>(fileName);
    }
}
