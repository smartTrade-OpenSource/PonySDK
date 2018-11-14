/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Label;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.util.ArrayOf;

public class PTFunctionalLabel extends PTLabel<Label> {

    private PTFunction function;

    @Override
    protected Label createUIObject() {
        return new Label();
    }

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        final BinaryModel model = buffer.readBinaryModel();
        function = (PTFunction) uiBuilder.getPTObject(model.getIntValue());
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.FUNCTION_ARGS == model) {
            final ArrayOf<JavaScriptObject> arrayValue = binaryModel.getArrayValue();
            final int length = arrayValue.length();
            final Object[] arrays = new Object[length];
            for (int i = 0; i < length; i++) {
                arrays[i] = arrayValue.get(i);
            }
            setText(uiObject.getElement(), function.getFunction().apply(arrays));
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }
}
