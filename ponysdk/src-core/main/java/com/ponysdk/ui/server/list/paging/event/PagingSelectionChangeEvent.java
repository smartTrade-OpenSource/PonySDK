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
package com.ponysdk.ui.server.list.paging.event;

import com.ponysdk.core.event.Event;

public class PagingSelectionChangeEvent extends Event<PagingSelectionChangeHandler> {

    public static final Type<PagingSelectionChangeHandler> TYPE = new Type<PagingSelectionChangeHandler>();

    private final int page;

    public PagingSelectionChangeEvent(Object sourceComponent, int page) {
        super(sourceComponent);
        this.page = page;
    }

    @Override
    protected void dispatch(PagingSelectionChangeHandler handler) {
        handler.onPageChange(this);
    }

    @Override
    public Type<PagingSelectionChangeHandler> getAssociatedType() {
        return TYPE;
    }

    public int getPage() {
        return page;
    }

}
