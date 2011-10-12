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

public class RowDeletedEvent extends SystemEvent<RowDeletedHandler> {

    public static final Type<RowDeletedHandler> TYPE = new Type<RowDeletedHandler>();

    public int deletedRowCount;
    public int row;

    public RowDeletedEvent(Object sourceComponent) {
        super(sourceComponent);
    }

    public RowDeletedEvent(Object sourceComponent, int row, int deletedRowCount) {
        super(sourceComponent);
        this.deletedRowCount = deletedRowCount;
        this.row = row;
    }

    @Override
    protected void dispatch(RowDeletedHandler handler) {
        handler.onRowDeleted(this);
    }

    @Override
    public Type<RowDeletedHandler> getAssociatedType() {
        return TYPE;
    }

    public int getDeletedRowCount() {
        return deletedRowCount;
    }

    public int getRow() {
        return row;
    }

}
