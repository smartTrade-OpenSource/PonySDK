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

import elemental2.dom.Document;
import elemental2.dom.Window;

import jsinterop.base.Js;

public abstract class PTAbstractWindow extends AbstractPTObject {

    protected Window window;

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.PRINT == model) {
            window.print();
            return true;
        } else if (ServerToClientModel.RELOAD == model) {
            window.location.reload();
            return true;
        } else if (ServerToClientModel.WINDOW_LOCATION_REPLACE == model) {
            window.location.replace(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.RESIZE_BY_X == model) {
            final int x = (int) binaryModel.getDoubleValue();
            final int y = (int) buffer.readBinaryModel().getDoubleValue();
            window.resizeBy(x, y);
            return true;
        } else if (ServerToClientModel.RESIZE_TO_WIDTH == model) {
            final int width = binaryModel.getIntValue();
            final int height = buffer.readBinaryModel().getIntValue();
            window.resizeTo(width, height);
            return true;
        } else if (ServerToClientModel.MOVE_BY_X == model) {
            final int x = (int) binaryModel.getDoubleValue();
            final int y = (int) buffer.readBinaryModel().getDoubleValue();
            window.moveBy(x, y);
            return true;
        } else if (ServerToClientModel.MOVE_TO_X == model) {
            final int x = (int) binaryModel.getDoubleValue();
            final int y = (int) buffer.readBinaryModel().getDoubleValue();
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

    public static void setTitle(final String title, final Window window) {
        final Object doc = Js.asPropertyMap(window).get("document");
        Js.asPropertyMap(doc).set("title", title);
    }

    public static boolean isIntersectionObserverAPI(final Window window) {
        return Js.asPropertyMap(window).has("IntersectionObserver");
    }

    public static boolean isPageVisibilityAPI(final Document document) {
        return Js.asPropertyMap(document).has("hidden");
    }

    public static boolean isDocumentVisible(final Document document) {
        return !Js.isTruthy(Js.asPropertyMap(document).get("hidden"));
    }

}
