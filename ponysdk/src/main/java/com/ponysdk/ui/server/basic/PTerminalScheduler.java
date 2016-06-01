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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.ui.model.ClientToServerModel;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * This class provides low-level task scheduling primitives.
 */
public final class PTerminalScheduler extends PObject {

    private static final String SCHEDULER_KEY = PTerminalScheduler.class.getCanonicalName();

    private final Map<Long, RepeatingCommand> commands = new HashMap<>();

    private PTerminalScheduler() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCHEDULER;
    }

    public static PTerminalScheduler get() {
        return get(null);
    }

    private static PTerminalScheduler get(final PWindow window) {
        String rootID = SCHEDULER_KEY;

        if (window != null) {
            rootID += window.getID();
        }

        PTerminalScheduler scheduler = UIContext.get().getAttribute(rootID);
        if (scheduler == null) {
            scheduler = new PTerminalScheduler();
            UIContext.get().setAttribute(SCHEDULER_KEY, scheduler);
        }
        return scheduler;
    }

    public static void scheduleFixedRate(final RepeatingCommand cmd, final Duration duration) {
        get().scheduleFixedRate0(cmd, duration.toMillis());
    }

    public static void scheduleFixedDelay(final RepeatingCommand cmd, final Duration duration) {
        get().scheduleFixedDelay0(cmd, duration.toMillis());
    }

    private void scheduleFixedRate0(final RepeatingCommand cmd, final long delayMs) {
        final long commandID = UIContext.get().nextID();
        scheduleFixedRateCommand(commandID, (int) delayMs);// Terminal
                                                           // limitation
        commands.put(commandID, cmd);
    }

    private void scheduleFixedDelay0(final RepeatingCommand cmd, final long delayMs) {
        final long cmdID = UIContext.get().nextID();
        scheduleFixedDelayCommand(cmdID, (int) delayMs);// Terminal limitation
        commands.put(cmdID, cmd);
    }

    private void scheduleFixedRateCommand(final long commandID, final int delayMs) {
        saveUpdate(ServerToClientModel.COMMAND_ID, commandID, ServerToClientModel.FIXRATE, delayMs);
    }

    private void scheduleFixedDelayCommand(final long cmdID, final int delayMs) {
        saveUpdate(ServerToClientModel.COMMAND_ID, cmdID, ServerToClientModel.FIXDELAY, delayMs);
    }

    private void cancelScheduleCommand(final long commandID) {
        commands.remove(commandID);
        saveUpdate(ServerToClientModel.COMMAND_ID, commandID, ServerToClientModel.STOP, null);
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (instruction.containsKey(ClientToServerModel.HANDLER_KEY_SCHEDULER.toStringValue())) {
            final long commandID = instruction.getJsonNumber(ClientToServerModel.COMMAND_ID.toStringValue()).longValue();
            final RepeatingCommand command = commands.get(commandID);
            if (command == null) return;

            if (!command.execute()) {
                cancelScheduleCommand(commandID);
            } else {
                // Re-schedule in fixed delay mode
                if (instruction.containsKey(ClientToServerModel.FIXDELAY.toStringValue())) {
                    scheduleFixedDelayCommand(commandID, instruction.getInt(ClientToServerModel.FIXDELAY.toStringValue()));
                }
            }
        } else {
            super.onClientData(instruction);
        }
    }

    @FunctionalInterface
    public interface RepeatingCommand {

        /**
         * Returns true if the RepeatingCommand should be invoked again.
         */
        boolean execute();
    }

}
