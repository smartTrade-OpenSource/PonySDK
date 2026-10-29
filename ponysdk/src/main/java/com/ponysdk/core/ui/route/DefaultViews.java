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

package com.ponysdk.core.ui.route;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;

/** The views a {@link Router} shows when the application gives it none for a path that matches nothing, or a denied one. */
final class DefaultViews {

    private DefaultViews() {
    }

    static final class NotFound implements IsPWidget {

        private final PLabel element = Element.newPLabel();

        NotFound(final RouteParameters parameters) {
            element.setText("Not found: " + parameters.token());
        }

        @Override
        public PWidget asWidget() {
            return element;
        }
    }

    static final class Denied implements IsPWidget {

        private final PLabel element = Element.newPLabel();

        Denied(final RouteParameters parameters) {
            element.setText("Access denied: " + parameters.token());
        }

        @Override
        public PWidget asWidget() {
            return element;
        }
    }
}
