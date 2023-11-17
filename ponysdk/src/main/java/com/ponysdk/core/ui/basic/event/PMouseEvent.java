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

    private NativeButton nativeButton;
    private int x;
    private int y;
    private int clientX;
    private int clientY;
    private int sourceAbsoluteLeft;
    private int sourceAbsoluteTop;
    private int sourceOffsetHeight;
    private int sourceOffsetWidth;

    private boolean controlKeyDown;
    private boolean altKeyDown;
    private boolean shiftKeyDown;
    private boolean metaKeyDown;

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

    public NativeButton getNativeButton() {
        return nativeButton;
    }

    public void setNativeButton(final int nativeButton) {
        this.nativeButton = NativeButton.fromValue(nativeButton);
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

    public boolean isControlKeyDown() {
        return controlKeyDown;
    }

    public void setControlKeyDown(final boolean controlKeyDown) {
        this.controlKeyDown = controlKeyDown;
    }

    public boolean isAltKeyDown() {
        return altKeyDown;
    }

    public void setAltKeyDown(final boolean altKeyDown) {
        this.altKeyDown = altKeyDown;
    }

    public boolean isShiftKeyDown() {
        return shiftKeyDown;
    }

    public void setShiftKeyDown(final boolean shiftKeyDown) {
        this.shiftKeyDown = shiftKeyDown;
    }

    public boolean isMetaKeyDown() {
        return metaKeyDown;
    }

    public void setMetaKeyDown(final boolean metaKeyDown) {
        this.metaKeyDown = metaKeyDown;
    }

    @Override
    public String toString() {
        return super.toString() + " ; x = " + x + " ; y = " + y + " ; nativeButton = " + nativeButton + " ; clientX = " + clientX
                + " ; clientY = " + clientY + " ; sourceAbsoluteLeft = " + sourceAbsoluteLeft + " ; sourceAbsoluteTop = "
                + sourceAbsoluteTop + " ; sourceOffsetHeight = " + sourceOffsetHeight + " ; sourceOffsetWidth = " + sourceOffsetWidth;
    }

    public enum NativeButton {

        BUTTON_LEFT(1),
        BUTTON_MIDDLE(4),
        BUTTON_RIGHT(2);

        private int value;

        private NativeButton(final int value) {
            this.value = value;
        }

        public static final NativeButton fromValue(final int value) {
            for (final NativeButton button : values()) {
                if (button.value == value) return button;
            }
            throw new IllegalArgumentException("No button with this value : " + value);
        }
    }

}
