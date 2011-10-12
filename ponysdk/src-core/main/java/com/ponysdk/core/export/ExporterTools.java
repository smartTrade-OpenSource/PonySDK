/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *  
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.ponysdk.core.export;

import java.awt.Color;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
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

public class ExporterTools {

    private static final Logger log = LoggerFactory.getLogger(ExporterTools.class);

    public static <T> void exportCSV(final String fileName, List<ExportableField> exportableFields, List<T> rows, String dateFormat, String timeZone) throws Exception {
        TimeZone timezone = null;
        if (timeZone != null)
            timezone = TimeZone.getTimeZone(timeZone);

        final CsvFormatter formatter = new CsvFormatter(exportableFields, dateFormat, timezone);
        final StringBuffer csv = new StringBuffer();
        csv.append(formatter.getHeader());
        csv.append("\n");
        for (final T row : rows) {
            csv.append(formatter.formatObject(row));
            csv.append("\n");
        }

        // Set MIME type to binary data to prevent opening of pdf in browser window
        final StreamResource streamResource = new StreamResource();
        streamResource.open(new StreamHandler() {

            @Override
            public void onStream(HttpServletRequest req, HttpServletResponse response) {
                response.reset();
                response.setContentType("application/vnd.ms-excel");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                PrintWriter printer;
                try {
                    printer = response.getWriter();
                    printer.print(csv.toString());
                    printer.flush();
                    printer.close();
                } catch (final Exception e) {
                    log.error("Error when exporting", e);
                }
            }
        });
    }

    public static <T> void exportPDF(final String fileName, List<ExportableField> exportableFields, List<T> records, String title) throws Exception {
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
            public void onStream(HttpServletRequest req, HttpServletResponse response) {
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
    }

    public static <T> void exportPDF(final String fileName, String jasperReport, List<T> records) throws Exception {
        final JRDataSource dsource = new JRBeanCollectionDataSource(records);
        final byte[] reportBytes = JasperRunManager.runReportToPdf(dsource.getClass().getClassLoader().getResourceAsStream(jasperReport), null, dsource);

        // Set MIME type to binary data to prevent opening of PDF in browser window
        final StreamResource streamResource = new StreamResource();
        streamResource.open(new StreamHandler() {

            @Override
            public void onStream(HttpServletRequest req, HttpServletResponse response) {
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

    }

    public static <T> void exportXML(final String fileName, List<ExportableField> exportableFields, List<T> rows, String rootName) throws Exception {

        final String records = convert(exportableFields, rows, rootName).toString();
        exportXMLString(fileName, records);
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
                s.append(StringEscapeUtils.escapeXml(String.valueOf(ExporterTools.getProperty(pojo, exportableField.getKey()))));
                s.append("</" + normalizedCaption + ">");
            }
            s.append("</" + rootName + ">");

        }
        s.append("</" + rootName + "s>");
        return s;
    }

    public static boolean hasProperty(Class<?> clas, String property) {
        final String propertyPath = property;
        boolean isValid = true;
        Class<?> propertyClass = null;
        try {
            if (propertyPath != null) {
                final String[] tokens = propertyPath.split("\\.");
                final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(clas);
                for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (propertyDescriptor.getName().equals(tokens[0])) {
                        propertyClass = propertyDescriptor.getPropertyType();
                        break;
                    }
                }

                if (propertyClass == null)
                    throw new Exception("unknown property#" + tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    final PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(propertyClass);
                    boolean found = false;
                    for (final PropertyDescriptor propertyDescriptor : descriptors) {
                        if (propertyDescriptor.getName().equals(tokens[i])) {
                            propertyClass = propertyDescriptor.getPropertyType();
                            found = true;
                        }
                    }
                    if (!found)
                        throw new Exception("unknown property#" + tokens[i] + " for class#" + propertyClass);
                }
            }
        } catch (final Exception e) {
            final String errorMessage = "Error occured when finding property '" + propertyPath + "'";
            log.error(errorMessage, e);
            isValid = false;
        }
        return isValid;
    }

    public static String getProperty(Object bean, String property) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final String propertyPath = property;
        Object propertyValue = "NA";
        if (propertyPath != null) {

            final String[] tokens = propertyPath.split("\\.");
            propertyValue = ExporterTools.getPropertyValue(bean, tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i - 1].equals("customFields")) {
                    final Method method = bean.getClass().getMethod("getCustomField", String.class);
                    propertyValue = method.invoke(bean, tokens[i]);
                } else if (tokens[i].equals("toString")) {
                    propertyValue = propertyValue.toString();
                } else if (PropertyUtils.isReadable(propertyValue, tokens[i])) {
                    propertyValue = PropertyUtils.getProperty(propertyValue, tokens[i]);
                } else {
                    if (propertyValue instanceof List<?>) {
                        final List<?> propertyList = (List<?>) propertyValue;
                        final List<Object> values = new ArrayList<Object>();
                        for (final Object object : propertyList) {
                            values.add(ExporterTools.getPropertyValue(object, tokens[i]));
                        }
                        if (values.isEmpty())
                            propertyValue = "NA";
                        else
                            propertyValue = values;
                    } else if (propertyValue instanceof Map<?, ?>) {
                        final Map<?, ?> propertyMap = (Map<?, ?>) propertyValue;
                        final List<Object> values = new ArrayList<Object>();
                        for (final Object object : propertyMap.values()) {
                            values.add(ExporterTools.getPropertyValue(object, tokens[i]));
                        }
                        propertyValue = values;
                    } else {
                        propertyValue = "NA";
                    }
                }
            }
        }
        return String.valueOf(propertyValue == null ? "NA" : propertyValue);
    }

    public static Object getPropertyValue(Object bean, final String propertyName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object propertyValue;
        if (PropertyUtils.isReadable(bean, propertyName)) {
            propertyValue = PropertyUtils.getProperty(bean, propertyName);
            if (propertyValue == null) {
                propertyValue = "NA";
            }
        } else {
            propertyValue = "NA";
        }
        return propertyValue;
    }
}
