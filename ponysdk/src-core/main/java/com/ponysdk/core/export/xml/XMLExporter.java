
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
import com.ponysdk.core.internalization.PString;

public class XMLExporter<T> implements Exporter<T> {

    private static final Logger log = LoggerFactory.getLogger(XMLExporter.class);

    private static final String NAME = "XML";

    protected final String fileName;

    protected final String rootName;

    public XMLExporter(final String rootName, final String fileName) {
        this.rootName = rootName;
        this.fileName = fileName;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String export(final List<ExportableField> exportableFields, final List<T> records) throws Exception {
        final String xml = convert(exportableFields, records, rootName).toString();
        exportXMLString(fileName, xml);

        return PString.get("export.result", records.size(), fileName);
    }

    public void exportXMLString(final String fileName, final String content) throws Exception {
        // Set MIME type to binary data to prevent opening of PDF in browser window
        final StreamResource streamResource = new StreamResource();
        streamResource.open(new StreamHandler() {

            @Override
            public void onStream(final HttpServletRequest req, final HttpServletResponse response) {
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

    public StringBuilder convert(final List<ExportableField> exportableFields, final List<T> pojos, String rootName) throws Exception {
        rootName = StringEscapeUtils.escapeXml(rootName);
        final StringBuilder s = new StringBuilder();
        s.append("<" + rootName + "s>");
        for (final T pojo : pojos) {
            s.append("<" + rootName + ">");
            for (final ExportableField exportableField : exportableFields) {
                String normalizedCaption = exportableField.getCaption().replace(" ", "").replace(".", "_").replace("/", "Per");
                normalizedCaption = StringEscapeUtils.escapeXml(normalizedCaption);
                s.append("<" + normalizedCaption + ">");
                s.append(StringEscapeUtils.escapeXml(getDisplayValue(pojo, exportableField)));
                s.append("</" + normalizedCaption + ">");
            }
            s.append("</" + rootName + ">");

        }
        s.append("</" + rootName + "s>");
        return s;
    }

    protected String getDisplayValue(final T pojo, final ExportableField exportableField) throws Exception {
        return PropertyUtil.getProperty(pojo, exportableField.getKey());
    }

}
