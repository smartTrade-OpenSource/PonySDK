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

import com.ponysdk.core.event.SystemEvent;

public class ColumnDescriptorRemovedEvent extends SystemEvent<ColumnDescriptorRemovedHandler> {

    public static final Type<ColumnDescriptorRemovedHandler> TYPE = new Type<ColumnDescriptorRemovedHandler>();

    private final String caption;

    private final String tableName;

    public ColumnDescriptorRemovedEvent(Object sourceComponent, String caption, String tableName) {
        super(sourceComponent);
        this.caption = caption;
        this.tableName = tableName;
    }

    @Override
    protected void dispatch(ColumnDescriptorRemovedHandler handler) {
        handler.onColumnRemoved(this);
    }

    @Override
    public Type<ColumnDescriptorRemovedHandler> getAssociatedType() {
        return TYPE;
    }

    public String getCaption() {
        return caption;
    }

    public String getTableName() {
        return tableName;
    }

}
