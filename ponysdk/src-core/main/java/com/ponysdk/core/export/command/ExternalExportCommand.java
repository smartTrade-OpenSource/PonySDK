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

package com.ponysdk.core.export.command;

import java.util.List;

import com.ponysdk.core.command.AbstractServiceCommand;
import com.ponysdk.core.export.ExportContext;
import com.ponysdk.core.query.Result;
import com.ponysdk.ui.server.list.selector.SelectionMode;
import com.ponysdk.ui.server.list.selector.SelectionResult;

public class ExternalExportCommand<T, U extends Result<List<T>>> extends ExportCommand<T> {

    private final AbstractServiceCommand<U> findCommand;

    public ExternalExportCommand(final String exportName, final ExportContext<T> exportContext, final AbstractServiceCommand<U> findCommand) {
        super(exportContext);
        this.findCommand = findCommand;
    }

    @Override
    public String execute() {
        if (exportContext.getSelectionResult().getSelectionMode() == SelectionMode.FULL) {
            final U findResult = findCommand.execute();
            if (findCommand.isSuccessfull()) {
                exportContext.setSelectionResult(new SelectionResult<>(SelectionMode.FULL, findResult.getData()));
                return super.execute();
            } else {
                super.onFailure(findCommand.getCaught());
                return null;
            }
        } else {
            return super.execute();
        }
    }

}
