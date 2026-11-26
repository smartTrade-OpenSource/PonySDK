/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.impl.java;

import java.util.ArrayDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

/**
 * A page to watch a transparent reconnection: a server thread updates a tick every 100 ms, and every second adds a
 * row and removes the oldest past 20, so that both property updates and structural writes cross a lost connection.
 * After a resume, the rows shown must be the last 20 with no gap, and the tick the server's.
 * <p>
 * Run with {@code -Dponysdk.entry.point.class=com.ponysdk.impl.java.ReconnectionSampleEntryPoint}.
 */
public class ReconnectionSampleEntryPoint implements EntryPoint {

    private static final int ROWS = 20;

    @Override
    public void start(final UIContext uiContext) {
        final PFlowPanel root = Element.newPFlowPanel();
        PWindow.getMain().add(root);

        final PLabel tick = Element.newPLabel("tick 0");
        tick.ensureDebugId("tick");
        root.add(tick);

        final int[] clicks = { 0 };
        final PButton button = Element.newPButton("clicks 0");
        button.ensureDebugId("clicks");
        button.addClickHandler(event -> button.setText("clicks " + ++clicks[0]));
        root.add(button);

        final PFlowPanel rows = Element.newPFlowPanel();
        rows.ensureDebugId("rows");
        root.add(rows);
        final ArrayDeque<PLabel> shown = new ArrayDeque<>();

        final ScheduledExecutorService pusher = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "reconnection-sample-" + uiContext.getID());
            thread.setDaemon(true);
            return thread;
        });
        final long[] n = { 0 };
        pusher.scheduleAtFixedRate(() -> uiContext.execute(() -> {
            n[0]++;
            tick.setText("tick " + n[0]);
            if (n[0] % 10 == 0) {
                final PLabel row = Element.newPLabel("row " + n[0] / 10);
                row.addStyleName("row");
                rows.add(row);
                shown.addLast(row);
                if (shown.size() > ROWS) shown.removeFirst().removeFromParent();
            }
        }), 100, 100, TimeUnit.MILLISECONDS);
        uiContext.addContextDestroyListener(context -> pusher.shutdownNow());
    }
}
