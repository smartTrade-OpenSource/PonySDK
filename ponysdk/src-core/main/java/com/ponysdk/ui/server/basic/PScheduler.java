
package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Update;

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
            update.getMainProperty().setProperty(PropertyKey.START, true);
            update.getMainProperty().setProperty(PropertyKey.COMMAND_ID, existingCommandID);
            update.getMainProperty().setProperty(PropertyKey.DELAY, delayMs);
            PonySession.getCurrent().stackInstruction(update);
        } else {
            final long cmdID = PonySession.getCurrent().nextID();
            final Update update = new Update(ID);
            update.getMainProperty().setProperty(PropertyKey.START, true);
            update.getMainProperty().setProperty(PropertyKey.COMMAND_ID, cmdID);
            update.getMainProperty().setProperty(PropertyKey.DELAY, delayMs);
            PonySession.getCurrent().stackInstruction(update);
            commandByID.put(cmdID, cmd);
            IDByCommand.put(cmd, cmdID);
        }
    }

    @Override
    public void onEventInstruction(final EventInstruction instruction) {
        if (HandlerType.SCHEDULER.equals(instruction.getHandlerType())) {
            final long cmdID = instruction.getMainProperty().getChildProperty(PropertyKey.ID).getLongValue();
            final RepeatingCommand command = commandByID.get(cmdID);
            final boolean invokeAgain = command.execute();
            if (!invokeAgain) {
                final Update update = new Update(ID);
                update.getMainProperty().setProperty(PropertyKey.STOP, true);
                update.getMainProperty().setProperty(PropertyKey.COMMAND_ID, cmdID);
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
