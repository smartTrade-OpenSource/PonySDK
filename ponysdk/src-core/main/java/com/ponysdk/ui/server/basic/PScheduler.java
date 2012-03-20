
package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.terminal.HandlerType;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.EventInstruction;
import com.ponysdk.ui.terminal.instruction.Remove;

public abstract class PScheduler extends PObject {

    private final Map<Long, RepeatingCommand> commands = new HashMap<Long, RepeatingCommand>();

    private PScheduler() {}

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCHEDULER;
    }

    public static PScheduler get() {
        PScheduler scheduler = PonySession.getCurrent().getAttribute(PScheduler.class.getCanonicalName());
        if (scheduler == null) {
            scheduler = new PScheduler() {};
            PonySession.getCurrent().setAttribute(PScheduler.class.getCanonicalName(), scheduler);
        }
        return scheduler;
    }

    public void scheduleFixedDelay(final RepeatingCommand cmd, final int delayMs) {
        final long cmdID = PonySession.getCurrent().nextID();

        final Add add = new Add(cmdID, ID);
        add.getMainProperty().setProperty(PropertyKey.DELAY, delayMs);

        PonySession.getCurrent().stackInstruction(add);
        commands.put(cmdID, cmd);
    }

    @Override
    public void onEventInstruction(final EventInstruction instruction) {
        if (HandlerType.SCHEDULER.equals(instruction.getHandlerType())) {
            final long cmdID = instruction.getMainProperty().getChildProperty(PropertyKey.ID).getLongValue();
            final RepeatingCommand command = commands.get(cmdID);
            final boolean invokeAgain = command.execute();
            if (!invokeAgain) {
                final Remove remove = new Remove(cmdID, ID);
                PonySession.getCurrent().stackInstruction(remove);
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
