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

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTTreeItem extends PTUIObject {

    private boolean isRoot = false;

    private Tree tree;

    @Override
    public void create(final Create create, final UIService uiService) {
        this.isRoot = create.getMainProperty().getChildProperty(PropertyKey.ROOT).getBooleanValue();
        Property textProperty = create.getMainProperty().getChildProperty(PropertyKey.TEXT);
        TreeItem treeItem;
        if (textProperty == null) {
            treeItem = new TreeItem();
        } else {
            treeItem = new TreeItem(textProperty.getValue());
        }
        init(create, uiService, treeItem);
    }

    @Override
    public void add(final Add add, final UIService uiService) {
        UIObject widget = asWidget(add.getObjectID(), uiService);

        if (widget instanceof Tree) {
            this.tree = (Tree) widget;
        } else {
            if (add.getMainProperty().containsChildProperty(PropertyKey.WIDGET)) {
                cast().setWidget((Widget) widget);
            } else {
                final TreeItem w = (TreeItem) widget;
                int index = add.getMainProperty().getIntValue();
                if (isRoot) {
                    tree.insertItem(index, w);
                } else {
                    cast().insertItem(index, w);
                }
            }
        }
    }

    @Override
    public void update(final Update update, final UIService uiService) {
        Property property = update.getMainProperty();

        switch (property.getKey()) {
            case SELECTED:
                cast().setSelected(property.getBooleanValue());
                break;
            case STATE:
                cast().setState(property.getBooleanValue());
                break;

            default:
                super.update(update, uiService);
                break;
        }
    }

    @Override
    public TreeItem cast() {
        return (TreeItem) uiObject;
    }

}
