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

package com.ponysdk.sample.client.page;

import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.basic.PRichTextArea;
import com.ponysdk.ui.server.basic.PVerticalPanel;

public class RichTextAreaPageActivity extends PageActivity {

    public RichTextAreaPageActivity() {
        super("RichTextArea", "Basics UI Components");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(Place place) {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {

        final PVerticalPanel layout = new PVerticalPanel();
        pageView.getBody().setWidget(layout);

        final PRichTextArea ritchTextArea = new PRichTextArea();

        layout.add(ritchTextArea);

    }
}
