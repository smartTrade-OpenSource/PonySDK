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

package com.ponysdk.ui.server.list.event;

import com.ponysdk.core.event.PSystemEvent;

public class ShowSubListEvent<T> extends PSystemEvent<ShowSubListHandler<T>> {

    public static final Type<ShowSubListHandler<?>> TYPE = new Type<ShowSubListHandler<?>>();

    private final T data;

    private final boolean show;

    private final int row;

    public ShowSubListEvent(Object sourceComponent, T data, boolean show, int row) {
        super(sourceComponent);
        this.data = data;
        this.show = show;
        this.row = row;
    }

    @Override
    protected void dispatch(ShowSubListHandler<T> handler) {
        handler.onShowSubList(this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Type<ShowSubListHandler<T>> getAssociatedType() {
        return (Type) TYPE;
    }

    @Override
    public T getData() {
        return data;
    }

    public boolean isShow() {
        return show;
    }

    public int getRow() {
        return row;
    }

}
