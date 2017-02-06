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
 * A {@link PSimplePanel} that wraps its contents in stylized boxes, which can be used to add
 * rounded corners
 * to a {@link PWidget}.
 * </p>
 * <p>
 * This widget will <em>only</em> work in quirks mode in most cases. Specifically, setting the
 * height or width
 * of the DecoratorPanel will result in rendering issues.
 * </p>
 * <p>
 * Wrapping a {@link PWidget} in a "9-box" allows users to specify images in each of the corners and
 * along the
 * four borders. This method allows the content within the {@link PDecoratorPanel} to resize without
 * disrupting the look of the border. In addition, rounded corners can generally be combined into a
 * single
 * image file, which reduces the number of downloaded files at startup. This class also simplifies
 * the process
 * of using AlphaImageLoaders to support 8-bit transparencies (anti-aliasing and shadows) in ie6,
 * which does
 * not support them normally.
 * </p>
 * <h3>Setting the Size:</h3>
 * <p>
 * If you set the width or height of the {@link PDecoratorPanel}, you need to set the height and
 * width of the
 * middleCenter cell to 100% so that the middleCenter cell takes up all of the available space. If
 * you do not
 * set the width and height of the {@link PDecoratorPanel}, it will wrap its contents tightly.
 * </p>
 *
 * <pre>
 * .gwt-DecoratorPanel .middleCenter {
 *   height: 100%;
 *   width: 100%;
 * }
 * </pre>
 *
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DecoratorPanel { the panel }</li>
 * <li>.gwt-DecoratorPanel .top { the top row }</li>
 * <li>.gwt-DecoratorPanel .topLeft { the top left cell }</li>
 * <li>.gwt-DecoratorPanel .topLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .topCenter { the top center cell }</li>
 * <li>.gwt-DecoratorPanel .topCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .topRight { the top right cell }</li>
 * <li>.gwt-DecoratorPanel .topRightInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .middle { the middle row }</li>
 * <li>.gwt-DecoratorPanel .middleLeft { the middle left cell }</li>
 * <li>.gwt-DecoratorPanel .middleLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .middleCenter { the middle center cell }</li>
 * <li>.gwt-DecoratorPanel .middleCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .middleRight { the middle right cell }</li>
 * <li>.gwt-DecoratorPanel .middleRightInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .bottom { the bottom row }</li>
 * <li>.gwt-DecoratorPanel .bottomLeft { the bottom left cell }</li>
 * <li>.gwt-DecoratorPanel .bottomLeftInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .bottomCenter { the bottom center cell }</li>
 * <li>.gwt-DecoratorPanel .bottomCenterInner { the inner element of the cell }</li>
 * <li>.gwt-DecoratorPanel .bottomRight { the bottom right cell }</li>
 * <li>.gwt-DecoratorPanel .bottomRightInner { the inner element of the cell }</li>
 * </ul>
 */
public class PDecoratorPanel extends PSimplePanel {

    protected PDecoratorPanel() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DECORATOR_PANEL;
    }
}
