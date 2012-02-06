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

package com.ponysdk.ui.server.list;

import java.util.ArrayList;
import java.util.List;

public class ListConfiguration<T> {

    private List<ListColumnDescriptor<T, ?>> columnDescriptors = new ArrayList<ListColumnDescriptor<T, ?>>();

    private String tableName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<ListColumnDescriptor<T, ?>> getColumnDescriptors() {
        return columnDescriptors;
    }

    public void setColumnDescriptors(List<ListColumnDescriptor<T, ?>> fields) {
        this.columnDescriptors = fields;
    }

    public void addColumnDescriptor(ListColumnDescriptor<T, ?> listColumnDescriptor) {
        columnDescriptors.add(listColumnDescriptor);
    }
}
