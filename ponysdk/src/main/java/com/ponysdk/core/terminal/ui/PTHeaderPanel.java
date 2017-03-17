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

import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTHeaderPanel extends PTPanel<HeaderPanel> {

    @Override
    protected HeaderPanel createUIObject() {
        return new HeaderPanel();
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.INDEX.equals(binaryModel.getModel())) {
            final Widget w = asWidget(ptObject);
            final int index = binaryModel.getIntValue();
            if (index == 0) {
                cast().setHeaderWidget(w);
                // Wait GWT fix :
                // https://groups.google.com/forum/#!msg/google-web-toolkit/8odDZdlhDVo/hw852twqQAUJ
                w.getElement().getParentElement().getStyle().clearProperty("minWidth");
                w.getElement().getParentElement().getStyle().clearProperty("minHeight");
            } else if (index == 1) {
                cast().setContentWidget(w);
            } else if (index == 2) {
                cast().setFooterWidget(w);
                // Wait GWT fix :
                // https://groups.google.com/forum/#!msg/google-web-toolkit/8odDZdlhDVo/hw852twqQAUJ
                w.getElement().getParentElement().getStyle().clearProperty("minWidth");
                w.getElement().getParentElement().getStyle().clearProperty("minHeight");
            }
        } else {
            buffer.rewind(binaryModel);
            super.add(buffer, ptObject);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.RESIZE.ordinal() == modelOrdinal) {
            uiObject.onResize();
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void remove(final ReaderBuffer buffer, final PTObject ptObject) {
        uiObject.remove(asWidget(ptObject));
    }

}
