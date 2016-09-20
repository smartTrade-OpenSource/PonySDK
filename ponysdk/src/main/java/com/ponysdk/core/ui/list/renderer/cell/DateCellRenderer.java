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

package com.ponysdk.core.ui.list.renderer.cell;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateCellRenderer extends LabelCellRenderer<Date> {

    private final DateFormat dateFormat;

    public DateCellRenderer(final DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public DateCellRenderer(final String format) {
        this(new SimpleDateFormat(format));
    }

    public DateCellRenderer(final DateFormat dateFormat, final String nullDisplay) {
        this(dateFormat);
        setNullDisplay(nullDisplay);
    }

    public DateCellRenderer(final String format, final String nullDisplay) {
        this(format);
        setNullDisplay(nullDisplay);
    }

    @Override
    public String getValue(final Date value) {
        return value != null ? dateFormat.format(value) : nullDisplay;
    }

}
