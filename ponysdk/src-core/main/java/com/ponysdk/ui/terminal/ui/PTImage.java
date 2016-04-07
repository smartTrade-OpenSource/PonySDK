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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.HandlerModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTImage extends PTWidget<Image> {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        final BinaryModel url = buffer.getBinaryModel();
        if (Model.IMAGE_URL.equals(url.getModel())) {
            final BinaryModel left = buffer.getBinaryModel();
            if (Model.IMAGE_LEFT.equals(left.getModel())) {
                // Model.IMAGE_TOP
                final int top = buffer.getBinaryModel().getIntValue();
                // Model.IMAGE_WIDTH
                final int width = buffer.getBinaryModel().getIntValue();
                // Model.IMAGE_HEIGHT
                final int height = buffer.getBinaryModel().getIntValue();
                this.uiObject = new Image(url.getStringValue(), left.getIntValue(), top, width, height);
            } else {
                buffer.rewind(left);
                this.uiObject = new Image(url.getStringValue());
            }
        } else {
            buffer.rewind(url);
            this.uiObject = new Image();
        }
        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIService uiService) {
        if (HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST_HANDLER.equals(handlerModel)) {
            // Model.STREAM_REQUEST_ID
            cast().setUrl(GWT.getHostPageBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&"
                    + Model.STREAM_REQUEST_ID + "=" + buffer.getBinaryModel().getIntValue());
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.IMAGE_URL.equals(binaryModel.getModel())) {
            cast().setUrl(binaryModel.getStringValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

}
