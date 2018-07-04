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

import javax.json.JsonObject;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;

/**
 * AddOn are used to bind server side object with javascript object
 *
 * @param <T>
 *            the linked PWidget type
 */
public abstract class PAddOnComposite<T extends PWidget> extends PAddOn implements IsPWidget {

    protected T widget;

    /**
     * Instantiates a new PAddOnComposite
     *
     * @param widget
     *            the widget
     * @param args
     *            the JsonObject arguments
     */
    protected PAddOnComposite(final T widget, final JsonObject args) {
        super(args);
        this.widget = widget;
        this.widget.bindAddon(this);

        if (null != widget.getWindow()) attach(widget.getWindow(), widget.getFrame());
        else widget.addInitializeListener(object -> attach(widget.getWindow(), widget.getFrame()));
    }

    /**
     * Instantiates a new PAddOnComposite
     *
     * @param widget
     *            the widget
     */
    protected PAddOnComposite(final T widget) {
        this(widget, null);
    }

    @Override
    public boolean attach(final PWindow window, final PFrame frame) {
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
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON_COMPOSITE;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.WIDGET_ID, widget.asWidget().getID());
    }

    @Override
    public T asWidget() {
        return widget;
    }

}
