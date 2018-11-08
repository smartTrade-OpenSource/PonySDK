/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.terminal.ui;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.html.Window;

public abstract class PTAbstractWindow extends AbstractPTObject {

    protected Window window;

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.PRINT == model) {
            window.print();
            return true;
        } else if (ServerToClientModel.RELOAD == model) {
            window.getLocation().reload();
            return true;
        } else if (ServerToClientModel.WINDOW_LOCATION_REPLACE == model) {
            window.getLocation().replace(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.RESIZE_BY_X == model) {
            final float x = (float) binaryModel.getDoubleValue();
            final float y = (float) buffer.readBinaryModel().getDoubleValue();
            window.resizeBy(x, y);
            return true;
        } else if (ServerToClientModel.RESIZE_TO_WIDTH == model) {
            final int width = binaryModel.getIntValue();
            final int height = buffer.readBinaryModel().getIntValue();
            window.resizeTo(width, height);
            return true;
        } else if (ServerToClientModel.MOVE_BY_X == model) {
            final float x = (float) binaryModel.getDoubleValue();
            final float y = (float) buffer.readBinaryModel().getDoubleValue();
            window.moveBy(x, y);
            return true;
        } else if (ServerToClientModel.MOVE_TO_X == model) {
            final float x = (float) binaryModel.getDoubleValue();
            final float y = (float) buffer.readBinaryModel().getDoubleValue();
            window.moveTo(x, y);
            return true;
        } else if (ServerToClientModel.FOCUS == model) {
            if (binaryModel.getBooleanValue()) window.focus();
            else window.blur();
            return true;
        } else if (ServerToClientModel.WINDOW_TITLE == model) {
            setTitle(binaryModel.getStringValue(), window);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    public final native void setTitle(String title, Window window) /*-{
                                                                   window.document.title = title;
                                                                   }-*/;

}
