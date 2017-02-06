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

package com.ponysdk.sample.client.page;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.server.service.query.Query;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PConfirmDialogHandler;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.form.Form;
import com.ponysdk.core.ui.form.FormFieldComponent;
import com.ponysdk.core.ui.form.event.SubmitFormEvent;
import com.ponysdk.core.ui.form.event.SubmitFormHandler;
import com.ponysdk.core.ui.form.formfield.IntegerTextBoxFormField;
import com.ponysdk.core.ui.form.formfield.ListBoxFormField;
import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.form.formfield.TextBoxFormField;
import com.ponysdk.core.ui.form.validator.IntegerFieldValidator;
import com.ponysdk.core.ui.form.validator.NotEmptyFieldValidator;
import com.ponysdk.core.ui.list.DataGridActivity;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.DefaultSimpleListView;
import com.ponysdk.core.ui.list.IdentityDataGridColumnDescriptor;
import com.ponysdk.core.ui.list.dataprovider.RemoteDataProvider;
import com.ponysdk.core.ui.list.paging.DefaultPagerView;
import com.ponysdk.core.ui.list.paging.Pager;
import com.ponysdk.core.ui.list.refreshable.Cell;
import com.ponysdk.core.ui.list.renderer.cell.CellRenderer;
import com.ponysdk.core.ui.list.renderer.cell.LabelCellRenderer;
import com.ponysdk.core.ui.list.renderer.header.ComplexHeaderCellRenderer;
import com.ponysdk.core.ui.list.renderer.header.FilterableHeaderCellRenderer;
import com.ponysdk.core.ui.list.renderer.header.HeaderCellRenderer;
import com.ponysdk.core.ui.list.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.core.ui.list.selector.CompositeSelectorView;
import com.ponysdk.core.ui.list.selector.DefaultActionSelectorView;
import com.ponysdk.core.ui.list.selector.DefaultInfoSelectorView;
import com.ponysdk.core.ui.list.selector.Selector;
import com.ponysdk.core.ui.list.selector.SelectorCheckBox;
import com.ponysdk.core.ui.list.valueprovider.BeanValueProvider;
import com.ponysdk.core.ui.rich.PConfirmDialog;
import com.ponysdk.sample.client.datamodel.Pony;

public class DataGridPageActivity extends SamplePageActivity implements SubmitFormHandler {

    private DataGridActivity<Pony> dataGrid;

    private PFlexTable createPonyActivityPanel;
    private Form createPony;
    private TextBoxFormField<String> raceFormField;
    private TextBoxFormField<String> nameFormField;
    private TextBoxFormField<Integer> ageFormField;

    final PTextBox line = Element.newPTextBox();

