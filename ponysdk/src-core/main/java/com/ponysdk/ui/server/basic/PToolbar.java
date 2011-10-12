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
package com.ponysdk.ui.server.basic;

import java.util.Iterator;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class PToolbar implements IsPWidget, HasPWidgets {

    private final PHorizontalPanel panel = new PHorizontalPanel();

    public PToolbar() {
        panel.setStyleName(PonySDKTheme.TOOLBAR);
        panel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
    }

    @Override
    public PWidget asWidget() {
        return panel;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return panel.iterator();
    }

    @Override
    public void add(PWidget w) {
        panel.add(w);
        panel.setCellHeight(w, "100%");
    }

    public void addSepararator() {
        final PSimplePanel separator = new PSimplePanel();
        separator.addStyleName(PonySDKTheme.TOOLBAR_SEPARATOR);
        panel.add(separator);
    }

    @Override
    public void clear() {
        panel.clear();
    }

    @Override
    public boolean remove(PWidget w) {
        return panel.remove(w);
    }

}
