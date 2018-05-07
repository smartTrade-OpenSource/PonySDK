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

package com.ponysdk.impl.webapplication.application;

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;

public class SimpleView extends PDockLayoutPanel implements ApplicationView {

    private final PSimplePanel body = Element.newPSimpleLayoutPanel();

    public SimpleView() {
        super(PUnit.PX);
        setSizeFull();

        final PSplitLayoutPanel center = Element.newPSplitLayoutPanel();
        center.setSizeFull();
        center.add(body);

        add(center);
    }

    @Override
    public PSimplePanel getHeader() {
        return null;
    }

    @Override
    public PSimplePanel getMenu() {
        return null;
    }

    @Override
    public PSimplePanel getBody() {
        return body;
    }

    @Override
    public PSimplePanel getFooter() {
        return null;
    }

    @Override
    public PSimplePanel getLogs() {
        return null;
    }

}
