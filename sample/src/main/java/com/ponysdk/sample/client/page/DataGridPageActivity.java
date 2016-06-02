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

import com.ponysdk.core.query.Query;
import com.ponysdk.sample.client.datamodel.Pony;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PConfirmDialogHandler;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;
import com.ponysdk.ui.server.form.Form;
import com.ponysdk.ui.server.form.FormFieldComponent;
import com.ponysdk.ui.server.form.event.SubmitFormEvent;
import com.ponysdk.ui.server.form.event.SubmitFormHandler;
import com.ponysdk.ui.server.form.formfield.IntegerTextBoxFormField;
import com.ponysdk.ui.server.form.formfield.ListBoxFormField;
import com.ponysdk.ui.server.form.formfield.StringTextBoxFormField;
import com.ponysdk.ui.server.form.formfield.TextBoxFormField;
import com.ponysdk.ui.server.form.validator.IntegerFieldValidator;
import com.ponysdk.ui.server.form.validator.NotEmptyFieldValidator;
import com.ponysdk.ui.server.list.DataGridActivity;
import com.ponysdk.ui.server.list.DataGridColumnDescriptor;
import com.ponysdk.ui.server.list.DefaultSimpleListView;
import com.ponysdk.ui.server.list.dataprovider.RemoteDataProvider;
import com.ponysdk.ui.server.list.paging.DefaultPagerView;
import com.ponysdk.ui.server.list.paging.Pager;
import com.ponysdk.ui.server.list.refreshable.Cell;
import com.ponysdk.ui.server.list.renderer.cell.CellRenderer;
import com.ponysdk.ui.server.list.renderer.cell.LabelCellRenderer;
import com.ponysdk.ui.server.list.renderer.header.ComplexHeaderCellRenderer;
import com.ponysdk.ui.server.list.renderer.header.FilterableHeaderCellRenderer;
import com.ponysdk.ui.server.list.renderer.header.HeaderCellRenderer;
import com.ponysdk.ui.server.list.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list.selector.CompositeSelectorView;
import com.ponysdk.ui.server.list.selector.DefaultActionSelectorView;
import com.ponysdk.ui.server.list.selector.DefaultInfoSelectorView;
import com.ponysdk.ui.server.list.selector.Selector;
import com.ponysdk.ui.server.list.selector.SelectorCheckBox;
import com.ponysdk.ui.server.list.valueprovider.BeanValueProvider;
import com.ponysdk.ui.server.list.valueprovider.IdentityValueProvider;
import com.ponysdk.ui.server.rich.PConfirmDialog;

public class DataGridPageActivity extends SamplePageActivity implements SubmitFormHandler {

    private DataGridActivity<Pony> dataGrid;

    private PFlexTable createPonyActivityPanel;
    private Form createPony;
    private TextBoxFormField<String> raceFormField;
    private TextBoxFormField<String> nameFormField;
    private TextBoxFormField<Integer> ageFormField;

    final PTextBox line = new PTextBox();

