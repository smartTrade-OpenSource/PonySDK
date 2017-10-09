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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.PTElement.MyHTMLPanel;

public class PTElement extends PTComplexPanel<MyHTMLPanel> {

    private static final String EMPTY = "";
    private String tag;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        // ServerToClientModel.TAG
        tag = buffer.readBinaryModel().getStringValue();
        super.create(buffer, objectId, uiService);
    }

    @Override
    protected MyHTMLPanel createUIObject() {
        return new MyHTMLPanel(tag, EMPTY);
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.INDEX == binaryModel.getModel()) {
            uiObject.insert(asWidget(ptObject), uiObject.getElement(), binaryModel.getIntValue(), true);
        } else {
            buffer.rewind(binaryModel);
            super.add(buffer, ptObject);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.INNER_HTML == model) {
            uiObject.getElement().setInnerHTML(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.INNER_TEXT == model) {
            uiObject.getElement().setInnerText(binaryModel.getStringValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    static final class MyHTMLPanel extends HTMLPanel {

        private MyHTMLPanel(final String tag, final String html) {
            super(tag, html);
        }

        @Override
        protected void insert(final Widget child, final Element container, final int beforeIndex, final boolean domInsert) {
            super.insert(child, container, beforeIndex, domInsert);
        }

    }

}
