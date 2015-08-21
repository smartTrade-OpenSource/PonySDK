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
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTTreeItem extends PTUIObject<TreeItem> {

    private boolean isRoot = false;

    private Tree tree;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        this.isRoot = create.getBoolean(Model.ROOT);

        if (create.containsKey(Model.TEXT)) {
            init(create, uiService, new TreeItem(SafeHtmlUtils.fromString(create.getString(Model.TEXT))));
        } else {
            init(create, uiService, new TreeItem());
        }
    }

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        final UIObject widget = asWidget(add.getObjectID(), uiService);

        if (widget instanceof Tree) {
            this.tree = (Tree) widget;
        } else {
            if (add.containsKey(Model.WIDGET)) {
                uiObject.setWidget((Widget) widget);
            } else {
                final TreeItem w = (TreeItem) widget;
                final int index = add.getInt(Model.INDEX);
                if (isRoot) {
                    tree.insertItem(index, w);
                } else {
                    uiObject.insertItem(index, w);
                }
            }
        }
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.SELECTED)) {
            uiObject.setSelected(update.getBoolean(Model.SELECTED));
        } else if (update.containsKey(Model.STATE)) {
            uiObject.setState(update.getBoolean(Model.STATE));
        } else {
            super.update(update, uiService);
        }
    }

}
