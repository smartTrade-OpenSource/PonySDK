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

import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.PUnit;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.basic.PAlignment;

/**
 * A panel that lays its children
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it is run
 * have an explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 */
public class PLayoutPanel extends PComplexPanel implements PAnimatedLayout {

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.LAYOUT_PANEL;
    }

    public void setWidgetHorizontalPosition(final PWidget child, final PAlignment position) {
        assertIsChild(child);
        final Update update = new Update(getID());
        update.put(PROPERTY.HORIZONTAL_ALIGNMENT, position.ordinal());
        update.put(PROPERTY.WIDGET, child.getID());
        Txn.get().getTxnContext().save(update);
    }

    public void setWidgetVerticalPosition(final PWidget child, final PAlignment position) {
        assertIsChild(child);
        final Update update = new Update(getID());
        update.put(PROPERTY.VERTICAL_ALIGNMENT, position.ordinal());
        update.put(PROPERTY.WIDGET, child.getID());
        Txn.get().getTxnContext().save(update);
    }

    public void setWidgetHidden(final PWidget widget, final boolean hidden) {
        assertIsChild(widget);
        final Update update = new Update(getID());
        update.put(PROPERTY.WIDGET_HIDDEN, hidden);
        update.put(PROPERTY.WIDGET, widget.getID());
        Txn.get().getTxnContext().save(update);
    }

    public void setWidgetLeftRight(final PWidget child, final double left, final double right, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, PROPERTY.LEFT, left, PROPERTY.RIGHT, right, unit);
    }

    public void setWidgetLeftWidth(final PWidget child, final double left, final double width, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, PROPERTY.LEFT, left, PROPERTY.WIDTH, width, unit);
    }

    public void setWidgetRightWidth(final PWidget child, final double right, final double width, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, PROPERTY.RIGHT, right, PROPERTY.WIDTH, width, unit);
    }

    public void setWidgetTopBottom(final PWidget child, final double top, final double bottom, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, PROPERTY.TOP, top, PROPERTY.BOTTOM, bottom, unit);
    }

    public void setWidgetTopHeight(final PWidget child, final double top, final double height, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, PROPERTY.TOP, top, PROPERTY.HEIGHT, height, unit);
    }

    public void setWidgetBottomHeight(final PWidget child, final double bottom, final double height, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, PROPERTY.BOTTOM, bottom, PROPERTY.HEIGHT, height, unit);
    }

    private void sendUpdate(final PWidget child, final String key1, final double v1, final String key2, final double v2, final PUnit unit) {
        final Update u = new Update(getID());
        u.put(PROPERTY.UNIT, unit.ordinal());
        u.put(PROPERTY.WIDGET, child.getID());
        u.put(key1, v1);
        u.put(key2, v2);

        Txn.get().getTxnContext().save(u);
    }

    @Override
    public void animate(final int duration) {
        saveUpdate(PROPERTY.ANIMATE, duration);
    }
}
