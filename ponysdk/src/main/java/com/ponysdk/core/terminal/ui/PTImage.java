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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTImage extends PTWidget<Image> {

    private String url;
    private int left = -1;
    private int top = -1;
    private int width = -1;
    private int height = -1;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        final BinaryModel urlModel = buffer.readBinaryModel();
        if (ServerToClientModel.IMAGE_URL.equals(urlModel.getModel())) {
            url = urlModel.getStringValue();
            final BinaryModel leftModel = buffer.readBinaryModel();
            if (ServerToClientModel.IMAGE_LEFT.equals(leftModel.getModel())) {
                left = leftModel.getIntValue();
                top = buffer.readBinaryModel().getIntValue();
                width = buffer.readBinaryModel().getIntValue();
                height = buffer.readBinaryModel().getIntValue();
            } else {
                buffer.rewind(leftModel);
            }
        } else {
            buffer.rewind(urlModel);
        }

        super.create(buffer, objectId, uiBuilder);
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
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.IMAGE_URL.ordinal() == modelOrdinal) {
            cast().setUrl(binaryModel.getStringValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void addHandler(final ReaderBuffer buffer, final HandlerModel handlerModel) {
        if (HandlerModel.HANDLER_EMBEDED_STREAM_REQUEST.equals(handlerModel)) {
            // ServerToClientModel.STREAM_REQUEST_ID
            final int streamRequestId = buffer.readBinaryModel().getIntValue();

            cast().setUrl(GWT.getHostPageBaseURL() + "stream?" + ClientToServerModel.UI_CONTEXT_ID.toStringValue() + "="
                    + PonySDK.uiContextId + "&" + ClientToServerModel.STREAM_REQUEST_ID.toStringValue() + "=" + streamRequestId);
        } else {
            super.addHandler(buffer, handlerModel);
        }
    }

}
