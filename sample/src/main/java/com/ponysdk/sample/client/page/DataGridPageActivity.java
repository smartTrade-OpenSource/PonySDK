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
import com.ponysdk.core.query.Result;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.sample.client.datamodel.Pony;
import com.ponysdk.sample.command.pony.CreatePonyCommand;
import com.ponysdk.sample.command.pony.FindPonysCommand;
import com.ponysdk.sample.event.pony.PonyCreatedEvent;
import com.ponysdk.ui.server.basic.IsPWidget;
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
import com.ponysdk.ui.server.form.event.SubmitFormEvent;
import com.ponysdk.ui.server.form.event.SubmitFormHandler;
import com.ponysdk.ui.server.form2.Form;
import com.ponysdk.ui.server.form2.FormFieldComponent;
import com.ponysdk.ui.server.form2.formfield.IntegerFieldValidator;
import com.ponysdk.ui.server.form2.formfield.IntegerTextBoxFormField;
import com.ponysdk.ui.server.form2.formfield.ListBoxFormField;
import com.ponysdk.ui.server.form2.formfield.StringTextBoxFormField;
import com.ponysdk.ui.server.form2.formfield.TextBoxFormField;
import com.ponysdk.ui.server.form2.validator.NotEmptyFieldValidator;
import com.ponysdk.ui.server.list2.DataGridActivity;
import com.ponysdk.ui.server.list2.DataGridColumnDescriptor;
import com.ponysdk.ui.server.list2.DefaultSimpleListView;
import com.ponysdk.ui.server.list2.dataprovider.RemoteDataProvider;
import com.ponysdk.ui.server.list2.paging.DefaultPagerView;
import com.ponysdk.ui.server.list2.paging.Pager;
import com.ponysdk.ui.server.list2.renderer.cell.CellRenderer;
import com.ponysdk.ui.server.list2.renderer.cell.LabelCellRenderer;
import com.ponysdk.ui.server.list2.renderer.header.ComplexHeaderCellRenderer;
import com.ponysdk.ui.server.list2.renderer.header.FilterableHeaderCellRenderer;
import com.ponysdk.ui.server.list2.renderer.header.HeaderCellRenderer;
import com.ponysdk.ui.server.list2.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list2.selector.CompositeSelectorView;
import com.ponysdk.ui.server.list2.selector.DefaultActionSelectorView;
import com.ponysdk.ui.server.list2.selector.DefaultInfoSelectorView;
import com.ponysdk.ui.server.list2.selector.Selector;
import com.ponysdk.ui.server.list2.selector.SelectorCheckBox;
import com.ponysdk.ui.server.list2.valueprovider.BeanValueProvider;
import com.ponysdk.ui.server.list2.valueprovider.IdentityValueProvider;
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
                final List<Pony> copy = new ArrayList<Pony>();
                final List<Pony> ponys = new FindPonysCommand(new Query()).execute().getData();
                for (int j = 0; j < 3; j++) {
                    final Pony src = ponys.get(j);
                    final Pony p = new Pony(src.getId(), "Copy-" + src.getName(), src.getAge(), src.getRace());
                    copy.add(p);
                }
                dataGrid.insertSubList(i, copy);
            }
        });

        final PButton remove = new PButton("Remove");
        remove.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Integer i = Integer.parseInt(line.getText());
                dataGrid.removeSubList(i);
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
                final Result<List<Pony>> result = new FindPonysCommand(query).execute();
                final List<Pony> data = result.getData();
                final int fullSize = result.getFullSize();
                pager.process(fullSize);
                selector.reset();
                selector.setPageSize(data.size());
                selector.setFullSize(fullSize);
                return data;
            }

            @Override
            protected List<Pony> getFullData(final Query query) {
                return new FindPonysCommand(query).execute().getData();
            }

        };

        final DataGridColumnDescriptor<Pony, Pony> selectColumnDescriptor = new DataGridColumnDescriptor<Pony, Pony>();
        selectColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Select"));
        selectColumnDescriptor.setValueProvider(new IdentityValueProvider<Pony>());
        final CellRenderer<Pony> selectCellRenderer = new CellRenderer<Pony>() {

            @Override
            public IsPWidget render(final int row, final Pony value) {
                final SelectorCheckBox<Pony> selectorCheckBox = new SelectorCheckBox<Pony>();
                selectorCheckBox.setData(value);
                selectorCheckBox.addSelectableListener(selector);
                selector.registerSelectable(selectorCheckBox);

                selectorCheckBox.addValueChangeHandler(new PValueChangeHandler<Boolean>() {

                    @Override
                    public void onValueChange(final PValueChangeEvent<Boolean> event) {
                        if (event.getValue()) {
                            selectorCheckBox.onCheck();
                            dataGrid.selectRow(row);

                        } else {
                            selectorCheckBox.onUncheck();
                            dataGrid.unSelectRow(row);

                        }
                    }
                });

                return selectorCheckBox;
            }
        };
        selectColumnDescriptor.setCellRenderer(selectCellRenderer);
        selectColumnDescriptor.setSubCellRenderer(selectCellRenderer);

        final DataGridColumnDescriptor<Pony, Pony> descriptor = new DataGridColumnDescriptor<Pony, Pony>();
        descriptor.setHeaderCellRenderer(new HeaderCellRenderer() {

            @Override
            public IsPWidget render() {
                return new PLabel();
            }
        });

        final DataGridColumnDescriptor<Pony, String> nameColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        final ComplexHeaderCellRenderer nameHeaderCellRender = new FilterableHeaderCellRenderer("Name", new StringTextBoxFormField(), "name");
        nameHeaderCellRender.addFilterListener(dataProvider);
        nameColumnDescriptor.setHeaderCellRenderer(nameHeaderCellRender);
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setCellRenderer(new LabelCellRenderer<String>());
        nameColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        nameColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        final PListBox ageListBox = new PListBox(true);
        for (int i = 0; i < 30; i++)
            ageListBox.addItem(i + " year", i);

        final DataGridColumnDescriptor<Pony, String> ageColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));
        final ComplexHeaderCellRenderer ageHeaderCellRender = new ComplexHeaderCellRenderer("Age", new ListBoxFormField<Integer>(ageListBox), "age");
        ageHeaderCellRender.addFilterListener(dataProvider);
        ageColumnDescriptor.setHeaderCellRenderer(ageHeaderCellRender);
        ageColumnDescriptor.setCellRenderer(new LabelCellRenderer<String>());
        ageColumnDescriptor.setSubCellRenderer(new LabelCellRenderer<String>());

        final DataGridColumnDescriptor<Pony, String> raceColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
        final ComplexHeaderCellRenderer raceHeaderCellRender = new ComplexHeaderCellRenderer("Race", new StringTextBoxFormField(), "race");
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
        addPonyButton.addStyleName(PonySDKTheme.BUTTON_GREEN);
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
        // final Result<List<Pony>> result = new FindPonysCommand(new Query()).execute();
    }

    protected void showCreatePonyPopup() {
        final PDialogBox dialogBox = PConfirmDialog.show("Create pony", createPonyActivityPanel, "Create", "Cancel", new PConfirmDialogHandler() {

            @Override
            public void onCancel() {}

            @Override
            public boolean onOK(final PDialogBox p) {
                if (createPony.isValid()) {
                    final Pony pony = new Pony(null, nameFormField.getValue(), ageFormField.getValue(), raceFormField.getValue());
                    final CreatePonyCommand command = new CreatePonyCommand(pony);
                    final Pony newPony = command.execute();
                    if (command.isSuccessfull()) {
                        final PonyCreatedEvent event = new PonyCreatedEvent(this, newPony);
                        event.setBusinessMessage("Pony '" + newPony.getName() + "' has been added");
                        fireEvent(event);
                    }
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
