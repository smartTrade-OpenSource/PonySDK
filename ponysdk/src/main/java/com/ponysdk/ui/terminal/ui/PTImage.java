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
import com.ponysdk.ui.model.ClientToServerModel;
import com.ponysdk.ui.model.HandlerModel;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTImage extends PTWidget<Image> {

    private String url;
    private int left = -1;
    private int top = -1;
    private int width = -1;
    private int height = -1;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        final BinaryModel urlModel = buffer.getBinaryModel();
        if (ServerToClientModel.IMAGE_URL.equals(urlModel.getModel())) {
            final BinaryModel leftModel = buffer.getBinaryModel();
            url = urlModel.getStringValue();
            if (ServerToClientModel.IMAGE_LEFT.equals(leftModel.getModel())) {
                left = leftModel.getIntValue();
                top = buffer.getBinaryModel().getIntValue();
                width = buffer.getBinaryModel().getIntValue();
                height = buffer.getBinaryModel().getIntValue();
            } else {
                buffer.rewind(leftModel);
            }
        } else {
            buffer.rewind(urlModel);
        }

        super.create(buffer, objectId, uiService);
    }

    @Override
    protected Image createUIObject() {
        if (url != null) {
            return left != -1 ? new Image(url, left, top, width, height) : new Image(url);
        } else {
            return new Image();
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel, final UIBuilder uiService) {
        if (HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST.equals(handlerModel)) {
            // ServerToClientModel.STREAM_REQUEST_ID
            cast().setUrl(GWT.getHostPageBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&" + ClientToServerModel.STREAM_REQUEST_ID.toStringValue() + "="
                    + buffer.getBinaryModel().getIntValue());
        } else {
            super.addHandler(buffer, handlerModel, uiService);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.IMAGE_URL.equals(binaryModel.getModel())) {
            cast().setUrl(binaryModel.getStringValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

}
