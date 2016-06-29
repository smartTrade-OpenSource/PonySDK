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

package com.ponysdk.sample.client.page.addon;

import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.event.PTerminalEvent;

public class SelectizeAddon extends PAddOnComposite<PElement> implements PTerminalEvent.Handler {

    public SelectizeAddon() {
        super(new PElement("input"));
        asWidget().setAttribute("value", "science,biology,chemistry,physics");
        setTerminalHandler(this);
        // <input type="text" id="input-tags3" class="demo-default"
        // value="science,biology,chemistry,physics">
    }

    public void text(final String text) {
        callTerminalMethod("text", text);
    }

    @Override
    public void onTerminalEvent(final PTerminalEvent event) {
        System.err.println(event.getJsonObject());
    }

}
