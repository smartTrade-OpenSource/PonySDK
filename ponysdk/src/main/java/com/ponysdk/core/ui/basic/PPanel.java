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

package com.ponysdk.core.ui.basic;

import java.util.Iterator;

import com.ponysdk.core.ui.basic.event.HasPWidgets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all panels, which are widgets that can contain other widgets.
 */
public abstract class PPanel extends PWidget implements HasPWidgets {

    private static final Logger log = LoggerFactory.getLogger(PPanel.class);

    PPanel() {
    }

    @Override
    public abstract boolean remove(PWidget child);

    @Override
    public void add(final PWidget child) {
        throw new UnsupportedOperationException("This panel does not support no-arg add()");
    }

    @Override
    public void add(final IsPWidget child) {
        add(child.asWidget());
    }

    @Override
    void init0() {
        super.init0();
        forEach(widget -> widget.attach(window, frame));
    }

    protected final void adopt(final PWidget child) {
        if (child.getParent() == null) child.setParent(this);
        else throw new IllegalStateException("Can't adopt an already widget attached to a parent");
    }

    final void orphan(final PWidget child) {
        if (child == null) {
        } else if (child.getParent() == this) child.setParent(null);
        else throw new IllegalStateException("Can't adopt an widget attached to another parent");
    }

    @Override
    public void clear() {
        final Iterator<PWidget> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        forEach(this::doDestroy);
    }

    private void doDestroy(PWidget child){
        try {
            child.onDestroy();
        } catch (Exception e) {
            log.error("An error occurred while trying to process the destroy event", e);
        }
    }

}
