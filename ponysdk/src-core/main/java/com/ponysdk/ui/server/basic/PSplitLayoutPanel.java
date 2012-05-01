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

import com.google.gwt.dom.client.Style.Unit;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A panel that adds user-positioned splitters between each of its child widgets.
 * <p>
 * This panel is used in the same way as {@link PDockLayoutPanel}, except that its children's sizes are always
 * specified in {@link Unit#PX} units, and each pair of child widgets has a splitter between them that the
 * user can drag.
 * </p>
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it is run
 * have an explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SplitLayoutPanel { the panel itself }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-HDragger { horizontal dragger }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-VDragger { vertical dragger }</li>
 * </ul>
 */
public class PSplitLayoutPanel extends PDockLayoutPanel {

    public PSplitLayoutPanel(final Unit unit) {
        super(unit);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SPLIT_LAYOUT_PANEL;
    }

}
