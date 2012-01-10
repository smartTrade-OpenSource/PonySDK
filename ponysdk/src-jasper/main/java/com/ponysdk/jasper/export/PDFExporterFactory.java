
package com.ponysdk.jasper.export;

import com.ponysdk.core.export.Exporter;

public class PDFExporterFactory {

    public static <T> Exporter<T> newPDFExporter(String title, String fileName) {
        return new PDFExporter<T>(title, fileName);
    }
}
