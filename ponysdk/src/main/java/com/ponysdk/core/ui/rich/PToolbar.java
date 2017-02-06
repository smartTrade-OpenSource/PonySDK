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

package com.ponysdk.core.ui.rich;

import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWidget;

public class PToolbar extends PHorizontalPanel {

    public static final String HUNDRED_PERCENT = "100%";

    public PToolbar() {
        setStyleName("ptoolbar");
        setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
    }

    @Override
    public void add(final PWidget w) {
        super.add(w);
        setCellHeight(w, HUNDRED_PERCENT);
    }

    public void addSepararator() {
        final PSimplePanel separator = Element.newPSimplePanel();
        separator.addStyleName("pony-Toolbar-Separator");
        add(separator);
    }

}
