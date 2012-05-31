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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.instruction.Create;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A type of widget that can wrap another widget, hiding the wrapped widget's methods. When added to a panel,
 * a composite behaves exactly as if the widget it wraps had been added.
 * <p>
 * The composite is useful for creating a single widget out of an aggregate of multiple other widgets
 * contained in a single panel.
 * </p>
 */
public abstract class PComposite extends PWidget {

    private PWidget widget;

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.COMPOSITE;
    }

    @Override
    protected void init(final WidgetType widgetType) {}

    protected void initWidget(final PWidget child) {
        if (this.widget != null) { throw new IllegalStateException("PComposite.initWidget() may only be " + "called once."); }
        ID = PonySession.getCurrent().nextID();
        create = new Create(ID, getWidgetType());

        PonySession.getCurrent().stackInstruction(create);
        PonySession.getCurrent().registerObject(this);

        child.removeFromParent();
        this.widget = child;
        child.setParent(this);

        create.put(PROPERTY.WIDGET, child.getID());

        getPonySession().stackInstruction(create);
    }

}
