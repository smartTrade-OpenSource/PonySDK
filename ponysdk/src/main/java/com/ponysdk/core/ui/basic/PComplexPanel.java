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

import java.util.Collections;
import java.util.Iterator;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.model.ServerBinaryModel;

/**
 * Abstract base class for panels that can contain multiple child widgets.
 */
public abstract class PComplexPanel extends PPanel {

    protected PWidgetCollection children;

    protected PComplexPanel() {
    }

    public void add(final PWidget... widgets) {
        for (final PWidget w : widgets) {
            add(w);
        }
    }

    @Override
    protected boolean attach(final PWindow window, final PFrame frame) {
        this.frame = frame;

        if (this.window == null && window != null) {
            this.window = window;
            init();
            return true;
        } else if (this.window != window) {
            throw new IllegalAccessError(
                "Widget already attached to an other window, current window : #" + this.window + ", new window : #" + window);
        }

        return false;
    }

    @Override
    public void add(final PWidget child) {
        assertNotMe(child);

        if (child.getWindow() == null || child.getWindow() == window) {
            child.removeFromParent();

            if (children == null) children = new PWidgetCollection(this);
            children.add(child);

            adopt(child);
            if (isInitialized()) child.attach(window, frame);

            child.saveAdd(child.getID(), ID);
        } else {
            if (initialized) {
                throw new IllegalAccessError(
                    "Can't attach widget " + child + " to window #" + window + " because it's already attached to window #" + child);
            } else {
                throw new IllegalAccessError("Can't only attach widget " + child + " to window #" + child.getWindow()
                        + ". Need to attach the new parent to the same window before");
            }
        }
    }

    public void insert(final PWidget child, final int beforeIndex) {
        assertNotMe(child);

        if (child.getWindow() == null || child.getWindow() == window) {
            child.removeFromParent();

            if (children == null) children = new PWidgetCollection(this);
            children.insert(child, beforeIndex);

            adopt(child);
            if (isInitialized()) child.attach(window, frame);

            if (!isInitialized() || children.size() - 1 == beforeIndex) child.saveAdd(child.getID(), ID);
            else child.saveAdd(child.getID(), ID, new ServerBinaryModel(ServerToClientModel.INDEX, beforeIndex));
        } else {
            throw new IllegalAccessError("Widget " + child + " already attached to an other window, current window : "
                    + child.getWindow() + ", new window : " + window);
        }
    }

    @Override
    public boolean remove(final PWidget child) {
        if (children != null && child != null && children.remove(child)) {
            orphan(child);
            child.saveRemove(child.getID(), ID);
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(final int index) {
        return remove(getWidget(index));
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
