
package com.ponysdk.core.export.csv;

import com.ponysdk.core.export.Exporter;

public class CSVExporterFactory {

    public static <T> Exporter<T> newPDFExporter(String fileName) {
        return new CSVExporter<T>(fileName);
    }
}
