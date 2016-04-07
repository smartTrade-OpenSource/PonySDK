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

import javax.json.JsonObject;

import com.ponysdk.core.Parser;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;

/**
 * This class provides low-level task scheduling primitives.
 */
public abstract class PScheduler extends PObject {

    private static final String SCHEDULER_KEY = PScheduler.class.getCanonicalName();

    private final Map<Long, RepeatingCommand> commandByID = new HashMap<>();
    private final Map<RepeatingCommand, Long> IDByCommand = new HashMap<>();

    private PScheduler() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCHEDULER;
    }

    public static PScheduler get() {
        return get(null);
    }

    private static PScheduler get(final PWindow window) {
        String rootID = SCHEDULER_KEY;

        if (window != null) {
            rootID += window.getID();
        }

        PScheduler scheduler = UIContext.get().getAttribute(rootID);
        if (scheduler == null) {
            scheduler = new PScheduler() {
            };
            UIContext.get().setAttribute(SCHEDULER_KEY, scheduler);
        }
        return scheduler;
    }

    public static void scheduleFixedRate(final RepeatingCommand cmd, final int delayMs) {
        get().scheduleFixedRate0(cmd, delayMs);
    }

    public static void scheduleFixedDelay(final RepeatingCommand cmd, final int delayMs) {
        get().scheduleFixedDelay0(cmd, delayMs);
    }

    private void scheduleFixedRate0(final RepeatingCommand cmd, final int delayMs) {
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

    private void scheduleFixedDelay0(final RepeatingCommand cmd, final int delayMs) {
        final Long existingCommandID = IDByCommand.get(cmd);
        if (existingCommandID != null)
            cancelScheduleCommand(existingCommandID);

        final long cmdID = UIContext.get().nextID();
        scheduleFixedDelayCommand(cmdID, delayMs);
        commandByID.put(cmdID, cmd);
        IDByCommand.put(cmd, cmdID);
    }

    private void scheduleFixedRateCommand(final long cmdID, final int delayMs) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.COMMAND_ID, cmdID);
        parser.parse(Model.FIXRATE, delayMs);
        parser.endObject();
    }

    private void scheduleFixedDelayCommand(final long cmdID, final int delayMs) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.COMMAND_ID, cmdID);
        parser.parse(Model.FIXDELAY, delayMs);
        parser.endObject();
    }

    private void cancelScheduleCommand(final long cmdID) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE, ID);
        if (window != null) {
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.parse(Model.COMMAND_ID, cmdID);
        parser.parse(Model.STOP);
        parser.endObject();

        final RepeatingCommand command = commandByID.remove(cmdID);
        IDByCommand.remove(command);
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(HandlerModel.HANDLER_KEY_SCHEDULER.toStringValue())) {
            final long cmdID = instruction.getJsonNumber(Model.COMMAND_ID.toStringValue()).longValue();
            final RepeatingCommand command = commandByID.get(cmdID);
            if (command == null)
                return;

            final boolean invokeAgain = command.execute();
            if (!invokeAgain) {
                cancelScheduleCommand(cmdID);
            } else {
                // Re-schedule in fixed delay mode
                if (instruction.containsKey(Model.FIXDELAY.toStringValue())) {
                    scheduleFixedDelayCommand(cmdID, instruction.getInt(Model.FIXDELAY.toStringValue()));
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
