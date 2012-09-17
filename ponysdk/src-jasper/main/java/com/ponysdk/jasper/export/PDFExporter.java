
package com.ponysdk.jasper.export;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.HorizontalAlign;
import ar.com.fdvs.dj.domain.constants.Page;
import ar.com.fdvs.dj.domain.constants.VerticalAlign;

import com.ponysdk.core.StreamResource;
import com.ponysdk.core.event.StreamHandler;
import com.ponysdk.core.export.ExportableField;
import com.ponysdk.core.export.Exporter;

public class PDFExporter<T> implements Exporter<T> {

    private static final Logger log = LoggerFactory.getLogger(PDFExporter.class);

    private static final String NAME = "PDF";

    protected final String fileName;

    protected final String title;

    public PDFExporter(final String title, final String fileName) {
        this.fileName = fileName;
        this.title = title;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String export(final List<ExportableField> exportableFields, final List<T> records) throws Exception {
        // Dynamic report
        final FastReportBuilder drb = new FastReportBuilder();

        // Style header
        final Style headerStyle = new Style();
        headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
        headerStyle.setHorizontalAlign(HorizontalAlign.CENTER);
        headerStyle.setTransparent(false);
        headerStyle.setBackgroundColor(Color.LIGHT_GRAY);
        headerStyle.setBorder(Border.THIN);

        // Style column
        final Style columnStyle = new Style();
        columnStyle.setVerticalAlign(VerticalAlign.MIDDLE);
        columnStyle.setHorizontalAlign(HorizontalAlign.CENTER);
        columnStyle.setBorder(Border.THIN);

        // Add column
        for (final ExportableField exportableField : exportableFields) {
            drb.addColumn(exportableField.getCaption(), exportableField.getKey(), String.class, 10, columnStyle, headerStyle);
        }

        drb.setTitle(title);
        // drb.setPrintBackgroundOnOddRows(true);
        drb.setUseFullPageWidth(true);
        drb.setPageSizeAndOrientation(Page.Page_A4_Landscape());

        final DynamicReport dynamicReport = drb.build();

        // Simple report
        final JasperReport report = DynamicJasperHelper.generateJasperReport(dynamicReport, new ClassicLayoutManager(), new HashMap<Object, Object>());

        final byte[] reportBytes = JasperRunManager.runReportToPdf(report, new HashMap<Object, Object>(), new DynamicExportDataSource(records));

        // Set MIME type to binary data to prevent opening of PDF in browser window
        final StreamResource streamResource = new StreamResource();
        streamResource.open(new StreamHandler() {

            @Override
            public void onStream(final HttpServletRequest req, final HttpServletResponse response) {
                response.reset();
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                try {
                    final OutputStream outputStream = response.getOutputStream();
                    outputStream.write(reportBytes);
                    outputStream.flush();
                    outputStream.close();
                } catch (final IOException e) {
                    log.error("Error when exporting", e);
                }
            }
        });
        return "";
    }
    // private static <T> void exportPDF(final String fileName, String jasperReport, List<T> records) throws
    // Exception {
    // final JRDataSource dsource = new JRBeanCollectionDataSource(records);
    // final byte[] reportBytes =
    // JasperRunManager.runReportToPdf(dsource.getClass().getClassLoader().getResourceAsStream(jasperReport),
    // null, dsource);
    //
    // // Set MIME type to binary data to prevent opening of PDF in browser window
    // final StreamResource streamResource = new StreamResource();
    // streamResource.open(new StreamHandler() {
    //
    // @Override
    // public void onStream(HttpServletRequest req, HttpServletResponse response) {
    // response.reset();
    // response.setContentType("application/pdf");
    // response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    // try {
    // final OutputStream outputStream = response.getOutputStream();
    // outputStream.write(reportBytes);
    // outputStream.flush();
    // outputStream.close();
    // } catch (final IOException e) {
    // log.error("Error when exporting", e);
    // }
    // }
    // });
    //
    // }

}
