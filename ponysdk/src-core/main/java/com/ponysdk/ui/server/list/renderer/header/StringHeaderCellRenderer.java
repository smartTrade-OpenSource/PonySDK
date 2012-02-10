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

package com.ponysdk.ui.server.list.renderer.header;

import com.ponysdk.core.event.EventBus;
import com.ponysdk.core.event.EventBusAware;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PLabel;

public class StringHeaderCellRenderer implements HeaderCellRenderer, EventBusAware {

    private final PLabel caption;
    private final String pojoPropertyKey;

    public StringHeaderCellRenderer(final String caption) {
        this(caption, null);
    }

    public StringHeaderCellRenderer(final String caption, final String pojoPropertyKey) {
        this.caption = new PLabel(caption);
        this.pojoPropertyKey = pojoPropertyKey;
    }

    @Override
    public IsPWidget render() {
        return caption;
    }

    @Override
    public String getCaption() {
        return caption.getText();
    }

    @Override
    public void setEventBus(final EventBus eventBus) {
        if (pojoPropertyKey != null) {
            SortableHeader sortableHeader = new SortableHeader(caption, pojoPropertyKey);
            sortableHeader.setEventBus(eventBus);
        }
    }
}
