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

package com.ponysdk.ui.server.basic;

import com.ponysdk.ui.terminal.WidgetType;

/**
 * An absolute panel positions all of its children absolutely, allowing them to overlap.
 * <p>
 * Note that this panel will not automatically resize itself to allow enough room for its
 * absolutely-positioned children. It must be explicitly sized in order to make room for them.
 * </p>
 * <p>
 * Once a widget has been added to an absolute panel, the panel effectively "owns" the positioning of the
 * widget. Any existing positioning attributes on the widget may be modified by the panel.
 * </p>
 */
public class PAbsolutePanel extends PComplexPanel {

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ABSOLUTE_PANEL;
    }

}
