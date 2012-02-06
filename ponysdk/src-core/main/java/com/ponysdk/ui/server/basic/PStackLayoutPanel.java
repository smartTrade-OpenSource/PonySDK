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

package com.ponysdk.ui.server.basic;

import java.util.Iterator;

import com.google.gwt.dom.client.Style.Unit;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Remove;

public class PStackLayoutPanel extends PComposite implements HasPWidgets {

    private final PWidgetCollection children = new PWidgetCollection(this);

    public PStackLayoutPanel(Unit unit) {
        super();
        initWidget(new PLayoutPanel());
        create.getMainProperty().setProperty(PropertyKey.UNIT, unit.toString());
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.STACKLAYOUT_PANEL;
    }

    public void add(final PWidget child, String header, boolean asHtml, double headerSize) {
        child.removeFromParent();
        children.add(child);
        adopt(child);

        final Add add = new Add(child.getID(), getID());
        add.getMainProperty().setProperty(PropertyKey.HTML, header);
        add.getMainProperty().setProperty(PropertyKey.SIZE, headerSize);
        getPonySession().stackInstruction(add);
    }

    @Override
    public boolean remove(PWidget child) {
        if (child.getParent() != this) { return false; }
        orphan(child);

        children.remove(child);

        final Remove remove = new Remove(child.getID(), getID());
        getPonySession().stackInstruction(remove);
        return true;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return children.iterator();
    }

    @Override
    public void add(PWidget w) {
        assert false : "Single-argument add() is not supported for this widget";
    }

    @Override
    public void add(IsPWidget w) {
        add(w.asWidget());
    }

    @Override
    public void clear() {
        final Iterator<PWidget> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private void adopt(PWidget child) {
        assert (child.getParent() == null);
        child.setParent(this);
    }

    private void orphan(PWidget child) {
        assert (child.getParent() == this);
        child.setParent(null);
    }

}
