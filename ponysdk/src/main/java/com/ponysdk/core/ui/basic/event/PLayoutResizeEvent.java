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

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.core.ui.eventbus.EventHandler;

public class PLayoutResizeEvent extends Event<PLayoutResizeEvent.Handler> {

    public static final Type TYPE = new Type();

    private final List<LayoutResizeData> layoutResizeData = new ArrayList<>();

    @FunctionalInterface
    public interface Handler extends EventHandler {

        void onLayoutResize(PLayoutResizeEvent resizeEvent);
    }

    public PLayoutResizeEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    public void addLayoutResizeData(final LayoutResizeData d) {
        layoutResizeData.add(d);
    }

    @Override
    public Type getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PLayoutResizeEvent.Handler handler) {
        handler.onLayoutResize(this);
    }

    public List<LayoutResizeData> getLayoutResizeData() {
        return layoutResizeData;
    }

    public static class LayoutResizeData {

        private final PWidget widget;
        private final double size;

        public LayoutResizeData(final PWidget widget, final double size) {
            this.widget = widget;
            this.size = size;
        }

        public PWidget getWidget() {
            return widget;
        }

        public double getSize() {
            return size;
        }

    }

}
