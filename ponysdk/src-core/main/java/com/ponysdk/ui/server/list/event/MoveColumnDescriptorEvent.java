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

import java.util.List;

import com.ponysdk.core.event.SystemEvent;

public class MoveColumnDescriptorEvent extends SystemEvent<MoveColumnDescriptorHandler> {

    public static final Type<MoveColumnDescriptorHandler> TYPE = new Type<MoveColumnDescriptorHandler>();

    private final List<String> columnOrder;

    private final String tableName;

    public MoveColumnDescriptorEvent(Object sourceComponent, List<String> columnOrder, String tableName) {
        super(sourceComponent);
        this.columnOrder = columnOrder;
        this.tableName = tableName;
    }

    @Override
    protected void dispatch(MoveColumnDescriptorHandler handler) {
        handler.onMoveColumn(this);
    }

    @Override
    public Type<MoveColumnDescriptorHandler> getAssociatedType() {
        return TYPE;
    }

    public List<String> getColumnOrder() {
        return columnOrder;
    }

    public String getTableName() {
        return tableName;
    }

}
