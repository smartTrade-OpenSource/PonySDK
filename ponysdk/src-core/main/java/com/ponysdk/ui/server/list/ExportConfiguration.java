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

package com.ponysdk.ui.server.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.export.ExportableField;
import com.ponysdk.core.export.Exporter;

public class ExportConfiguration {

    private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();

    private final Map<String, ExportableField> exportFields = new LinkedHashMap<String, ExportableField>();

    public void addExporter(final Exporter<?> exporter) {
        exporters.add(exporter);
    }

    public List<Exporter<?>> getExporters() {
        return exporters;
    }

    public void addExportableField(final ExportableField exportableField) {
        exportFields.put(exportableField.getKey(), exportableField);
    }

    public void addExportableFields(final Collection<ExportableField> exportableField) {
        for (final ExportableField ef : exportableField) {
            exportFields.put(ef.getKey(), ef);
        }
    }

    public void removeExportableField(final ExportableField exportableField) {
        exportFields.remove(exportableField.getKey());
    }

    public List<ExportableField> getExportableFields() {
        return new ArrayList<ExportableField>(exportFields.values());
    }
}
