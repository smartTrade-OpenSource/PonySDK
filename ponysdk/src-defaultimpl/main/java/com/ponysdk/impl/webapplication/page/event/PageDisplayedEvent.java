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

package com.ponysdk.impl.webapplication.page.event;

import com.ponysdk.core.event.Event;
import com.ponysdk.core.event.EventHandler;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.impl.webapplication.page.event.PageDisplayedEvent.PageDisplayHandler;

public class PageDisplayedEvent extends Event<PageDisplayHandler> {

    public interface PageDisplayHandler extends EventHandler {

        public void onPageDisplayed(PageDisplayedEvent event);
    }

    public static final Event.Type<PageDisplayHandler> TYPE = new Event.Type<PageDisplayHandler>();

    private final PageActivity page;

    public PageDisplayedEvent(Object sourceComponent, PageActivity page) {
        super(sourceComponent);
        this.page = page;
    }

    @Override
    protected void dispatch(PageDisplayHandler handler) {
        handler.onPageDisplayed(this);
    }

    @Override
    public Event.Type<PageDisplayHandler> getAssociatedType() {
        return TYPE;
    }

    public PageActivity getPageActivity() {
        return page;
    }

}
