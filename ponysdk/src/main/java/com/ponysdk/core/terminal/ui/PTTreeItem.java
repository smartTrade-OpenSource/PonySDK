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

    private Tree tree;

    private String text;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.TEXT.equals(binaryModel.getModel())) {
            this.text = binaryModel.getStringValue();
        } else {
            buffer.rewind(binaryModel);
        }

        super.create(buffer, objectId, uiBuilder);

        binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.TREE_ROOT.equals(binaryModel.getModel())) {
            this.tree = (Tree) asWidget(binaryModel.getIntValue(), uiBuilder);
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

        final BinaryModel binaryModel = buffer.readBinaryModel();
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.WIDGET.equals(model)) {
            uiObject.setWidget((Widget) widget);
        } else if (ServerToClientModel.INDEX.equals(model)) {
            final int index = binaryModel.getIntValue();
            final TreeItem w = (TreeItem) widget;
            if (tree != null) tree.insertItem(index, w);
            else uiObject.insertItem(index, w);
        } else {
            buffer.rewind(binaryModel);
            final TreeItem w = (TreeItem) widget;
            if (tree != null) tree.addItem(w);
            else uiObject.addItem(w);
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.OPEN.ordinal() == modelOrdinal) {
            uiObject.setState(true, false);
            return true;
        } else if (ServerToClientModel.CLOSE.ordinal() == modelOrdinal) {
            uiObject.setState(false, false);
            return true;
        } else if (ServerToClientModel.TEXT.ordinal() == modelOrdinal) {
            uiObject.setText(binaryModel.getStringValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void remove(final ReaderBuffer buffer, final PTObject ptObject) {
        if (tree != null) tree.removeItem(asWidget(ptObject));
        else uiObject.removeItem(asWidget(ptObject));
    }

}
