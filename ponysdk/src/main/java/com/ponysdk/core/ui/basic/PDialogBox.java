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

import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;

/**
 * A form of popup that has a caption area at the top and can be dragged by the user. Unlike a
 * PPopupPanel,
 * calls to {@link #setWidth(String)} and {@link #setHeight(String)} will set the width and height
 * of the
 * dialog box itself, even if a widget has not been added as yet.
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.pony-DialogBox</dt>
 * <dd>the dialog, in the top layer; <code>::backdrop</code> when modal</dd>
 * <dt>.pony-DialogBox .pony-Caption</dt>
 * <dd>the caption, by which it is dragged</dd>
 * <dt>.pony-DialogBox .pony-Content</dt>
 * <dd>the content</dd>
 * <dt>.pony-Glass</dt>
 * <dd>the glass behind it, when enabled</dd>
 * </dl>
 */
public class PDialogBox extends PDecoratedPopupPanel {

    private String caption;

    protected PDialogBox() {
        this(false);
    }

    protected PDialogBox(final boolean autoHide) {
        super(autoHide);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.DIALOG_BOX;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(final String caption) {
        if (Objects.equals(this.caption, caption)) return;
        this.caption = caption;
        saveUpdate(ServerToClientModel.POPUP_CAPTION, caption);
    }

}
