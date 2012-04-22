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
import com.ponysdk.core.query.SortingType;

public class SortColumnEvent extends PSystemEvent<SortColumnHandler> {

    public static final Type<SortColumnHandler> TYPE = new Type<SortColumnHandler>();

    private final SortingType sortingType;

    private final String pojoPropertyKey;

    public SortColumnEvent(Object sourceComponent, SortingType sortingType, String pojoPropertyKey) {
        super(sourceComponent);
        this.sortingType = sortingType;
        this.pojoPropertyKey = pojoPropertyKey;
    }

    @Override
    protected void dispatch(SortColumnHandler handler) {
        handler.onColumnSort(this);
    }

    @Override
    public Type<SortColumnHandler> getAssociatedType() {
        return TYPE;
    }

    public SortingType getSortingType() {
        return sortingType;
    }

    public String getPojoPropertyKey() {
        return pojoPropertyKey;
    }

}
