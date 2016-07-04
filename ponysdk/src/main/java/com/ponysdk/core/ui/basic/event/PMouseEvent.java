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

package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.ui.eventbus.EventHandler;

public abstract class PMouseEvent<H extends EventHandler> extends PHumanInputEvent<H> {

    private int nativeButton;
    private int x;
    private int y;
    private int clientX;
    private int clientY;
    private int sourceAbsoluteLeft;
    private int sourceAbsoluteTop;
    private int sourceOffsetHeight;
    private int sourceOffsetWidth;

    public PMouseEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    public int getClientX() {
        return clientX;
    }

    public void setClientX(final int clientX) {
        this.clientX = clientX;
    }

    public int getClientY() {
        return clientY;
    }

    public void setClientY(final int clientY) {
        this.clientY = clientY;
    }

    public int getSourceAbsoluteLeft() {
        return sourceAbsoluteLeft;
    }

    public void setSourceAbsoluteLeft(final int sourceAbsoluteLeft) {
        this.sourceAbsoluteLeft = sourceAbsoluteLeft;
    }

    public int getSourceAbsoluteTop() {
        return sourceAbsoluteTop;
    }

    public void setSourceAbsoluteTop(final int sourceAbsoluteTop) {
        this.sourceAbsoluteTop = sourceAbsoluteTop;
    }

    public int getSourceOffsetHeight() {
        return sourceOffsetHeight;
    }

    public void setSourceOffsetHeight(final int sourceOffsetHeight) {
        this.sourceOffsetHeight = sourceOffsetHeight;
    }

    public int getSourceOffsetWidth() {
        return sourceOffsetWidth;
    }

    public void setSourceOffsetWidth(final int sourceOffsetWidth) {
        this.sourceOffsetWidth = sourceOffsetWidth;
    }

    public int getNativeButton() {
        return nativeButton;
    }

    public void setNativeButton(final int nativeButton) {
        this.nativeButton = nativeButton;
    }

    public int getX() {
        return x;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(final int y) {
        this.y = y;
    }

}