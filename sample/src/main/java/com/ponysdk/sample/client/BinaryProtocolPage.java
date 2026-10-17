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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;
import static com.ponysdk.sample.client.Costs.*;

import java.time.Duration;
import java.util.List;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.sample.client.page.addon.BinaryArgsAddOn;

/** Binary protocol: typed arrays streamed in pure binary. */
final class BinaryProtocolPage {

    private BinaryProtocolPage() {
    }

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel panel = Element.newPFlowPanel();
        final PElement box = Element.newDiv();
        box.setAttribute("style", "max-width:1040px");

        final PElement title = Element.newDiv();
        title.addStyleName("pony-page-title");
        title.setInnerText("Binary protocol — live signal console");

        final PElement sub = Element.newDiv();
        sub.addStyleName("pony-page-desc");
        sub.setAttribute("style", "margin-bottom:24px");
        sub.setInnerText("Server-driven end to end: the controls below are PonySDK widgets whose handlers run in Java; "
            + "the server computes the signals and streams them as typed double arrays in pure binary, and the browser "
            + "only renders. Push series and resolution up to stream tens of thousands of typed values per frame "
            + "(uint31 length, no 255 cap, no JSON).");

        // The chart is configured entirely through pure-binary typed creation args:
        // color (String), maxPoints (int), lineWidth (double), fill (boolean), seed (long)
        final BinaryArgsAddOn addon = new BinaryArgsAddOn("#4f46e5", 1024, 2.0, false, 1_234_567_890_123L);
        addon.asWidget().setAttribute("style", "display:block;margin-top:4px");

        // Stream state owned by the server
        final boolean[] playing = { true };
        final int[] nSeries = { 2 };
        final int[] mPoints = { 1024 };
        final double[] phase = { 0 };

        // Controls — PonySDK widgets; every click handler runs server-side
        final PFlowPanel controls = Element.newPFlowPanel();
        controls.addStyleName("ov-controls");
        controls.setAttribute("style", "align-items:flex-end");

        final PButton toggle = Element.newPButton("Pause");
        styleBinaryCtrl(toggle, true);
        toggle.addClickHandler(e -> {
            playing[0] = !playing[0];
            toggle.setText(playing[0] ? "Pause" : "Resume");
            styleBinaryCtrl(toggle, playing[0]);
        });
        final PFlowPanel toggleRow = Element.newPFlowPanel();
        toggleRow.setAttribute("style", "display:flex");
        toggleRow.add(toggle);
        controls.add(binaryCtrlGroup("Stream", toggleRow));

        final PButton[] sBtns = new PButton[4];
        final PFlowPanel seriesRow = Element.newPFlowPanel();
        seriesRow.setAttribute("style", "display:flex");
        for (int k = 1; k <= sBtns.length; k++) {
            final int val = k;
            final PButton b = Element.newPButton(String.valueOf(k));
            styleBinaryCtrl(b, k == nSeries[0]);
            b.addClickHandler(e -> {
                nSeries[0] = val;
                for (int j = 0; j < sBtns.length; j++) styleBinaryCtrl(sBtns[j], j + 1 == val);
            });
            sBtns[k - 1] = b;
            seriesRow.add(b);
        }
        controls.add(binaryCtrlGroup("Series", seriesRow));

        final int[] resVals = { 256, 1024, 4096 };
        final PButton[] rBtns = new PButton[resVals.length];
        final PFlowPanel resRow = Element.newPFlowPanel();
        resRow.setAttribute("style", "display:flex");
        for (int k = 0; k < resVals.length; k++) {
            final int idx = k;
            final int val = resVals[k];
            final PButton b = Element.newPButton(String.valueOf(val));
            styleBinaryCtrl(b, val == mPoints[0]);
            b.addClickHandler(e -> {
                mPoints[0] = val;
                for (int j = 0; j < rBtns.length; j++) styleBinaryCtrl(rBtns[j], j == idx);
            });
            rBtns[k] = b;
            resRow.add(b);
        }
        controls.add(binaryCtrlGroup("Points / series", resRow));

        box.add(title, sub, controls, addon.asWidget());
        panel.add(box);

        // Deterministic verification frame (kept stable for the browser IT): 1000 ints, 0..999
        final Object[] ramp = new Object[1000];
        for (int i = 0; i < ramp.length; i++) ramp[i] = i;
        addon.verify(ramp);

        // Live multi-series stream — one structured typed binary payload per frame: [n, m, n*m doubles]
        schedulers.add(PScheduler.scheduleAtFixedRate(() -> {
            if (!playing[0] || idle()) return;
            final int n = nSeries[0];
            final int m = mPoints[0];
            final Object[] frame = new Object[2 + n * m];
            frame[0] = n;
            frame[1] = m;
            for (int s = 0; s < n; s++) {
                final double off = s * 0.7;
                final int base = 2 + s * m;
                for (int i = 0; i < m; i++) {
                    final double t = (double) i / m;
                    frame[base + i] = Math.sin(t * Math.PI * (6 + s * 2) + phase[0] + off)
                        * (0.6 + 0.4 * Math.sin(t * Math.PI * 2 + phase[0] * 0.5))
                        + 0.06 * Math.sin(t * Math.PI * 40 + phase[0] * 3);
                }
            }
            phase[0] += 0.15;
            addon.stream(frame);
        }, Duration.ofMillis(120)));

        return panel;
    }
}
