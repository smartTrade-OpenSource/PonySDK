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

package com.ponysdk.impl.webapplication.footer;

import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.alignment.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.alignment.PVerticalAlignment;

public class DefaultFooterView extends PSimplePanel implements FooterView {

    private final PVerticalPanel content = new PVerticalPanel();

    public DefaultFooterView(final String copyright) {
        setSizeFull();
        content.setSizeFull();

        final PLabel label = new PLabel(copyright);
        content.add(label);
        content.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_CENTER);
        content.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_MIDDLE);
        setWidget(content);
    }

}
