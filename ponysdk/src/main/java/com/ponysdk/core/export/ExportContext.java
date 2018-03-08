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

import java.io.Serializable;
import java.util.List;

import com.ponysdk.core.server.service.query.Query;
import com.ponysdk.core.ui.list.selector.SelectionResult;

public class ExportContext<T> implements Serializable {

    private static final long serialVersionUID = -4385175322776780654L;

    private Query query;

    private List<ExportableField> exportableFields;

    private Exporter<T> exporter;

    private SelectionResult<T> selectionResult;

    public ExportContext() {
    }

    public ExportContext(final Query query, final List<ExportableField> exportableFields, final SelectionResult<T> selectionResult) {
        this.query = query;
        this.exportableFields = exportableFields;
        this.selectionResult = selectionResult;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(final Query query) {
        this.query = query;
    }

    public List<ExportableField> getExportableFields() {
        return exportableFields;
    }

    public void setExportableFields(final List<ExportableField> exportableFields) {
        this.exportableFields = exportableFields;
    }

    public Exporter<T> getExporter() {
        return exporter;
    }

    public void setExporter(final Exporter<T> exportModule) {
        this.exporter = exportModule;
    }

    public SelectionResult<T> getSelectionResult() {
        return selectionResult;
    }

    public void setSelectionResult(final SelectionResult<T> selectionResult) {
        this.selectionResult = selectionResult;
    }
}