    public DataGridPageActivity() {
        super("Data Grid", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        final PScrollPanel scroll = Element.newPScrollPanel();
        final PFlowPanel layout = Element.newPFlowPanel();
        final PFlexTable formContainer = Element.newPFlexTable();
        final PSimplePanel listContainer = Element.newPSimplePanel();
        layout.add(formContainer);

        final PButton add = Element.newPButton("Add");
        add.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Integer i = Integer.parseInt(line.getText());
                final Pony father = dataGrid.getRow(i);
                final List<Pony> copy = new ArrayList<>();
                for (int j = 0; j < 3; j++) {
                    final Pony p = new Pony(father.getId(), "Copy-" + father.getName(), father.getAge(), father.getRace());
                    copy.add(p);
                }
                dataGrid.insertSubList(father, copy);
            }
        });

        final PButton remove = Element.newPButton("Remove");
        remove.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Integer i = Integer.parseInt(line.getText());
                final Pony father = dataGrid.getRow(i);
                dataGrid.removeSubList(father);
            }
        });

        layout.add(line);
        layout.add(add);
        layout.add(remove);
        layout.add(listContainer);
        scroll.setWidget(layout);
        examplePanel.setWidget(scroll);

        // Register handler
        addHandler(SubmitFormEvent.TYPE, this);

        final Pager<Pony> pager = new Pager<>(new DefaultPagerView());
        dataGrid = new DataGridActivity<>(new DefaultSimpleListView());

        final DefaultActionSelectorView actionSelectorView = new DefaultActionSelectorView();
        final DefaultInfoSelectorView infoSelectorView = new DefaultInfoSelectorView();
        final CompositeSelectorView selectorView = new CompositeSelectorView(actionSelectorView, infoSelectorView);
        final Selector<Pony> selector = new Selector<>(selectorView);

        final RemoteDataProvider<Pony> dataProvider = new RemoteDataProvider<Pony>(pager, dataGrid) {

            @Override
            protected List<Pony> getData(final Query query) {
                // final Result<List<Pony>> result = new
                // FindPonysCommand(query).execute();
                // final List<Pony> data = result.getData();
                // final int fullSize = result.getFullSize();
                // pager.process(fullSize);
                // selector.reset();
                // selector.setPageSize(data.size());
                // selector.setFullSize(fullSize);
                // return data;
                return null;
            }

            @Override
            protected List<Pony> getFullData(final Query query) {
                return null;
                // return new FindPonysCommand(query).execute().getData();
            }

        };

        final IdentityDataGridColumnDescriptor<Pony> selectColumnDescriptor = new IdentityDataGridColumnDescriptor<>();
        selectColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Select"));
        final CellRenderer<Pony, SelectorCheckBox<Pony>> selectCellRenderer = new CellRenderer<Pony, SelectorCheckBox<Pony>>() {

            @Override
            public SelectorCheckBox<Pony> render(final int row, final Pony value) {
                final SelectorCheckBox<Pony> selectorCheckBox = new SelectorCheckBox<>();
                selectorCheckBox.setData(value);
                selectorCheckBox.addSelectableListener(selector);
                selector.registerSelectable(selectorCheckBox);

                selectorCheckBox.addValueChangeHandler(new PValueChangeHandler<Boolean>() {

                    @Override
                    public void onValueChange(final PValueChangeEvent<Boolean> event) {
                        if (event.getValue()) {
                            selectorCheckBox.onCheck();
                            dataGrid.selectRow(value);

                        } else {
                            selectorCheckBox.onUncheck();
                            dataGrid.unSelectRow(value);

                        }
                    }
                });

                return selectorCheckBox;
            }

            @Override
            public void update(final Pony value, final Cell<Pony, SelectorCheckBox<Pony>> previous) {
            }
        };
        selectColumnDescriptor.setCellRenderer(selectCellRenderer);
        selectColumnDescriptor.setSubCellRenderer(selectCellRenderer);

        final IdentityDataGridColumnDescriptor<Pony> descriptor = new IdentityDataGridColumnDescriptor<>();
        descriptor.setHeaderCellRenderer(new HeaderCellRenderer() {

            @Override
            public PLabel render() {
                return Element.newPLabel();
            }
        });

        final DataGridColumnDescriptor<Pony, String> nameColumnDescriptor = new DataGridColumnDescriptor<>();
        final ComplexHeaderCellRenderer nameHeaderCellRender = new FilterableHeaderCellRenderer("Name", new StringTextBoxFormField(),
            "name");
        nameHeaderCellRender.addFilterListener(dataProvider);
        nameColumnDescriptor.setHeaderCellRenderer(nameHeaderCellRender);
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setCellRenderer(new LabelCellRenderer<String>());
        nameColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        nameColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        final PListBox ageListBox = Element.newPListBox(true);
        for (int i = 0; i < 30; i++)
            ageListBox.addItem(i + " year", i);

        final DataGridColumnDescriptor<Pony, String> ageColumnDescriptor = new DataGridColumnDescriptor<>();
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));
        final ComplexHeaderCellRenderer ageHeaderCellRender = new ComplexHeaderCellRenderer("Age",
            new ListBoxFormField<Integer>(ageListBox), "age");
        ageHeaderCellRender.addFilterListener(dataProvider);
        ageColumnDescriptor.setHeaderCellRenderer(ageHeaderCellRender);
        ageColumnDescriptor.setCellRenderer(new LabelCellRenderer<String>());
        ageColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        final DataGridColumnDescriptor<Pony, String> raceColumnDescriptor = new DataGridColumnDescriptor<>();
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
        final ComplexHeaderCellRenderer raceHeaderCellRender = new ComplexHeaderCellRenderer("Race", new StringTextBoxFormField(),
            "race");
        raceHeaderCellRender.addFilterListener(dataProvider);
        raceColumnDescriptor.setHeaderCellRenderer(raceHeaderCellRender);
        raceColumnDescriptor.setCellRenderer(new LabelCellRenderer<String>());
        raceColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        dataGrid.addDataGridColumnDescriptor(selectColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(nameColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(ageColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(raceColumnDescriptor);

        dataProvider.registerHasCriteria(ageHeaderCellRender);
        dataProvider.registerHasCriteria(nameHeaderCellRender);
        dataProvider.registerHasCriteria(raceHeaderCellRender);

        formContainer.setWidget(0, 0, actionSelectorView);

        final PButton refresh = Element.newPButton("Refresh");
        refresh.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                dataProvider.onPageChange(0);
            }
        });
        formContainer.setWidget(0, 1, refresh);

        final PButton addPonyButton = Element.newPButton("Create new pony");
        addPonyButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                showCreatePonyPopup();
            }

        });
        // addPonyButton.addStyleName(PonySDKTheme.BUTTON_GREEN);
        formContainer.setWidget(0, 2, addPonyButton);

        formContainer.setWidget(0, 3, pager.asWidget());
        formContainer.setWidget(0, 4, infoSelectorView);

        // Build create pony form
        buildCreatePonyActivity();

        dataProvider.onPageChange(0);

        listContainer.setWidget(dataGrid);

    }

    @Override
    public void onSubmitForm(final SubmitFormEvent event) {
        //
        // final Result<List<Pony>> result = new FindPonysCommand(new
        // Query()).execute();
    }

    protected void showCreatePonyPopup() {
        final PDialogBox dialogBox = PConfirmDialog.show(getView().asWidget().getWindowID(), "Create pony", createPonyActivityPanel,
            "Create", "Cancel", new PConfirmDialogHandler() {

                @Override
                public void onCancel() {
                }

                @Override
                public boolean onOK(final PDialogBox p) {
                    return createPony.isValid();
                }
            });

        dialogBox.center();
    }

    private void buildCreatePonyActivity() {

        createPony = new Form();
        nameFormField = new StringTextBoxFormField();
        ageFormField = new IntegerTextBoxFormField();
        raceFormField = new StringTextBoxFormField();

        nameFormField.setValidator(new NotEmptyFieldValidator());
        raceFormField.setValidator(new NotEmptyFieldValidator());
        ageFormField.setValidator(new NotEmptyFieldValidator());
        ageFormField.setValidator(new IntegerFieldValidator());

        createPony.addFormField(nameFormField);
        createPony.addFormField(raceFormField);
        createPony.addFormField(ageFormField);

        createPonyActivityPanel = Element.newPFlexTable();
        createPonyActivityPanel.setWidget(0, 0, Element.newPLabel("Add a pony"));
        createPonyActivityPanel.getCellFormatter().setColSpan(0, 0, 3);
        createPonyActivityPanel.setWidget(1, 0, new FormFieldComponent("Name", nameFormField));
        createPonyActivityPanel.setWidget(1, 1, new FormFieldComponent("Race", raceFormField));
        createPonyActivityPanel.setWidget(1, 2, new FormFieldComponent("Age", ageFormField));
    }
}
