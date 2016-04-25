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

package com.ponysdk.ui.terminal.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTTerminalScheduledCommand extends AbstractPTObject {

    private final static Logger log = Logger.getLogger(PTTerminalScheduledCommand.class.getName());

    private UIService uiService;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        // ServerToClientModel.FIXDELAY
        final int delayMs = buffer.getBinaryModel().getIntValue();
        if (delayMs <= 0) {
            Scheduler.get().scheduleFinally(new RepeatingCommand() {

                @Override
                public boolean execute() {
                    final PTScript script = new PTScript();
                    // ServerToClientModel.EVAL
                    return script.update(buffer, buffer.getBinaryModel());
                }
            });
        } else {
            Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                @Override
                public boolean execute() {
                    final PTScript script = new PTScript();
                    // ServerToClientModel.EVAL
                    return script.update(buffer, buffer.getBinaryModel());
                }

            }, delayMs);
        }

        this.uiService = uiService;
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        // ServerToClientModel.FIXDELAY
        final int delayMs = buffer.getBinaryModel().getIntValue();
        if (delayMs < 0) {
            Scheduler.get().scheduleFinally(new RepeatingCommand() {

                @Override
                public boolean execute() {
                    executeInstruction(buffer);
                    return false;
                }
            });
            return true;
        } else {
            Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                @Override
                public boolean execute() {
                    executeInstruction(buffer);
                    return false;
                }

            }, delayMs);
            return true;
        }
    }

    protected void executeInstruction(final ReaderBuffer buffer) {
        final List<PTInstruction> instructions = new ArrayList<>();
        // for (int i = 0; i < jsonArray.size(); i++) {
        // instructions.add(new
        // PTInstruction(jsonArray.get(i).isObject().getJavaScriptObject()));
        // }

        PTInstruction currentInstruction = null;
        try {
            for (final PTInstruction instruction : instructions) {
                currentInstruction = instruction;
                uiService.processInstruction(instruction);
            }
        } catch (final Throwable e) {
            log.log(Level.SEVERE, "PonySDK has encountered an internal error while executing a scheduled command : "
                    + currentInstruction + " => Error Message " + e.getMessage(), e);
            uiService.stackError(currentInstruction, e);
        } finally {
            uiService.flushEvents();
        }
    }

}