    public DataGridPageActivity() {
        super("Data Grid", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        final PScrollPanel scroll = new PScrollPanel();
        final PFlowPanel layout = new PFlowPanel();
        final PFlexTable formContainer = new PFlexTable();
        final PSimplePanel listContainer = new PSimplePanel();
        layout.add(formContainer);

        final PButton add = new PButton("Add");
        add.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Integer i = Integer.parseInt(line.getText());
                final Pony father = dataGrid.getVisibleItem(i);
                final List<Pony> copy = new ArrayList<Pony>();
                for (int j = 0; j < 3; j++) {
                    final Pony p = new Pony(father.getId(), "Copy-" + father.getName(), father.getAge(),
                            father.getRace());
                    copy.add(p);
                }
                dataGrid.insertSubList(father, copy);
            }
        });

        final PButton remove = new PButton("Remove");
        remove.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Integer i = Integer.parseInt(line.getText());
                final Pony father = dataGrid.getVisibleItem(i);
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

        final Pager<Pony> pager = new Pager<Pony>(new DefaultPagerView());
        dataGrid = new DataGridActivity<Pony>(new DefaultSimpleListView());

        final DefaultActionSelectorView actionSelectorView = new DefaultActionSelectorView();
        final DefaultInfoSelectorView infoSelectorView = new DefaultInfoSelectorView();
        final CompositeSelectorView selectorView = new CompositeSelectorView(actionSelectorView, infoSelectorView);
        final Selector<Pony> selector = new Selector<Pony>(selectorView);

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

        final DataGridColumnDescriptor<Pony, Pony, SelectorCheckBox<Pony>> selectColumnDescriptor = new DataGridColumnDescriptor<>();
        selectColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Select"));
        selectColumnDescriptor.setValueProvider(new IdentityValueProvider<Pony>());
        final CellRenderer<Pony, SelectorCheckBox<Pony>> selectCellRenderer = new CellRenderer<Pony, SelectorCheckBox<Pony>>() {

            @Override
            public SelectorCheckBox<Pony> render(final int row, final Pony value) {
                final SelectorCheckBox<Pony> selectorCheckBox = new SelectorCheckBox<Pony>();
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

        final DataGridColumnDescriptor<Pony, Pony, PLabel> descriptor = new DataGridColumnDescriptor<>();
        descriptor.setHeaderCellRenderer(new HeaderCellRenderer() {

            @Override
            public PLabel render() {
                return new PLabel();
            }
        });

        final DataGridColumnDescriptor<Pony, String, PLabel> nameColumnDescriptor = new DataGridColumnDescriptor<>();
        final ComplexHeaderCellRenderer nameHeaderCellRender = new FilterableHeaderCellRenderer("Name", new StringTextBoxFormField(),
                "name");
        nameHeaderCellRender.addFilterListener(dataProvider);
        nameColumnDescriptor.setHeaderCellRenderer(nameHeaderCellRender);
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setCellRenderer(new LabelCellRenderer<String>());
        nameColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        nameColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        final PListBox ageListBox = new PListBox(true);
        for (int i = 0; i < 30; i++)
            ageListBox.addItem(i + " year", i);

        final DataGridColumnDescriptor<Pony, String, PLabel> ageColumnDescriptor = new DataGridColumnDescriptor<>();
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));
        final ComplexHeaderCellRenderer ageHeaderCellRender = new ComplexHeaderCellRenderer("Age",
                new ListBoxFormField<Integer>(ageListBox), "age");
        ageHeaderCellRender.addFilterListener(dataProvider);
        ageColumnDescriptor.setHeaderCellRenderer(ageHeaderCellRender);
        ageColumnDescriptor.setCellRenderer(new LabelCellRenderer<String>());
        ageColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        final DataGridColumnDescriptor<Pony, String, PLabel> raceColumnDescriptor = new DataGridColumnDescriptor<>();
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

        final PButton refresh = new PButton("Refresh");
        refresh.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                dataProvider.onPageChange(0);
            }
        });
        formContainer.setWidget(0, 1, refresh);

        final PButton addPonyButton = new PButton("Create new pony");
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
        final PDialogBox dialogBox = PConfirmDialog.show("Create pony", createPonyActivityPanel, "Create", "Cancel",
                new PConfirmDialogHandler() {

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public boolean onOK(final PDialogBox p) {
                        if (createPony.isValid()) {
                            // final Pony pony = new Pony(null, nameFormField.getValue(), ageFormField.getValue(),
                            // raceFormField.getValue());
                            // final CreatePonyCommand command = new CreatePonyCommand(pony);
                            // final Pony newPony = command.execute();
                            // if (command.isSuccessfull()) {
                            // final PonyCreatedEvent event = new PonyCreatedEvent(this, newPony);
                            // event.setBusinessMessage("Pony '" + newPony.getName() + "' has been added");
                            // fireEvent(event);
                            // }
                            return true;
                        }
                        return false;
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

        createPonyActivityPanel = new PFlexTable();
        createPonyActivityPanel.setWidget(0, 0, new PLabel("Add a pony"));
        createPonyActivityPanel.getFlexCellFormatter().setColSpan(0, 0, 3);
        createPonyActivityPanel.setWidget(1, 0, new FormFieldComponent("Name", nameFormField));
        createPonyActivityPanel.setWidget(1, 1, new FormFieldComponent("Race", raceFormField));
        createPonyActivityPanel.setWidget(1, 2, new FormFieldComponent("Age", ageFormField));
    }
}
