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

import com.ponysdk.ui.terminal.WidgetType;

/**
 * A panel that lays its children
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it is run
 * have an explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 */
public class PLayoutPanel extends PComplexPanel {

    // TODO nciaravola missing methods
    //
    // void setWidgetBottomHeight(PWidget child,double bottom,Unit bottomUnit,double height,Unit heightUnit)
    // void setWidgetHorizontalPosition(Widget child, Alignment position)
    // void setWidgetLeftRight(Widget child, double left,Unit leftUnit,double right,Unit rightUnit);
    // void setWidgetLeftWidth(Widget child, double left, Unit leftUnit,double width, Unit widthUnit) ;
    // void setWidgetRightWidth(Widget child, double right, Unit rightUnit,double width, Unit widthUnit) ;
    // void setWidgetTopBottom(Widget child, double top, Unit topUnit,double bottom, Unit bottomUnit) ;
    // void setWidgetTopHeight(Widget child, double top, Unit topUnit,double height, Unit heightUnit) ;
    // void setWidgetVerticalPosition(Widget child, Alignment position) ;
    // void setWidgetVisible(Widget child, boolean visible) ;

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.LAYOUT_PANEL;
    }

}
