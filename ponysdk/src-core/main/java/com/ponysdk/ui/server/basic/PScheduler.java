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

package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * This class provides low-level task scheduling primitives.
 */
public abstract class PScheduler extends PObject {

    private static final String SCHEDULER_KEY = PScheduler.class.getCanonicalName();

    private final Map<Long, RepeatingCommand> commandByID = new HashMap<Long, RepeatingCommand>();
    private final Map<RepeatingCommand, Long> IDByCommand = new HashMap<RepeatingCommand, Long>();

    private PScheduler() {}

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCHEDULER;
    }

    private static PScheduler get(final long windowID) {
        final String rootID = SCHEDULER_KEY + "_" + windowID;
        PScheduler scheduler = UIContext.get().getAttribute(rootID);
        if (scheduler == null) {
            scheduler = new PScheduler() {};
            UIContext.get().setAttribute(SCHEDULER_KEY, scheduler);
        }
        return scheduler;
    }

    public static PScheduler get() {
        if (UIContext.getCurrentWindow() == null) return get(0);
        return get(UIContext.getCurrentWindow().getID());
    }

    public void scheduleFixedRate(final RepeatingCommand cmd, final int delayMs) {
        final Long existingCommandID = IDByCommand.get(cmd);
        if (existingCommandID != null) {
            scheduleFixedRateCommand(existingCommandID, delayMs);
        } else {
            final long cmdID = UIContext.get().nextID();
            scheduleFixedRateCommand(cmdID, delayMs);
            commandByID.put(cmdID, cmd);
            IDByCommand.put(cmd, cmdID);
        }
    }

    public void scheduleFixedDelay(final RepeatingCommand cmd, final int delayMs) {
        final Long existingCommandID = IDByCommand.get(cmd);
        if (existingCommandID != null) cancelScheduleCommand(existingCommandID);

        final long cmdID = UIContext.get().nextID();
        scheduleFixedDelayCommand(cmdID, delayMs);
        commandByID.put(cmdID, cmd);
        IDByCommand.put(cmd, cmdID);

    }

    private void scheduleFixedRateCommand(final long cmdID, final int delayMs) {
        final Update update = new Update(ID);
        update.put(PROPERTY.START, true);
        update.put(PROPERTY.COMMAND_ID, cmdID);
        update.put(PROPERTY.FIXRATE, delayMs);
        Txn.get().getTxnContext().save(update);
    }

    private void scheduleFixedDelayCommand(final long cmdID, final int delayMs) {
        final Update update = new Update(ID);
        update.put(PROPERTY.START, true);
        update.put(PROPERTY.COMMAND_ID, cmdID);
        update.put(PROPERTY.FIXDELAY, delayMs);
        Txn.get().getTxnContext().save(update);
    }

    private void cancelScheduleCommand(final long cmdID) {
        final Update update = new Update(ID);
        update.put(PROPERTY.STOP, true);
        update.put(PROPERTY.COMMAND_ID, cmdID);
        Txn.get().getTxnContext().save(update);

        final RepeatingCommand command = commandByID.remove(cmdID);
        IDByCommand.remove(command);
    }

    @Override
    public void onClientData(final JSONObject instruction) throws JSONException {
        if (instruction.getString(HANDLER.KEY).equals(HANDLER.KEY_.SCHEDULER)) {
            final long cmdID = instruction.getLong(PROPERTY.ID);
            final RepeatingCommand command = commandByID.get(cmdID);
            if (command == null) return;

            final boolean invokeAgain = command.execute();
            if (!invokeAgain) {
                cancelScheduleCommand(cmdID);
            } else {
                // Re-schedule in fixed delay mode
                if (instruction.has(PROPERTY.FIXDELAY)) {
                    scheduleFixedDelayCommand(cmdID, instruction.getInt(PROPERTY.FIXDELAY));
                }
            }
        } else {
            super.onClientData(instruction);
        }
    }

    public interface RepeatingCommand {

        /**
         * Returns true if the RepeatingCommand should be invoked again.
         */
        boolean execute();
    }

}
