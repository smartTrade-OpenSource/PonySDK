/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.sample.quickstart;

import java.time.Duration;
import java.util.Random;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

/** The application the showcase's Overview shows, whole: a price the server pushes twenty times a second. */
// region app
public class LivePrice implements EntryPoint {

    @Override
    public void start(final UIContext uiContext) {
        final PLabel price = Element.newPLabel("1.08512");
        PWindow.getMain().add(price);
        final Random feed = new Random();
        final double[] mid = { 1.08512 };
        // each tick: about 10 bytes on the wire
        PScheduler.scheduleAtFixedRate(() -> {
            mid[0] += feed.nextGaussian() / 20_000;
            price.setText(String.format("%.5f", mid[0]));
        }, Duration.ofMillis(50));
    }
}
// endregion
