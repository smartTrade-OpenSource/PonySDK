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

package com.ponysdk.core.ui.basic;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.WidgetType;

/**
 * Base class for panels that contain only one widget.
 */
public class PSimplePanel extends PPanel implements PAcceptsOneWidget {

    private static final Logger log = LoggerFactory.getLogger(PSimplePanel.class);

    private PWidget widget;

    protected PSimplePanel() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SIMPLE_PANEL;
    }

    @Override
    public void add(final PWidget w) {
        log.error("Use setWidget(IsWidget w)");
        throw new UnsupportedOperationException("Use setWidget(IsWidget w)");
    }

    public PWidget getWidget() {
        return widget;
    }

    @Override
    public void setWidget(final IsPWidget w) {
        setWidget(w.asWidget());
    }

    @Override
    public boolean remove(final PWidget w) {
        // Validate.
        if (widget == null || widget != w) {
            return false;
        }

        // Orphan.
        try {
            orphan(w);
        } finally {
            // Physical detach.
            w.saveRemove(w.getID(), ID);
            // Logical detach.
            widget = null;
        }
        return true;
    }

    public void setWidget(final PWidget w) {
        if (w == this) throw new UnsupportedOperationException("You cannot call setWidget with 'this' in parameter");

        // Validate
        if (w == widget) return;

        // Detach new child.
        w.removeFromParent();

        // Remove old child.
        if (widget != null) {
            remove(widget);
        }

        // Logical attach.
        widget = w;

        if (w != null) {
            // Physical attach.
            w.saveAdd(w.getID(), ID);
            w.attach(window);
            adopt(w);
        }
    }

    @Override
    public Iterator<PWidget> iterator() {
        return new Iterator<PWidget>() {

            boolean hasElement = widget != null;

            PWidget returned = null;

            @Override
            public boolean hasNext() {
                return hasElement;
            }

            @Override
            public PWidget next() {
                if (!hasElement || widget == null) {
                    throw new NoSuchElementException();
                }
                hasElement = false;
                return returned = widget;
            }

            @Override
            public void remove() {
                if (returned != null) {
                    PSimplePanel.this.remove(returned);
                }
            }
        };
    }

}
