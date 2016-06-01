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

import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.server.model.ServerBinaryModel;

/**
 * Abstract base class for panels that can contain multiple child widgets.
 */
public abstract class PComplexPanel extends PPanel {

    protected PWidgetCollection children = new PWidgetCollection(this);

    public void add(final PWidget... widgets) {
        for (final PWidget w : widgets) {
            add(w);
        }
    }

    @Override
    public void add(final PWidget child) {
        assertNotMe(child);

        if (child.getWindowID() == PWindow.EMPTY_WINDOW_ID || child.getWindowID() == windowID) {
            child.removeFromParent();
            children.add(child);
            adopt(child);

            child.saveAdd(child.getID(), ID);
            child.attach(windowID);
        } else {
            throw new IllegalAccessError("Widget " + child + " already attached to an other window, current window : " + child.getWindowID() + ", new window : " + windowID);
        }
    }

    public void insert(final PWidget child, final int beforeIndex) {
        assertNotMe(child);

        if (child.getWindowID() == PWindow.EMPTY_WINDOW_ID || child.getWindowID() == windowID) {
            child.removeFromParent();

            children.insert(child, beforeIndex);
            adopt(child);

            if (children.size() - 1 == beforeIndex) {
                child.saveAdd(child.getID(), ID);
                child.attach(windowID);
            } else {
                child.saveAdd(child.getID(), ID, new ServerBinaryModel(ServerToClientModel.INDEX, beforeIndex));
                child.attach(windowID);
            }
        } else {
            throw new IllegalAccessError("Widget " + child + " already attached to an other window, current window : " + child.getWindowID() + ", new window : " + windowID);
        }
    }

    @Override
    public boolean remove(final PWidget w) {
        if (children.remove(w)) {
            orphan(w);
            saveRemove(w.getID(), ID);
            return true;
        }

        return false;
    }

    public boolean remove(final int index) {
        return remove(getWidget(index));
    }

    protected PWidget getChild(final long objectID) {
        for (final PWidget w : children) {
            if (w.getID() == objectID) return w;
        }
        return null;
    }

    public int getWidgetCount() {
        return children.size();
    }

    public PWidget getWidget(final int index) {
        return children.get(index);
    }

    public int getWidgetIndex(final PWidget child) {
        return children.indexOf(child);
    }

    @Override
    public Iterator<PWidget> iterator() {
        return children.iterator();
    }

    void assertIsChild(final PWidget widget) {
        if (widget != null && widget.getParent() != this) throw new IllegalStateException("The specified widget is not a child of this panel");
    }

    void assertNotMe(final PWidget widget) {
        if (widget == this) throw new IllegalStateException("Cannot insert widget to itself");
    }

}
