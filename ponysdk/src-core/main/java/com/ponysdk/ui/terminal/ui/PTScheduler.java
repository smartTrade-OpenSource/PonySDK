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
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTScheduler extends AbstractPTObject {

    private final Map<Long, SchedulerCommand> commandByIDs = new HashMap<Long, PTScheduler.SchedulerCommand>();

    @Override
    public void create(final PTInstruction create, final UIService uiService) {}

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final long commandID = update.getLong(PROPERTY.COMMAND_ID);
        if (update.containsKey(PROPERTY.STOP)) {
            // Stop the command
            commandByIDs.remove(commandID).cancel();
        } else if (update.containsKey(PROPERTY.FIXDELAY)) {
            // Fix-delay
            // Wait for execution terminated before scheduling again
            final SchedulerCommand previousCmd = commandByIDs.remove(commandID);
            if (previousCmd != null) previousCmd.cancel();
            final int delay = update.getInt(PROPERTY.FIXDELAY);
            final FixDelayCommand command = new FixDelayCommand(uiService, update.getObjectID(), commandID, delay);
            Scheduler.get().scheduleFixedDelay(command, delay);
            commandByIDs.put(commandID, command);
        } else if (update.containsKey(PROPERTY.FIXRATE)) {
            // Fix-rate
            final SchedulerCommand previousCmd = commandByIDs.remove(commandID);
            if (previousCmd != null) previousCmd.cancel();
            final int delay = update.getInt(PROPERTY.FIXRATE);
            final FixRateCommand command = new FixRateCommand(uiService, update.getObjectID(), commandID, delay);
            Scheduler.get().scheduleFixedDelay(command, delay);
            commandByIDs.put(commandID, command);
        }
    }

    @Override
    public void gc(final PTInstruction gc, final UIService uiService) {
        for (final SchedulerCommand command : commandByIDs.values()) {
            command.cancel();
        }
        commandByIDs.clear();
    }

    protected abstract class SchedulerCommand implements RepeatingCommand {

        protected final UIService uiService;
        protected final long schedulerID;
        protected final long commandID;
        protected final int delay;
        protected boolean cancelled = false;

        public SchedulerCommand(final UIService uiService, final long schedulerID, final long commandID, final int delay) {
            this.uiService = uiService;
            this.schedulerID = schedulerID;
            this.commandID = commandID;
            this.delay = delay;
        }

        public void cancel() {
            cancelled = true;
        }
    }

    protected class FixRateCommand extends SchedulerCommand {

        public FixRateCommand(final UIService uiService, final long schedulerID, final long commandID, final int delay) {
            super(uiService, schedulerID, commandID, delay);
        }

        @Override
        public boolean execute() {

            if (cancelled) return false;

            final PTInstruction instruction = new PTInstruction();
            instruction.setObjectID(schedulerID);
            instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            instruction.put(HANDLER.KEY, HANDLER.KEY_.SCHEDULER);
            instruction.put(PROPERTY.ID, commandID);
            instruction.put(PROPERTY.FIXRATE, delay);

            uiService.triggerEvent(instruction);

            return true;
        }

    }

    protected class FixDelayCommand extends SchedulerCommand {

        public FixDelayCommand(final UIService uiService, final long schedulerID, final long commandID, final int delay) {
            super(uiService, schedulerID, commandID, delay);
        }

        @Override
        public boolean execute() {

            if (cancelled) return false;

            final PTInstruction instruction = new PTInstruction();
            instruction.setObjectID(schedulerID);
            instruction.put(TYPE.KEY, TYPE.KEY_.EVENT);
            instruction.put(HANDLER.KEY, HANDLER.KEY_.SCHEDULER);
            instruction.put(PROPERTY.ID, commandID);
            instruction.put(PROPERTY.FIXDELAY, delay);

            uiService.triggerEvent(instruction);

            return false;
        }

    }
}
