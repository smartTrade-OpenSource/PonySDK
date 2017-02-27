/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.export.csv;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.export.ExportableField;
import com.ponysdk.core.export.Exporter;
import com.ponysdk.core.export.util.PropertyUtil;
import com.ponysdk.core.export.xml.XMLExporter;
import com.ponysdk.core.internalization.PString;
import com.ponysdk.core.server.application.UIContext;

public class CSVExporter<T> implements Exporter<T> {

    private static final Logger log = LoggerFactory.getLogger(XMLExporter.class);

    private static final String NAME = "CSV";

    private static final char DELIMITER = ';';

    private final String fileName;

    public CSVExporter(final String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String export(final List<ExportableField> exportableFields, final List<T> records) throws Exception {
        UIContext.get().stackStreamRequest((req, response, uiContext) -> {
            try {
                response.reset();
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".csv");
                final PrintWriter printer = response.getWriter();
                try {
                    final Iterator<ExportableField> iter = exportableFields.iterator();

                    while (iter.hasNext()) {
                        final ExportableField exportableField = iter.next();
                        final String header = exportableField.getCaption();
                        printer.print(header);
                        if (iter.hasNext()) printer.print(DELIMITER);
                    }

                    printer.println();
                    for (final T row : records) {
                        for (final ExportableField exportableField : exportableFields) {
                            printer.print(getDisplayValue(row, exportableField));
                            printer.print(DELIMITER);
                        }
                        printer.println();
                        printer.flush();
                    }
                } catch (final Exception e) {
                    log.error("Error when exporting", e);
                } finally {
                    printer.flush();
                    printer.close();
                }
            } catch (final Exception e) {
                log.error("Error when exporting", e);
            }
        });

        return PString.get("export.result", records.size(), fileName);
    }

    protected String getDisplayValue(final T row, final ExportableField exportableField) throws Exception {
        return PropertyUtil.getProperty(row, exportableField.getKey());
    }
}
