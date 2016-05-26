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
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.event.CommunicationErrorEvent;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ClientToServerModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public class PTScheduler extends AbstractPTObject implements CommunicationErrorEvent.Handler {

    private final Map<Long, SchedulerCommand> commandByIDs = new HashMap<>();
    private boolean hasCommunicationError = false;

    private UIBuilder uiService;

    public PTScheduler() {
        UIBuilder.getRootEventBus().addHandler(CommunicationErrorEvent.TYPE, this);
    }

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);
        this.uiService = uiService;
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        // ServerToClientModel.COMMAND_ID
        final long commandID = binaryModel.getLongValue();

        final BinaryModel model = buffer.getBinaryModel();
        if (ServerToClientModel.STOP.equals(model)) {
            // Stop the command
            commandByIDs.remove(commandID).cancel();
            return true;
        } else if (ServerToClientModel.FIXDELAY.equals(model)) {
            final int delay = model.getIntValue();
            // Fix-delay
            // Wait for execution terminated before scheduling again
            final SchedulerCommand previousCmd = commandByIDs.remove(commandID);
            if (previousCmd != null)
                previousCmd.cancel();
            final FixDelayCommand command = new FixDelayCommand(uiService, getObjectID(), commandID, delay);
            Scheduler.get().scheduleFixedDelay(command, delay);
            commandByIDs.put(commandID, command);
            return true;
        } else if (ServerToClientModel.FIXRATE.equals(model)) {
            final int delay = model.getIntValue();
            // Fix-rate
            final SchedulerCommand previousCmd = commandByIDs.remove(commandID);
            if (previousCmd != null)
                previousCmd.cancel();
            final FixRateCommand command = new FixRateCommand(uiService, getObjectID(), commandID, delay);
            Scheduler.get().scheduleFixedDelay(command, delay);
            commandByIDs.put(commandID, command);
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public void gc(final UIBuilder uiService) {
        for (final SchedulerCommand command : commandByIDs.values()) {
            command.cancel();
        }
        commandByIDs.clear();
    }

    protected abstract class SchedulerCommand implements RepeatingCommand {

        protected final UIBuilder uiService;
        protected final int schedulerID;
        protected final long commandID;
        protected final int delay;
        protected boolean cancelled = false;

        public SchedulerCommand(final UIBuilder uiService, final int schedulerID, final long commandID,
                final int delay) {
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

        public FixRateCommand(final UIBuilder uiService, final int schedulerID, final long commandID, final int delay) {
            super(uiService, schedulerID, commandID, delay);
        }

        @Override
        public boolean execute() {
            if (cancelled)
                return false;

            final PTInstruction instruction = new PTInstruction(schedulerID);
            instruction.put(ClientToServerModel.HANDLER_SCHEDULER);
            instruction.put(ClientToServerModel.COMMAND_ID, commandID);
            instruction.put(ClientToServerModel.FIXRATE, delay);

            uiService.sendDataToServer(instruction);

            return !hasCommunicationError;
        }

    }

    protected class FixDelayCommand extends SchedulerCommand {

        public FixDelayCommand(final UIBuilder uiService, final int schedulerID, final long commandID,
                final int delay) {
            super(uiService, schedulerID, commandID, delay);
        }

        @Override
        public boolean execute() {
            if (cancelled)
                return false;

            final PTInstruction instruction = new PTInstruction(schedulerID);
            instruction.put(ClientToServerModel.HANDLER_SCHEDULER);
            instruction.put(ClientToServerModel.COMMAND_ID, commandID);
            instruction.put(ClientToServerModel.FIXDELAY, delay);

            uiService.sendDataToServer(instruction);

            return false;
        }

    }

    @Override
    public void onCommunicationError(final CommunicationErrorEvent event) {
        hasCommunicationError = true;
    }
}
