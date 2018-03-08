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

package com.ponysdk.core.ui.list;

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAcceptsOneWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.rich.PToolbar;

public class DefaultComplexListView extends PVerticalPanel implements ComplexListView {

    private final SimpleListView simpleListView = new DefaultSimpleListView();

    private final PSimplePanel inputLayout = Element.newPSimplePanel();

    private final PToolbar toolbarLayout = new PToolbar();

    private final PSimplePanel pagingLayout = Element.newPSimplePanel();

    private final PSimplePanel topListLayout = Element.newPSimplePanel();

    private final PSimplePanel bottomListLayout = Element.newPSimplePanel();

    private final PVerticalPanel bottomListCustomInformationLayout = Element.newPVerticalPanel();

    private final PSimplePanel preferencesLayout = Element.newPSimplePanel();

    private final PLabel searchResultTimeLabel = Element.newPLabel();

    public DefaultComplexListView() {
        setSizeFull();

        final PHorizontalPanel toolbarGroupPanel = Element.newPHorizontalPanel();
        toolbarGroupPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        toolbarGroupPanel.setWidth("100%");
        toolbarGroupPanel.add(toolbarLayout.asWidget());
        toolbarGroupPanel.add(pagingLayout);
        toolbarGroupPanel.add(preferencesLayout);
        toolbarGroupPanel.setCellHorizontalAlignment(pagingLayout.asWidget(), PHorizontalAlignment.ALIGN_RIGHT);
        topListLayout.setSizeFull();

        final PVerticalPanel headerPanel = Element.newPVerticalPanel();
        headerPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        headerPanel.setSizeFull();
        headerPanel.setStyleProperty("paddingLeft", "1em");
        headerPanel.setStyleProperty("paddingRight", "1.3em");
        headerPanel.add(inputLayout);
        headerPanel.add(toolbarGroupPanel);
        headerPanel.add(topListLayout);
        headerPanel.setWidth("100%");

        final PSimplePanel positionPanel = Element.newPSimplePanel();
        positionPanel.setWidget(headerPanel);

        bottomListLayout.setWidget(searchResultTimeLabel);

        add(positionPanel);
        add(simpleListView.asWidget());
        add(bottomListLayout);
        setCellHorizontalAlignment(bottomListLayout, PHorizontalAlignment.ALIGN_CENTER);
        // bottomListLayout.addStyleName(PonySDKTheme.COMPLEXLIST_BOTTOM_LIST);
        add(bottomListCustomInformationLayout);
    }

    @Override
    public void addAction(final String caption, final PClickHandler clickHandler) {
        final PButton button = Element.newPButton(caption);
        button.addClickHandler(clickHandler);
        toolbarLayout.add(button);
    }

    @Override
    public void clearList() {
        simpleListView.clear(1);
    }

    @Override
    public void clear(final int i) {
        simpleListView.clear(i);
    }

    @Override
    public void insertRow(final int row) {
        simpleListView.insertRow(row);
    }

    @Override
    public void removeRow(final int row) {
        simpleListView.removeRow(row);
    }

    @Override
    public void setColumns(final int size) {
        simpleListView.setColumns(size);
    }

    @Override
    public void setSearchResultInformation(final String text) {
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
    public void addWidget(final IsPWidget component, final int column, final int row, final int colspan) {
        simpleListView.addWidget(component, column, row, colspan);
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
    public void selectRow(final int row) {
        simpleListView.selectRow(row);
    }

    @Override
    public void unSelectRow(final int row) {
        simpleListView.unSelectRow(row);
    }

    @Override
    public void addRowStyle(final int row, final String styleName) {
        simpleListView.addRowStyle(row, styleName);
    }

    @Override
    public void removeRowStyle(final int row, final String styleName) {
        simpleListView.removeRowStyle(row, styleName);
    }

    @Override
    public void setFloatableToolBar(final PScrollPanel ancestorScrollPanel) {
        // positionPanel.setLinkedScrollPanel(ancestorScrollPanel);
    }

    @Override
    public void updateView() {
        // positionPanel.correct();
    }

    @Override
    public void addHeaderStyle(final String styleName) {
        simpleListView.addHeaderStyle(styleName);

    }

    @Override
    public PAcceptsOneWidget getTopListLayout() {
        return topListLayout;
    }

    @Override
    public void addCustomInformation(final String text) {
        bottomListCustomInformationLayout.add(Element.newPLabel(text));
    }

    @Override
    public void addCellStyle(final int row, final int col, final String styleName) {
        simpleListView.addCellStyle(row, col, styleName);
    }

    @Override
    public void removeCellStyle(final int row, final int column, final String styleName) {
        simpleListView.removeCellStyle(row, column, styleName);
    }

    @Override
    public void addColumnStyle(final int column, final String styleName) {
        simpleListView.addColumnStyle(column, styleName);
    }

    @Override
    public void removeColumnStyle(final int column, final String styleName) {
        simpleListView.removeColumnStyle(column, styleName);
    }

    @Override
    public void setColumnWidth(final int column, final String width) {
        simpleListView.setColumnWidth(column, width);
    }

    @Override
    public PSimplePanel getPreferencesLayout() {
        return preferencesLayout;
    }

    @Override
    public void moveRow(final int index, final int beforeIndex) {
        simpleListView.moveRow(index, beforeIndex);
    }

    @Override
    public void moveColumn(final int index, final int beforeIndex) {
        simpleListView.moveColumn(index, beforeIndex);
    }

}
