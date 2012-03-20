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
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.GC;
import com.ponysdk.ui.terminal.instruction.Remove;

public class PTScheduler extends AbstractPTObject {

    private final Map<Long, SchedulerCommand> commandByIDs = new HashMap<Long, PTScheduler.SchedulerCommand>();

    @Override
    public void create(final Create create, final UIService uiService) {}

    @Override
    public void add(final Add add, final UIService uiService) {
        final int delayMs = add.getMainProperty().getChildProperty(PropertyKey.DELAY).getIntValue();
        final SchedulerCommand command = new SchedulerCommand(uiService, add.getParentID(), add.getObjectID());
        Scheduler.get().scheduleFixedDelay(command, delayMs);
        commandByIDs.put(add.getObjectID(), command);
    }

    @Override
    public void remove(final Remove remove, final UIService uiService) {
        final SchedulerCommand command = commandByIDs.remove(remove.getObjectID());
        command.cancel();
    }

    @Override
    public void gc(final GC gc, final UIService uiService) {
        for (final SchedulerCommand command : commandByIDs.values())
            command.cancel();
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

            final EventInstruction instruction = new EventInstruction(schedulerID, HandlerType.SCHEDULER);
            instruction.getMainProperty().setProperty(PropertyKey.ID, commandID);
            uiService.triggerEvent(instruction);

            return true;
        }

        public void cancel() {
            cancelled = true;
        }

    }
}
