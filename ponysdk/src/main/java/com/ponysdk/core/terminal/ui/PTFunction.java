/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.terminal.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTFunction extends AbstractPTObject {

    private static final Logger log = Logger.getLogger(PTFunction.class.getName());

    private JsTextFunction function;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        final String name = buffer.readBinaryModel().getStringValue();
        function = lookup(name);
        if (function == null) log.log(Level.SEVERE, "No terminal function named " + name);
    }

    public static native JsTextFunction lookup(String name) /*-{
                                                            var f = $wnd[name];
                                                            return typeof f === 'function' ? f : null;
                                                            }-*/;

    public JsTextFunction getFunction() {
        return function;
    }

}
