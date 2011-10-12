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
package com.ponysdk.core.export.command;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.command.Command;
import com.ponysdk.core.event.BusinessEvent.Level;
import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.export.ExportContext;
import com.ponysdk.core.export.ExporterTools;
import com.ponysdk.core.export.event.DataExportedEvent;

public class ExportCommand<T> implements Command {

    protected final ExportContext<T> exportContext;
    private final String exportName;
    private final EventBus eventBus;
    private final String fileName;
    private List<T> data;
    private final static Logger log = LoggerFactory.getLogger(ExportCommand.class);

    public ExportCommand(final String exportName, final ExportContext<T> exportContext) {
        this(exportName, exportContext, PonySession.getRootEventBus());
    }

    public ExportCommand(final String exportName, final ExportContext<T> exportContext, EventBus eventBus) {
        this.exportName = exportName;
        this.fileName = exportContext.getType().getFileName(exportName);
        this.exportContext = exportContext;
        this.eventBus = eventBus;
        this.data = exportContext.getSelectionResult().getSelectedData();
    }

    @Override
    public void execute() {
        if (data == null || data.isEmpty()) {
            return;
        }
        try {
            switch (exportContext.getType()) {
            case CSV:
                exportCSV(fileName);
                break;
            case PDF:
                exportPDF(fileName);
                break;
            case XML:
                exportXML(fileName);
                break;
            default:
                throw new Exception("Unknow export type : " + exportContext.getType());
            }
            onSuccess();
        } catch (final Exception e) {
            onFailure(e);
        }

    }

    protected void onSuccess() {
        final DataExportedEvent event = new DataExportedEvent(this, exportContext.getType());
        event.setLevel(Level.INFO);
        event.setBusinessMessage(data.size() + " " + exportName + "  exported into " + fileName);
        eventBus.fireEvent(event);
    }

    protected void onFailure(Throwable caught) {
        final DataExportedEvent event = new DataExportedEvent(this, exportContext.getType());
        event.setLevel(Level.INFO);
        final String errorMessage = "Failure occured when exporting " + exportName;
        log.error(errorMessage, caught);
        event.setBusinessMessage(errorMessage + ", see server logs for more details.");
        eventBus.fireEvent(event);
    }

    protected void exportCSV(String fileName) throws Exception {
        ExporterTools.exportCSV(fileName, exportContext.getExportableFields(), data, null, null);
    }

    protected void exportPDF(String fileName) throws Exception {
        ExporterTools.exportPDF(fileName, exportContext.getExportableFields(), data, exportName.toUpperCase());
    }

    protected void exportXML(String fileName) throws Exception {
        ExporterTools.exportXML(fileName, exportContext.getExportableFields(), data, exportName);
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public List<T> getData() {
        return this.data;
    }

}
