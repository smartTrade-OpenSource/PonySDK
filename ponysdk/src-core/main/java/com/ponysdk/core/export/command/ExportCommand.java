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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.command.Command;
import com.ponysdk.core.event.PBusinessEvent.Level;
import com.ponysdk.core.event.PEventBus;
import com.ponysdk.core.export.ExportContext;
import com.ponysdk.core.export.Exporter;
import com.ponysdk.core.export.event.DataExportedEvent;

public class ExportCommand<T> implements Command<String> {

    protected final ExportContext<T> exportContext;

    private final PEventBus eventBus;

    private final static Logger log = LoggerFactory.getLogger(ExportCommand.class);

    public ExportCommand(final ExportContext<T> exportContext) {
        this(exportContext, PonySession.getRootEventBus());
    }

    public ExportCommand(final ExportContext<T> exportContext, final PEventBus eventBus) {
        this.exportContext = exportContext;
        this.eventBus = eventBus;
    }

    @Override
    public String execute() {
        if (exportContext.getSelectionResult().getSelectedData() == null || exportContext.getSelectionResult().getSelectedData().isEmpty()) return null;
        try {
            Exporter<T> exporter = exportContext.getExporter();
            String message = exporter.export(exportContext.getExportableFields(), exportContext.getSelectionResult().getSelectedData());
            onSuccess(message);
            return message;
        } catch (final Exception e) {
            onFailure(e);
            return null;
        }
    }

    protected void onSuccess(final String message) {
        final DataExportedEvent event = new DataExportedEvent(this, exportContext.getExporter());
        event.setLevel(Level.INFO);
        event.setBusinessMessage(message);
        eventBus.fireEvent(event);
    }

    protected void onFailure(final Throwable caught) {
        final DataExportedEvent event = new DataExportedEvent(this, exportContext.getExporter());
        event.setLevel(Level.INFO);
        log.error("Failure occured when exporting", caught);
        event.setBusinessMessage(caught.getMessage() + ", see server logs for more details.");
        eventBus.fireEvent(event);
    }

}
