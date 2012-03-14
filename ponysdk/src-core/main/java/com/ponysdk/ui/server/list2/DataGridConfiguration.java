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

package com.ponysdk.ui.server.list2;

import java.util.ArrayList;
import java.util.List;

public class DataGridConfiguration<T> {

    private List<DataGridColumnDescriptor<T, ?>> columnDescriptors = new ArrayList<DataGridColumnDescriptor<T, ?>>();

    private boolean selectionEnabled = false;
    private boolean showSubListEnabled = false;

    public List<DataGridColumnDescriptor<T, ?>> getColumnDescriptors() {
        return columnDescriptors;
    }

    public void setColumnDescriptors(final List<DataGridColumnDescriptor<T, ?>> fields) {
        this.columnDescriptors = fields;
    }

    public void addColumnDescriptor(final DataGridColumnDescriptor<T, ?> listColumnDescriptor) {
        columnDescriptors.add(listColumnDescriptor);
    }

    public boolean isSelectionEnabled() {
        return selectionEnabled;
    }

    public void setSelectionEnabled(final boolean selectionEnabled) {
        this.selectionEnabled = selectionEnabled;
    }

    public boolean isShowSubListEnabled() {
        return showSubListEnabled;
    }

    public void setShowSubListEnabled(final boolean showSubListEnabled) {
        this.showSubListEnabled = showSubListEnabled;
    }

}
