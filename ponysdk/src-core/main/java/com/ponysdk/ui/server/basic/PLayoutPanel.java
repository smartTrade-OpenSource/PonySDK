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

import com.ponysdk.core.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.PUnit;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.basic.PAlignment;
import com.ponysdk.ui.terminal.model.Model;

/**
 * A panel that lays its children
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it is run
 * have an explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 */
public class PLayoutPanel extends PComplexPanel implements PAnimatedLayout {

    public PLayoutPanel(final PWindow window) {
        super(window);
        init();
    }

    public PLayoutPanel() {
        this(null);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.LAYOUT_PANEL;
    }

    public void setWidgetHorizontalPosition(final PWidget child, final PAlignment position) {
        assertIsChild(child);

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.HORIZONTAL_ALIGNMENT, position.ordinal());
        parser.comma();
        parser.parse(Model.WIDGET_ID, child.getID());
        parser.endObject();
    }

    public void setWidgetVerticalPosition(final PWidget child, final PAlignment position) {
        assertIsChild(child);

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.WIDGET_ID, child.getID());
        parser.comma();
        parser.parse(Model.VERTICAL_ALIGNMENT, position.ordinal());
        parser.endObject();
    }

    public void setWidgetHidden(final PWidget widget, final boolean hidden) {
        assertIsChild(widget);

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.WIDGET_HIDDEN, hidden);
        parser.comma();
        parser.parse(Model.WIDGET_ID, widget.getID());
        parser.endObject();
    }

    public void setWidgetLeftRight(final PWidget child, final double left, final double right, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, Model.LEFT, left, Model.RIGHT, right, unit);
    }

    public void setWidgetLeftWidth(final PWidget child, final double left, final double width, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, Model.LEFT, left, Model.WIDTH, width, unit);
    }

    public void setWidgetRightWidth(final PWidget child, final double right, final double width, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, Model.RIGHT, right, Model.WIDTH, width, unit);
    }

    public void setWidgetTopBottom(final PWidget child, final double top, final double bottom, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, Model.TOP, top, Model.BOTTOM, bottom, unit);
    }

    public void setWidgetTopHeight(final PWidget child, final double top, final double height, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, Model.TOP, top, Model.HEIGHT, height, unit);
    }

    public void setWidgetBottomHeight(final PWidget child, final double bottom, final double height, final PUnit unit) {
        assertIsChild(child);
        sendUpdate(child, Model.BOTTOM, bottom, Model.HEIGHT, height, unit);
    }

    private void sendUpdate(final PWidget child, final Model key1, final double v1, final Model key2, final double v2, final PUnit unit) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.comma();
        parser.parse(Model.OBJECT_ID, ID);
        if (window != null) {
            parser.comma();
            parser.parse(Model.WINDOW_ID, window.getID());
        }
        parser.comma();
        parser.parse(Model.UNIT, unit.ordinal());
        parser.comma();
        parser.parse(Model.WIDGET_ID, child.getID());
        parser.comma();
        parser.parse(key1, v1);
        parser.comma();
        parser.parse(key2, v2);
        parser.endObject();
    }

    @Override
    public void animate(final int duration) {
        saveUpdate(Model.ANIMATE, duration);
    }
}
