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

package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;

public class PScrollEvent extends Event<PScrollEvent.PScrollHandler> {

    public static final Type TYPE = new Type();

    private final int height;
    private final int width;
    private final int verticalPostion;
    private final int horizontalPosition;

    public PScrollEvent(final Object source, final int height, final int width, final int verticalPostion,
                        final int horizontalPosition) {
        super(source);
        this.height = height;
        this.width = width;
        this.verticalPostion = verticalPostion;
        this.horizontalPosition = horizontalPosition;
    }

    @Override
    public Type getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PScrollHandler handler) {
        handler.onScroll(this);
    }

    public boolean isScrollTop() {
        return verticalPostion == 0;
    }

    public boolean isScrollBottom() {
        return verticalPostion == height;
    }

    public boolean isScrollLeft() {
        return horizontalPosition == 0;
    }

    public boolean isScrollRight() {
        return horizontalPosition == width;
    }

    public double getVerticalScrollFactor() {
        return verticalPostion / (double) height;
    }

    public double getHorizontalScrollFactor() {
        return horizontalPosition / (double) width;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getVerticalPostion() {
        return verticalPostion;
    }

    public int getHorizontalPosition() {
        return horizontalPosition;
    }

    @FunctionalInterface
    public interface PScrollHandler extends EventHandler {

        void onScroll(PScrollEvent event);
    }

}
