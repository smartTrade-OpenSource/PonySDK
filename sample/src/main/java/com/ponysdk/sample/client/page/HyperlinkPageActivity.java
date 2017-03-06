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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.impl.webapplication.page.place.PagePlace;

public class HyperlinkPageActivity extends SamplePageActivity {

    public HyperlinkPageActivity() {
        super("Hyperlink", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = Element.newPVerticalPanel();
        panel.setSpacing(10);

        panel.add(Element.newPLabel("Choose a section:"));

        final PAnchor checkBoxAnchor = Element.newPAnchor("CheckBox");
        checkBoxAnchor.addClickHandler(event -> goTo(new PagePlace("CheckBox")));
        panel.add(checkBoxAnchor);

        final PAnchor radioButtonAnchor = Element.newPAnchor("RadioButton");
        radioButtonAnchor.addClickHandler(event -> goTo(new PagePlace("Radio Button")));
        panel.add(radioButtonAnchor);

        final PAnchor basicButtonAnchor = Element.newPAnchor("BasicButton");
        basicButtonAnchor.addClickHandler(event -> goTo(new PagePlace("Basic Button")));
        panel.add(basicButtonAnchor);

        final PAnchor customButtonAnchor = Element.newPAnchor("CustomButton");
        customButtonAnchor.addClickHandler(event -> goTo(new PagePlace("Custom Button")));
        panel.add(customButtonAnchor);

        final PAnchor fileUpload = Element.newPAnchor("FileUpload");
        fileUpload.addClickHandler(event -> goTo(new PagePlace("File Upload")));
        panel.add(fileUpload);

        examplePanel.setWidget(panel);
    }
}
