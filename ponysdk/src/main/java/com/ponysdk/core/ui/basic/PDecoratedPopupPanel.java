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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.WidgetType;

/**
 * <p>
 * A {@link PPopupPanel} that wraps its content in a 3x3 grid, which allows users to add rounded
 * corners.
 * </p>
 * <h3>Setting the Size:</h3>
 * <p>
 * If you set the width or height of the {@link PDecoratedPopupPanel}, you need to set the height
 * and width of the middleCenter cell to 100% so that the middleCenter cell takes up all of the
 * available space.
 * If you do not set the width and height of the {@link PDecoratedPopupPanel}, it will wrap its
 * contents tightly.
 * </p>
 *
 * <pre>
 * .gwt-DecoratedPopupPanel .popupMiddleCenter {
 *   height: 100%;
 *   width: 100%;
 * }
 * </pre>
 *
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratedPopupPanel { the outside of the popup }</li>
 * <li>.gwt-DecoratedPopupPanel .popupContent { the wrapper around the content }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopLeft { the top left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopCenter { the top center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopRight { the top right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupTopRightInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleLeft { the middle left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleCenter { the middle center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleRight { the middle right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupMiddleRightInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomLeft { the bottom left cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomCenter { the bottom center cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomRight { the bottom right cell }</li>
 * <li>.gwt-DecoratedPopupPanel .popupBottomRightInner { the inner element of the cell }</li>
 * </ul>
 */
public class PDecoratedPopupPanel extends PPopupPanel {

    public PDecoratedPopupPanel(final boolean autoHide) {
        super(autoHide);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DECORATED_POPUP_PANEL;
    }
}
