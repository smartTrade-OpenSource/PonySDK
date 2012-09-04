
package com.ponysdk.core.export.xml;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.core.export.ExportableField;
import com.ponysdk.core.export.Exporter;
import com.ponysdk.core.export.util.PropertyUtil;

public class XMLExporter<T> implements Exporter<T> {

    private static final Logger log = LoggerFactory.getLogger(XMLExporter.class);

    private static final String NAME = "XML";

    protected final String fileName;

    protected final String rootName;

    public XMLExporter(String rootName, String fileName) {
        this.rootName = rootName;
        this.fileName = fileName;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String export(List<ExportableField> exportableFields, List<T> records) throws Exception {
        final String xml = convert(exportableFields, records, rootName).toString();
        exportXMLString(fileName, xml);
        return records.size() + " row(s) exported in " + fileName;
    }

    public static <T> void exportXMLString(final String fileName, final String content) throws Exception {
        // Set MIME type to binary data to prevent opening of PDF in browser window
        final StreamResource streamResource = new StreamResource();
        streamResource.open(new StreamHandler() {

            @Override
            public void onStream(HttpServletRequest req, HttpServletResponse response) {
                response.reset();
                response.setContentType("application/xml");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                PrintWriter printer;
                try {
                    printer = response.getWriter();
                    printer.print(content);
                    printer.flush();
                    printer.close();
                } catch (final IOException e) {
                    log.error("Error when exporting", e);
                }
            }
        });
    }

    public static <T> StringBuilder convert(List<ExportableField> exportableFields, List<T> pojos, String rootName) throws Exception {
        rootName = StringEscapeUtils.escapeXml(rootName);
        final StringBuilder s = new StringBuilder();
        s.append("<" + rootName + "s>");
        for (final T pojo : pojos) {
            s.append("<" + rootName + ">");
            for (final ExportableField exportableField : exportableFields) {
                String normalizedCaption = exportableField.getCaption().replace(" ", "").replace(".", "_").replace("/", "Per");
                normalizedCaption = StringEscapeUtils.escapeXml(normalizedCaption);
                s.append("<" + normalizedCaption + ">");
                s.append(StringEscapeUtils.escapeXml(String.valueOf(PropertyUtil.getProperty(pojo, exportableField.getKey()))));
                s.append("</" + normalizedCaption + ">");
            }
            s.append("</" + rootName + ">");

        }
        s.append("</" + rootName + "s>");
        return s;
    }

}
