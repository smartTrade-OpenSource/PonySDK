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
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTImage extends PTWidget {

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        if (create.containsKey(PROPERTY.IMAGE_URL)) {
            final String url = create.getString(PROPERTY.IMAGE_URL);
            final int left = create.getInt(PROPERTY.IMAGE_LEFT);
            final int top = create.getInt(PROPERTY.IMAGE_TOP);
            final int width = create.getInt(PROPERTY.WIDGET_WIDTH);
            final int height = create.getInt(PROPERTY.WIDGET_HEIGHT);

            init(create, uiService, new Image(url, left, top, width, height));
        } else {
            init(create, uiService, new Image());
        }
    }

    @Override
    public void addHandler(final PTInstruction addHandler, final UIService uiService) {
        if (addHandler.containsKey(HANDLER.EMBEDED_STREAM_REQUEST_HANDLER)) {
            cast().setUrl(GWT.getModuleBaseURL() + "stream?" + "ponySessionID=" + UIBuilder.sessionID + "&" + PROPERTY.STREAM_REQUEST_ID + "=" + addHandler.getString(PROPERTY.STREAM_REQUEST_ID));
        } else {
            super.addHandler(addHandler, uiService);
        }

    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.IMAGE_URL)) {
            cast().setUrl(update.getString(PROPERTY.IMAGE_URL));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public Image cast() {
        return (Image) uiObject;
    }
}
