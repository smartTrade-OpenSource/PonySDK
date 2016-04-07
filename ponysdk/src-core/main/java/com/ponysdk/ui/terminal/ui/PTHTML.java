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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.user.client.ui.HTML;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTHTML extends PTLabel {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        this.uiObject = new HTML();
        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);

        BinaryModel binaryModel = buffer.getBinaryModel();
        if (Model.HTML.equals(binaryModel.getModel())) {
            cast().setHTML(binaryModel.getStringValue());
        } else {
            buffer.rewind(binaryModel);
        }

        binaryModel = buffer.getBinaryModel();
        if (Model.WORD_WRAP.equals(binaryModel.getModel())) {
            cast().setWordWrap(binaryModel.getBooleanValue());
        } else {
            buffer.rewind(binaryModel);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.HTML.equals(binaryModel.getModel())) {
            cast().setHTML(binaryModel.getStringValue());
            return true;
        }
        if (Model.WORD_WRAP.equals(binaryModel.getModel())) {
            cast().setWordWrap(binaryModel.getBooleanValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public HTML cast() {
        return (HTML) super.cast();
    }

}
