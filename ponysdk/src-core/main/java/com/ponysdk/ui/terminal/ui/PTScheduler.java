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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTScheduler extends AbstractPTObject {

    private final Map<Long, SchedulerCommand> commandByIDs = new HashMap<Long, PTScheduler.SchedulerCommand>();

    @Override
    public void create(final PTInstruction create, final UIService uiService) {}

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final long commandID = update.getLong(PROPERTY.COMMAND_ID);
        if (update.containsKey(PROPERTY.START)) {
            final SchedulerCommand previousCmd = commandByIDs.get(commandID);
            if (previousCmd != null) previousCmd.cancel();

            final SchedulerCommand command = new SchedulerCommand(uiService, update.getObjectID(), commandID);
            Scheduler.get().scheduleFixedDelay(command, update.getInt(PROPERTY.DELAY));
            commandByIDs.put(commandID, command);
        } else {
            commandByIDs.remove(commandID).cancel();
        }
    }

    @Override
    public void gc(final PTInstruction gc, final UIService uiService) {
        for (final SchedulerCommand command : commandByIDs.values()) {
            command.cancel();
        }
        commandByIDs.clear();
    }

    protected class SchedulerCommand implements RepeatingCommand {

        private final UIService uiService;
        private final long schedulerID;
        private final long commandID;
        private boolean cancelled = false;

        public SchedulerCommand(final UIService uiService, final long schedulerID, final long commandID) {
            this.uiService = uiService;
            this.schedulerID = schedulerID;
            this.commandID = commandID;
        }

        @Override
        public boolean execute() {

            if (cancelled) return false;

            final PTInstruction instruction = new PTInstruction();
            instruction.setObjectID(schedulerID);
            instruction.put(TYPE.KEY, TYPE.EVENT);
            instruction.put(HANDLER.KEY, HANDLER.SCHEDULER);
            instruction.put(PROPERTY.ID, commandID);

            uiService.triggerEvent(instruction);

            return true;
        }

        public void cancel() {
            cancelled = true;
        }

    }
}
