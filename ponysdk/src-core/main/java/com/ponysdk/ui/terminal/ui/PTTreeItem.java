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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTTreeItem extends PTUIObject<TreeItem> {

    private boolean isRoot = false;

    private Tree tree;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        final String text = buffer.getBinaryModel().getStringValue();
        if (text != null) {
            this.uiObject = new TreeItem(SafeHtmlUtils.fromString(text));
        } else {
            this.uiObject = new TreeItem();
        }

        this.objectID = objectId;
        uiService.registerUIObject(this.objectID, uiObject);

        final BinaryModel binaryModel = buffer.getBinaryModel();
        if (Model.ROOT.equals(binaryModel.getModel())) {
            this.isRoot = binaryModel.getBooleanValue();
        } else {
            buffer.rewind(binaryModel);
        }
    }

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final UIObject widget = asWidget(ptObject);

        if (widget instanceof Tree) {
            this.tree = (Tree) widget;
        } else {
            final BinaryModel binaryModel = buffer.getBinaryModel();
            if (Model.WIDGET.equals(binaryModel.getModel())) {
                uiObject.setWidget((Widget) widget);
            } else {
                final TreeItem w = (TreeItem) widget;
                final int index = buffer.getInt(Model.INDEX);
                if (isRoot) {
                    tree.insertItem(index, w);
                } else {
                    uiObject.insertItem(index, w);
                }
            }
        }
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.SELECTED.equals(binaryModel.getModel())) {
            uiObject.setSelected(binaryModel.getBooleanValue());
            return true;
        }
        if (Model.STATE.equals(binaryModel.getModel())) {
            uiObject.setState(binaryModel.getBooleanValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

}
