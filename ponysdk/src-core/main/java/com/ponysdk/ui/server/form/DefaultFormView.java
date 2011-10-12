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
package com.ponysdk.ui.server.form;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.HasPWidgets;

public class DefaultFormView extends PSimplePanel implements FormView {

    private static final String CSS_SMARTFORM = "Form";

    private final List<FormField> fields = new ArrayList<FormField>();
    private final HasPWidgets layout;
    private final PHorizontalPanel actionPanel = new PHorizontalPanel();

    private boolean isFirstInsertInActionPanel = true;

    public DefaultFormView(String caption, PPanel layout) {
        this.layout = layout;
        addStyleName(CSS_SMARTFORM);
        actionPanel.setSizeFull();
        layout.add(actionPanel);
        setWidget(layout);
    }

    public DefaultFormView(PPanel layout) {
        this(null, layout);
    }

    public DefaultFormView() {
        this(null, new PVerticalPanel());
    }

    public DefaultFormView(String caption) {
        this(caption, new PVerticalPanel());
    }

    public void addWidgetToActionPanel() {
        if (isFirstInsertInActionPanel) {
            isFirstInsertInActionPanel = false;
            final PLabel separator = new PLabel("|");
            actionPanel.add(separator);
        }
    }

    public boolean isValid() {
        for (final FormField field : fields) {
            if (!field.isValid())
                return false;
        }
        return true;
    }

    public void reset() {
        for (final FormField field : fields) {
            field.reset();
        }
    }

    @Override
    public void addFormField(IsPWidget w) {
        layout.add(w.asWidget());
    }

    @Override
    public void removeFormField(IsPWidget w) {
        layout.remove(w.asWidget());
    }

}
