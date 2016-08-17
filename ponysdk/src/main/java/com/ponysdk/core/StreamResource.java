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

package com.ponysdk.core;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.eventbus.StreamHandler;

public class StreamResource {

    public void open(final StreamHandler streamListener) {
        UIContext.get().stackStreamRequest(streamListener);
    }

    public void embed(final StreamHandler streamListener, final PWidget widget) {
        UIContext.get().stackEmbededStreamRequest(streamListener, widget.getID());
    }
}
