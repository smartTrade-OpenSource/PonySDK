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
package com.ponysdk.ui.server.list.paging;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PWidget;

public class MenuBarPagingView extends PHorizontalPanel implements PagingView {

    private final Map<Integer, IsPWidget> items = new HashMap<Integer, IsPWidget>();
    private IsPWidget selectedItem;
    private PMenuBar menuBar;
    private PMenuItem startMenuItem;
    private PMenuItem previousMenuItem;
    private PMenuItem nextMenuItem;
    private PMenuItem endMenuItem;

    @Override
    public void addSeparator(String text) {
        menuBar.addSeparator();
    }

    @Override
    public void addPageIndex(int pageIndex, final PCommand command) {
        final PMenuItem item = new PMenuItem(String.valueOf(pageIndex + 1));
        item.setWidth("15px");
        item.setCommand(command);
        menuBar.insertItem(item, 4 + items.values().size());
        items.put(pageIndex, item);
    }

    @Override
    public void setSelectedPage(int pageIndex) {
        final PWidget item = items.get(pageIndex).asWidget();
        item.setStyleProperty("fontWeight", "bold");
        if (selectedItem != null)
            selectedItem.asWidget().setStyleName("unselected");
        selectedItem = item;
    }

    @Override
    public void setStart(boolean enabled, PCommand command) {
        startMenuItem.setCommand(command);
        startMenuItem.setEnabled(enabled);
    }

    @Override
    public void setEnd(boolean enabled, PCommand command) {
        endMenuItem.setCommand(command);
        endMenuItem.setEnabled(enabled);
    }

    @Override
    public void setNext(boolean enabled, PCommand command) {
        nextMenuItem.setCommand(command);
        nextMenuItem.setEnabled(enabled);
    }

    @Override
    public void setPrevious(boolean enabled, PCommand command) {
        previousMenuItem.setCommand(command);
        previousMenuItem.setEnabled(enabled);
    }

    @Override
    public void clear() {
        super.clear();
        items.clear();
        initUI();
    }

    private void initUI() {
        menuBar = new PMenuBar();
        menuBar.setStyleName("pony-ActionToolbar");
        menuBar.setStyleProperty("marginRight", ".5em");

        startMenuItem = new PMenuItem("<<");
        menuBar.addItem(startMenuItem);
        menuBar.addSeparator();
        previousMenuItem = new PMenuItem("<");
        menuBar.addItem(previousMenuItem);
        menuBar.addSeparator();

        menuBar.addSeparator();
        nextMenuItem = new PMenuItem(">");
        menuBar.addItem(nextMenuItem);
        menuBar.addSeparator();
        endMenuItem = new PMenuItem(">>");
        menuBar.addItem(endMenuItem);

        add(menuBar);
    }

    @Override
    public void showPagingBar(boolean show) {
        setVisible(show);
    }
}
