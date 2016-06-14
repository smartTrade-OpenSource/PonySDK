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

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.Parser;

public abstract class PAddOnComposite<T extends PWidget> extends PAddOn implements IsPWidget {

    protected T widget;

    public PAddOnComposite(final T w) {
        widget = w;

        if (PWindow.EMPTY_WINDOW_ID != widget.getWindowID()) {
            attach(widget.getWindowID());
        } else {
            widget.setAttachListener(() -> attach(widget.getWindowID()));

            PWindowManager.addWindowListener(new PWindowManager.RegisterWindowListener() {

                @Override
                public void registered(final int windowID) {
                    if (windowID == widget.getWindowID()) {
                        attach(widget.getWindowID());
                    }
                }

                @Override
                public void unregistered(final int windowID) {
                }
            });
        }
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        if (widget != null) {
            parser.parse(ServerToClientModel.WIDGET_ID, widget.asWidget().getID());
        }
    }

    @Override
    public T asWidget() {
        return widget;
    }

}
