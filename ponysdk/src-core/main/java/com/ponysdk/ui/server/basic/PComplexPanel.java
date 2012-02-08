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

import java.util.Iterator;

import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Remove;

public abstract class PComplexPanel extends PPanel {

    private final PWidgetCollection children = new PWidgetCollection(this);

    @Override
    public void add(final PWidget child) {
        insert(child, getChildren().size());
    }

    public void insert(final PWidget child, final int beforeIndex) {
        child.removeFromParent();
        getChildren().insert(child, beforeIndex);
        adopt(child);

        final Add add = new Add(child.getID(), getID());
        add.setMainPropertyValue(PropertyKey.INDEX, beforeIndex);
        getPonySession().stackInstruction(add);
    }

    @Override
    public boolean remove(final PWidget w) {
        if (w.getParent() != this) return false;
        orphan(w);
        getChildren().remove(w);

        final Remove remove = new Remove(w.getID(), getID());
        getPonySession().stackInstruction(remove);
        return true;
    }

    protected PWidgetCollection getChildren() {
        return children;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return getChildren().iterator();
    }

}
