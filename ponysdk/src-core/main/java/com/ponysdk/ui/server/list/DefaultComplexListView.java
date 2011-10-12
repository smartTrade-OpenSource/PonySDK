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
package com.ponysdk.ui.server.list;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.addon.PFloatablePanel;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PAcceptsOneWidget;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PToolbar;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class DefaultComplexListView extends PVerticalPanel implements ComplexListView {

    private final SimpleListView simpleListView = new DefaultSimpleListView();
    private final PSimplePanel inputLayout = new PSimplePanel();
    private final PToolbar toolbarLayout = new PToolbar();
    private final PSimplePanel pagingLayout = new PSimplePanel();
    private final PFloatablePanel positionPanel = new PFloatablePanel();
    private final PSimplePanel topListLayout = new PSimplePanel();
    private final PSimplePanel bottomListLayout = new PSimplePanel();
    private final PVerticalPanel bottomListCustomInformationLayout = new PVerticalPanel();

    private final PLabel searchResultTimeLabel = new PLabel();

    public DefaultComplexListView() {
        setSizeFull();

        final PHorizontalPanel toolbarGroupPanel = new PHorizontalPanel();
        toolbarGroupPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        toolbarGroupPanel.setWidth("100%");
        toolbarGroupPanel.add(toolbarLayout.asWidget());
        toolbarGroupPanel.add(pagingLayout);
        toolbarGroupPanel.setCellHorizontalAlignment(pagingLayout.asWidget(), PHorizontalAlignment.ALIGN_RIGHT);
        topListLayout.setSizeFull();

        final PVerticalPanel headerPanel = new PVerticalPanel();
        headerPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        headerPanel.setSizeFull();
        headerPanel.setStyleProperty("paddingLeft", "1em");
        headerPanel.setStyleProperty("paddingRight", "1.3em");
        headerPanel.add(inputLayout);
        headerPanel.add(toolbarGroupPanel);
        headerPanel.add(topListLayout);
        headerPanel.setWidth("100%");

        positionPanel.setWidget(headerPanel);

        bottomListLayout.setWidget(searchResultTimeLabel);

        simpleListView.asWidget().addStyleName("pony-ComplexList");

        add(positionPanel);
        add(simpleListView.asWidget());
        add(bottomListLayout);
        setCellHorizontalAlignment(bottomListLayout, PHorizontalAlignment.ALIGN_CENTER);
        bottomListLayout.addStyleName(PonySDKTheme.COMPLEX_LIST_BOTTOM_LIST);
        add(bottomListCustomInformationLayout);
    }

    @Override
    public void addAction(String caption, PClickHandler clickHandler) {
        final PButton button = new PButton(caption);
        button.addClickHandler(clickHandler);
        toolbarLayout.add(button);
    }

    @Override
    public void clearList() {
        simpleListView.clearList();
    }

    @Override
    public void insertRow(int row) {
        simpleListView.insertRow(row);
    }

    @Override
    public void removeRow(int row) {
        simpleListView.removeRow(row);
    }

    @Override
    public void setColumns(int size) {
        simpleListView.setColumns(size);
    }

    @Override
    public void setSearchResultInformation(String text) {
        searchResultTimeLabel.setText(text);
    }

    @Override
    public PAcceptsOneWidget getBottomListLayout() {
        return bottomListLayout;
    }

    @Override
    public PAcceptsOneWidget getFormLayout() {
        return inputLayout;
    }

    @Override
    public void addWidget(IsPWidget component, int column, int row) {
        simpleListView.addWidget(component, column, row);
    }

    @Override
    public PToolbar getToolbarLayout() {
        return toolbarLayout;
    }

    @Override
    public PAcceptsOneWidget getPagingLayout() {
        return pagingLayout;
    }

    @Override
    public void selectRow(int row) {
        simpleListView.selectRow(row);
    }

    @Override
    public void unSelectRow(int row) {
        simpleListView.unSelectRow(row);
    }

    @Override
    public void addRowStyle(int row, String styleName) {
        simpleListView.addRowStyle(row, styleName);
    }

    @Override
    public void setFloatableToolBar(PScrollPanel ancestorScrollPanel) {
        positionPanel.setLinkedScrollPanel(ancestorScrollPanel);
    }

    @Override
    public void updateView() {
        positionPanel.correct();
    }

    @Override
    public void addHeaderStyle(String styleName) {
        simpleListView.addHeaderStyle(styleName);

    }

    @Override
    public PAcceptsOneWidget getTopListLayout() {
        return topListLayout;
    }

    @Override
    public void addCustomInformation(String text) {
        bottomListCustomInformationLayout.add(new PLabel(text));
    }

    @Override
    public void addCellStyle(int row, int col, String styleName) {
        simpleListView.addCellStyle(row, col, styleName);
    }

    @Override
    public void removeCellStyle(int row, int column, String styleName) {
        simpleListView.removeCellStyle(row, column, styleName);

    }

}
