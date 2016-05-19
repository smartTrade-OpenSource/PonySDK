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

import com.ponysdk.ui.server.basic.event.HasPWidgets;

/**
 * Abstract base class for all panels, which are widgets that can contain other widgets.
 */
public abstract class PPanel extends PWidget implements HasPWidgets {

    public PPanel() {
    }

    public PPanel(final int windowID) {
        super(windowID);
    }

    @Override
    public abstract boolean remove(PWidget child);

    @Override
    public void add(final PWidget child) {
        throw new UnsupportedOperationException("This panel does not support no-arg add()");
    }

    @Override
    public void add(final IsPWidget w) {
        add(w.asWidget());
    }

    @Override
    protected boolean attach(final int windowID) {
        final boolean result = super.attach(windowID);

        final Iterator<PWidget> it = iterator();
        while (it.hasNext()) {
            final PWidget child = it.next();
            if (child.attach(windowID)) executeAdd(child.getID(), ID);
        }

        return result;
    }

    protected final void adopt(final PWidget child) {
        assert child.getParent() == null;
        child.setParent(this);
    }

    protected final void orphan(final PWidget child) {
        assert child.getParent() == this;
        child.setParent(null);
    }

    @Override
    public void clear() {
        final Iterator<PWidget> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

}
