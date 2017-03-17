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

package com.ponysdk.core.terminal.ui;

import java.util.logging.Logger;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.terminal.model.ReaderBuffer;

abstract class PTPanel<T extends Panel> extends PTWidget<T> {

    private final static Logger log = Logger.getLogger(PTPanel.class.getName());

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final Widget widget = asWidget(ptObject);
        if (widget != null) uiObject.add(widget);
        else log.warning("No widget created for object #" + ptObject.getObjectID() + ", Details : " + buffer);
    }

    @Override
    public void remove(final ReaderBuffer buffer, final PTObject ptObject) {
        uiObject.remove(asWidget(ptObject));
    }

}
