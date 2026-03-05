/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.wa.datatable;

import java.util.Optional;

/**
 * Defines a column in a {@link PDataTable}.
 *
 * @param field      the data field name this column maps to
 * @param header     the display header text
 * @param type       the column data type ("string", "number", "date", "boolean")
 * @param sortable   whether the column supports sorting
 * @param filterable whether the column supports filtering
 * @param width      optional column width in pixels
 */
public record ColumnDef(
    String field,
    String header,
    String type,
    boolean sortable,
    boolean filterable,
    Optional<Integer> width
) {
    /**
     * Creates a simple non-sortable, non-filterable string column.
     */
    public static ColumnDef of(final String field, final String header) {
        return new ColumnDef(field, header, "string", false, false, Optional.empty());
    }

    /**
     * Creates a sortable string column.
     */
    public static ColumnDef sortable(final String field, final String header) {
        return new ColumnDef(field, header, "string", true, false, Optional.empty());
    }
}
