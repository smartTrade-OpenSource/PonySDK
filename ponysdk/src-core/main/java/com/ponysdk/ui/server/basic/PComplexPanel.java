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

import java.util.Collections;
import java.util.Iterator;

import com.ponysdk.ui.terminal.model.Model;

/**
 * Abstract base class for panels that can contain multiple child widgets.
 */
public abstract class PComplexPanel extends PPanel {

    protected PWidgetCollection children;

    public PComplexPanel() {
    }

    public PComplexPanel(final PWindow window) {
        super(window);
    }

    public void add(final PWidget... widgets) {
        for (final PWidget w : widgets) {
            add(w);
        }
    }

    @Override
    public void add(final PWidget child) {
        assertNotMe(child);

        child.removeFromParent();

        if (children == null) children = new PWidgetCollection(this);

        children.add(child);
        adopt(child);

        saveAdd(child.getID(), ID);
    }

    public void insert(final PWidget child, final int beforeIndex) {
        assertNotMe(child);

        child.removeFromParent();

        if (children == null) children = new PWidgetCollection(this);

        children.insert(child, beforeIndex);
        adopt(child);

        if (children.size() - 1 == beforeIndex) {
            saveAdd(child.getID(), ID);
        } else {
            saveAdd(child.getID(), ID, Model.INDEX, beforeIndex);
        }
    }

    @Override
    public boolean remove(final PWidget w) {
        if (w.getParent() == this && children != null) {
            orphan(w);
            if (children.remove(w)) {
                saveRemove(w.getID(), ID);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean remove(final int index) {
        return remove(getWidget(index));
    }

    protected PWidgetCollection getOrBuildChildrenCollection() {
        if (children == null) children = new PWidgetCollection(this);
        return children;
    }

    protected PWidget getChild(final long objectID) {
        if (children != null) {
            for (final PWidget w : children) {
                if (w.getID() == objectID) return w;
            }
        }
        return null;
    }

    public int getWidgetCount() {
        return children != null ? children.size() : 0;
    }

    public PWidget getWidget(final int index) {
        return children != null ? children.get(index) : null;
    }

    public int getWidgetIndex(final PWidget child) {
        return children != null ? children.indexOf(child) : -1;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return children != null ? children.iterator() : Collections.emptyIterator();
    }

    void assertIsChild(final PWidget widget) {
        if (widget != null && widget.getParent() != this)
            throw new IllegalStateException("The specified widget is not a child of this panel");
    }

    void assertNotMe(final PWidget widget) {
        if (widget == this) throw new IllegalStateException("Cannot insert widget to itself");
    }

}
