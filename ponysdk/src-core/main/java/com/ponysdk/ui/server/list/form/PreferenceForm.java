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

package com.ponysdk.ui.server.list.form;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ponysdk.core.event.EventBus;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PMouseOutEvent;
import com.ponysdk.ui.server.basic.event.PMouseOutHandler;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.list.ListColumnDescriptor;
import com.ponysdk.ui.server.list.event.MoveColumnDescriptorEvent;
import com.ponysdk.ui.server.list.event.RemoveColumnDescriptorEvent;
import com.ponysdk.ui.server.list.event.ShowColumnDescriptorEvent;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class PreferenceForm extends PScrollPanel {

    List<SelectableLabel> labels = new ArrayList<SelectableLabel>();

    private final EventBus eventBus;

    private final PFlexTable labelPanel = new PFlexTable();

    private final String tableName;

    private static final String ARROW_DOWN_IMAGE_URL = "images/arrow-down.png";

    private static final String ARROW_UP_IMAGE_URL = "images/arrow-up.png";

    private class SelectableLabel extends PLabel {

        PCheckBox checkBox;

        boolean viewable = true;

        boolean custom = false;

        public SelectableLabel(final String caption) {
            super(caption);
        }

    }

    public PreferenceForm(final Collection<ListColumnDescriptor<?, ?>> captions, final EventBus eventBus, final String tableName) {
        this.eventBus = eventBus;
        this.tableName = tableName;
        init(captions);
        buildUI();
        setWidth("500px");
        setHeight("500px");
    }

    private void init(final Collection<ListColumnDescriptor<?, ?>> captions) {
        labelPanel.getRowFormatter().addStyleName(0, "pony-ComplexList-ColumnHeader");
        for (final ListColumnDescriptor<?, ?> caption : captions) {
            if (caption == null) continue;
            final SelectableLabel label = new SelectableLabel(caption.getCaption());
            final PCheckBox checkBox = new PCheckBox();
            label.checkBox = checkBox;
            label.viewable = caption.isViewable();
            label.custom = caption.isCustom();
            checkBox.addValueChangeHandler(new PValueChangeHandler<Boolean>() {

                @Override
                public void onValueChange(final PValueChangeEvent<Boolean> event) {
                    final ShowColumnDescriptorEvent refreshListEvent = new ShowColumnDescriptorEvent(PreferenceForm.this, caption.getCaption(), event.getValue(), tableName);
                    eventBus.fireEvent(refreshListEvent);
                    label.viewable = event.getValue();
                    refreshLabels();
                }
            });
            checkBox.setValue(caption.isViewable());
            labels.add(label);
        }
        refreshLabels();
    }

    private void refreshLabels() {
        labelPanel.clear();
        labelPanel.setWidth("100%");
        labelPanel.setWidget(0, 0, new PLabel("Index"));
        labelPanel.setWidget(0, 1, new PLabel("Header"));
        labelPanel.setWidget(0, 2, new PLabel("Visible"));
        labelPanel.getRowFormatter().addStyleName(0, "pony-ComplexList-ColumnHeader");
        int i = 1;
        int nextViewableIndex = 1;
        for (final SelectableLabel label : labels) {
            // add index if and only if label is viewable
            if (label.viewable) {
                labelPanel.setWidget(i, 0, new PLabel(nextViewableIndex + ""));
                nextViewableIndex++;
            }

            final PHorizontalPanel panel = new PHorizontalPanel();
            panel.setSizeFull();
            panel.add(label);
            final PHorizontalPanel simplePanel = new PHorizontalPanel();
            final PHorizontalPanel buttonPanel = new PHorizontalPanel();
            panel.add(simplePanel);
            panel.setCellHorizontalAlignment(simplePanel, PHorizontalAlignment.ALIGN_RIGHT);
            labelPanel.setWidget(i, 1, panel);

            // hide button panel : shoul only be visible when mouse hover panel
            buttonPanel.setSizeFull();
            buttonPanel.setVisible(false);
            panel.addDomHandler(new PMouseOverHandler() {

                @Override
                public void onMouseOver(final PMouseOverEvent mouseOverEvent) {
                    buttonPanel.setVisible(true);
                }
            }, PMouseOverEvent.TYPE);
            panel.addDomHandler(new PMouseOutHandler() {

                @Override
                public void onMouseOut(final PMouseOutEvent event) {
                    buttonPanel.setVisible(false);
                }
            }, PMouseOutEvent.TYPE);

            simplePanel.setWidth("30px");
            simplePanel.add(buttonPanel);
            if (i != 1) {

                // up image
                final PImage upImage = new PImage(ARROW_UP_IMAGE_URL);
                upImage.setTitle("Move column up");
                buttonPanel.add(upImage);
                buttonPanel.setCellHorizontalAlignment(upImage, PHorizontalAlignment.ALIGN_LEFT);
                upImage.addClickHandler(new PClickHandler() {

                    @Override
                    public void onClick(final PClickEvent event) {
                        final int index = labels.indexOf(label) - 1;
                        labels.remove(label);
                        labels.add(index, label);
                        fireColumnMoved();
                        for (int row = 1; row <= labels.size(); row++) {
                            labelPanel.getRowFormatter().removeStyleName(row, PonySDKTheme.SIMPLELIST_SELECTEDROW);
                        }
                        labelPanel.getRowFormatter().addStyleName(index + 1, PonySDKTheme.SIMPLELIST_SELECTEDROW);
                    }
                });
            }
            if (i != labels.size()) {
                // down image
                final PImage downImage = new PImage(ARROW_DOWN_IMAGE_URL);
                downImage.setTitle("Move column down");
                buttonPanel.add(downImage);
                buttonPanel.setCellHorizontalAlignment(downImage, PHorizontalAlignment.ALIGN_RIGHT);
                downImage.addClickHandler(new PClickHandler() {

                    @Override
                    public void onClick(final PClickEvent event) {
                        final int index = labels.indexOf(label) + 1;
                        labels.remove(label);
                        labels.add(index, label);
                        fireColumnMoved();
                        for (int row = 1; row <= labels.size(); row++) {
                            labelPanel.getRowFormatter().removeStyleName(row, PonySDKTheme.SIMPLELIST_SELECTEDROW);
                        }
                        labelPanel.getRowFormatter().addStyleName(index + 1, PonySDKTheme.SIMPLELIST_SELECTEDROW);
                    }
                });
            }
            // add erase button for custom column
            if (label.custom) {
                final PButton eraseButton = new PButton("Erase");
                eraseButton.addClickHandler(new PClickHandler() {

                    @Override
                    public void onClick(final PClickEvent event) {
                        eventBus.fireEvent(new RemoveColumnDescriptorEvent(PreferenceForm.this, label.getText(), tableName));
                        labels.remove(label);
                        refreshLabels();
                    }
                });
                buttonPanel.add(eraseButton);
            }

            labelPanel.setWidget(i, 2, label.checkBox);
            labelPanel.getRowFormatter().addStyleName(i, PonySDKTheme.SIMPLELIST_ROW);
            i++;
        }
    }

    private void buildUI() {
        // final PHorizontalPanel mainPanel = new PHorizontalPanel();
        // mainPanel.add();
        setWidget(labelPanel);
    }

    void fireColumnMoved() {
        final List<String> order = new ArrayList<String>();
        for (final PLabel label : labels) {
            order.add(label.getText());
        }
        eventBus.fireEvent(new MoveColumnDescriptorEvent(PreferenceForm.this, order, tableName));
        refreshLabels();
    }

}
