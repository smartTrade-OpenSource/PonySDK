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
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTImage extends PTWidget<Image> {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        if (create.containsKey(Model.IMAGE_URL)) {
            final String url = create.getString(Model.IMAGE_URL);
            if (create.containsKey(Model.IMAGE_LEFT)) {
                final int left = create.getInt(Model.IMAGE_LEFT);
                final int top = create.getInt(Model.IMAGE_TOP);
                final int width = create.getInt(Model.IMAGE_WIDTH);
                final int height = create.getInt(Model.IMAGE_HEIGHT);

                init(create, uiService, new Image(url, left, top, width, height));
            } else {
                init(create, uiService, new Image(url));
            }
        } else {
            init(create, uiService, new Image());
        }
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        if (addHandler.containsKey(Model.HANDLER_EMBEDED_STREAM_REQUEST_HANDLER)) {
            cast().setUrl(GWT.getHostPageBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&" + Model.STREAM_REQUEST_ID
                    + "=" + addHandler.getString(Model.STREAM_REQUEST_ID));
        } else {
            super.addHandler(addHandler, uiService);
        }
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.IMAGE_URL)) {
            cast().setUrl(update.getString(Model.IMAGE_URL));
        } else {
            super.update(update, uiService);
        }
    }

}
