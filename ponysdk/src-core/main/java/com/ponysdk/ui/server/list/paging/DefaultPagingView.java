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
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class DefaultPagingView extends PHorizontalPanel implements PagingView {

    private final Map<Integer, IsPWidget> items = new HashMap<Integer, IsPWidget>();

    private IsPWidget selectedItem;

    private PButton startButton;

    private PButton previousButton;

    private PHorizontalPanel pagesPanel;

    private PButton nextButton;

    private PButton endButton;

    public DefaultPagingView() {
        setSpacing(10);
        initUI();
    }

    private void initUI() {
        startButton = new PButton("|<");
        add(startButton);
        previousButton = new PButton("<");
        add(previousButton);
        pagesPanel = new PHorizontalPanel();
        add(pagesPanel);
        nextButton = new PButton(">");
        add(nextButton);
        endButton = new PButton(">|");
        add(endButton);
    }

    @Override
    public void addSeparator(String text) {
        add(new PLabel(text));
    }

    @Override
    public void addPageIndex(int pageIndex, final PCommand command) {
        final PButton button = new PButton(String.valueOf(pageIndex + 1));
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                command.execute();
            }
        });
        pagesPanel.add(button);
        items.put(pageIndex, button);
    }

    @Override
    public void setSelectedPage(int pageIndex) {
        final PWidget item = items.get(pageIndex).asWidget();
        item.setStyleName("selected");
        if (selectedItem != null) selectedItem.asWidget().setStyleName("unselected");
        selectedItem = item;
    }

    @Override
    public void setStart(boolean enabled, final PCommand command) {
        startButton.setEnabled(enabled);
        startButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                command.execute();
            }
        });

    }

    @Override
    public void setEnd(boolean enabled, final PCommand command) {
        endButton.setEnabled(enabled);
        endButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                command.execute();
            }
        });

    }

    @Override
    public void setNext(boolean enabled, final PCommand command) {
        nextButton.setEnabled(enabled);
        nextButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                command.execute();
            }
        });

    }

    @Override
    public void setPrevious(boolean enabled, final PCommand command) {
        previousButton.setEnabled(enabled);
        previousButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                command.execute();
            }
        });

    }

    @Override
    public void clear() {
        super.clear();
        initUI();
    }

    @Override
    public void showPagingBar(boolean show) {
        setVisible(show);
    }
}
