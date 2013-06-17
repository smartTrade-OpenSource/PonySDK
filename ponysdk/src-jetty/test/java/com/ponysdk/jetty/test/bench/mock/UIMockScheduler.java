
package com.ponysdk.jetty.test.bench.mock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.jetty.test.bench.UI;
import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;

public class UIMockScheduler extends UIMock {

    private static Logger log = LoggerFactory.getLogger(UIMockScheduler.class);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public UIMockScheduler(final long objectID) {
        super(objectID);
    }

    @Override
    public void update(final UI ui, final JSONObject instruction) throws Exception {
        final long aCommandID = instruction.getLong(PROPERTY.COMMAND_ID);
        if (instruction.has(PROPERTY.FIXRATE)) {
            final int delay = instruction.getInt(PROPERTY.FIXRATE);
            executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    try {
                        final JSONObject i = new JSONObject();
                        i.put(PROPERTY.OBJECT_ID, UIMockScheduler.this.objectID);
                        i.put(TYPE.KEY, TYPE.KEY_.EVENT);
                        i.put(HANDLER.KEY, HANDLER.KEY_.SCHEDULER);
                        i.put(PROPERTY.ID, aCommandID);
                        i.put(PROPERTY.FIXRATE, delay);
                        ui.sendToServer(i);
                    } catch (final Exception e) {
                        log.error("UIScheduler failed. Shutting down", e);
                        executor.shutdownNow();
                    }
                }
            }, delay, delay, TimeUnit.MILLISECONDS);
        }

    }
}