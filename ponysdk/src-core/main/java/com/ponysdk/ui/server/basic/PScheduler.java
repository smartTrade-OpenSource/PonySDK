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

import com.ponysdk.core.PonySession;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

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

    public static PScheduler get() {
        PScheduler scheduler = PonySession.getCurrent().getAttribute(SCHEDULER_KEY);
        if (scheduler == null) {
            scheduler = new PScheduler() {};
            PonySession.getCurrent().setAttribute(SCHEDULER_KEY, scheduler);
        }
        return scheduler;
    }

    public void scheduleFixedDelay(final RepeatingCommand cmd, final int delayMs) {
        final Long existingCommandID = IDByCommand.get(cmd);
        if (existingCommandID != null) {
            final Update update = new Update(ID);
            update.put(PROPERTY.START, true);
            update.put(PROPERTY.COMMAND_ID, existingCommandID);
            update.put(PROPERTY.DELAY, delayMs);
            PonySession.getCurrent().stackInstruction(update);
        } else {
            final long cmdID = PonySession.getCurrent().nextID();
            final Update update = new Update(ID);
            update.put(PROPERTY.START, true);
            update.put(PROPERTY.COMMAND_ID, cmdID);
            update.put(PROPERTY.DELAY, delayMs);
            PonySession.getCurrent().stackInstruction(update);
            commandByID.put(cmdID, cmd);
            IDByCommand.put(cmd, cmdID);
        }
    }

    @Override
    public void onEventInstruction(final JSONObject instruction) throws JSONException {
        if (instruction.getString(HANDLER.KEY).equals(HANDLER.SCHEDULER)) {
            final long cmdID = instruction.getLong(PROPERTY.ID);
            final RepeatingCommand command = commandByID.get(cmdID);
            final boolean invokeAgain = command.execute();
            if (!invokeAgain) {
                final Update update = new Update(ID);
                update.put(PROPERTY.STOP, true);
                update.put(PROPERTY.COMMAND_ID, cmdID);
                PonySession.getCurrent().stackInstruction(update);

                commandByID.remove(cmdID);
                IDByCommand.remove(command);
            }
        } else {
            super.onEventInstruction(instruction);
        }
    }

    public interface RepeatingCommand {

        /**
         * Returns true if the RepeatingCommand should be invoked again.
         */
        boolean execute();
    }

}
