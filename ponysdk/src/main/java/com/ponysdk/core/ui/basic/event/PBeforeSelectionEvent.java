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

public class PBeforeSelectionEvent<T> extends Event<PBeforeSelectionEvent.Handler<T>> {

    public static final Type TYPE = new Type();
    private T selectedItem;

    public PBeforeSelectionEvent(final Object source, final T selectedItem) {
        super(source);
        this.selectedItem = selectedItem;
    }

    public T getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(final T item) {
        this.selectedItem = item;
    }

    @Override
    public Type getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PBeforeSelectionEvent.Handler<T> handler) {
        handler.onBeforeSelection(this);
    }

    @FunctionalInterface
    public interface Handler<T> extends EventHandler {

        void onBeforeSelection(PBeforeSelectionEvent<T> event);

    }

}
