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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTTreeItem extends PTUIObject<TreeItem> {

    private boolean isRoot = false;

    private Tree tree;

    private String text;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        // ServerToClientModel.TEXT
        this.text = buffer.readBinaryModel().getStringValue();

        super.create(buffer, objectId, uiService);

        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.ROOT.equals(binaryModel.getModel())) {
            this.isRoot = binaryModel.getBooleanValue();
        } else {
            buffer.rewind(binaryModel);
        }
    }

    @Override
    protected TreeItem createUIObject() {
        return text != null ? new TreeItem(SafeHtmlUtils.fromString(text)) : new TreeItem();
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final UIObject widget = asWidget(ptObject);

        if (widget instanceof Tree) {
            this.tree = (Tree) widget;
        } else {
            final BinaryModel binaryModel = buffer.readBinaryModel();
            ServerToClientModel model = binaryModel.getModel();
            if (ServerToClientModel.WIDGET.equals(model)) {
                uiObject.setWidget((Widget) widget);
            } else if (ServerToClientModel.INDEX.equals(model)) {
                final int index = binaryModel.getIntValue();
                final TreeItem w = (TreeItem) widget;
                if (isRoot) tree.insertItem(index, w);
                else uiObject.insertItem(index, w);
            } else {
                buffer.rewind(binaryModel);
                final TreeItem w = (TreeItem) widget;
                if (isRoot) tree.addItem(w);
                else uiObject.addItem(w);
            }
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.SELECTED.ordinal() == modelOrdinal) {
            uiObject.setSelected(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.STATE.ordinal() == modelOrdinal) {
            uiObject.setState(binaryModel.getBooleanValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
