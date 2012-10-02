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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.export.ExportableField;
import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Result;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.impl.webapplication.application.ApplicationView;
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
import com.ponysdk.ui.server.list.DefaultSimpleListView;
import com.ponysdk.ui.server.list.ExportConfiguration;
import com.ponysdk.ui.server.list.event.ShowSubListEvent;
import com.ponysdk.ui.server.list.event.ShowSubListHandler;
import com.ponysdk.ui.server.list.renderer.cell.CellRenderer;
import com.ponysdk.ui.server.list.renderer.cell.StringCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.BeanValueProvider;
import com.ponysdk.ui.server.list.valueprovider.BooleanValueProvider;
import com.ponysdk.ui.server.list2.DataGridActivity;
import com.ponysdk.ui.server.list2.DataGridColumnDescriptor;
import com.ponysdk.ui.server.list2.dataprovider.RemoteDataProvider;
import com.ponysdk.ui.server.list2.header.ComplexHeaderCellRenderer;
import com.ponysdk.ui.server.list2.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list2.paging.DefaultPagerView;
import com.ponysdk.ui.server.list2.paging.Pager;
import com.ponysdk.ui.server.list2.selector.DefaultActionSelectorView;
import com.ponysdk.ui.server.list2.selector.DefaultSelectorInfoView;
import com.ponysdk.ui.server.list2.selector.Selector;
import com.ponysdk.ui.server.list2.selector.SelectorCheckBox;
import com.ponysdk.ui.server.rich.PConfirmDialog;

public class DataGridPageActivity extends SamplePageActivity implements SubmitFormHandler, ShowSubListHandler<Pony> {

    @Autowired
    protected ApplicationView applicationView;

    private DataGridActivity<Pony> dataGrid;

    private PFlexTable createPonyActivityPanel;
    private Form createPony;
    private TextBoxFormField<String> raceFormField;
    private TextBoxFormField<String> nameFormField;
    private TextBoxFormField<Integer> ageFormField;

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
        layout.add(listContainer);
        scroll.setWidget(layout);
        examplePanel.setWidget(scroll);

        // Register handler
        addHandler(SubmitFormEvent.TYPE, this);
        addHandler(ShowSubListEvent.TYPE, this);

        final Pager<Pony> pager = new Pager<Pony>(new DefaultPagerView());
        dataGrid = new DataGridActivity<Pony>(new DefaultSimpleListView());
        final Selector<Pony> selector = new Selector<Pony>(new DefaultActionSelectorView(), new DefaultSelectorInfoView());

        final RemoteDataProvider<Pony> dataProvider = new RemoteDataProvider<Pony>(pager, dataGrid) {

            @Override
            protected List<Pony> getData(final Query query) {
                final Result<List<Pony>> result = new FindPonysCommand(query).execute();
                final int fullSize = result.getFullSize();
                pager.process(fullSize);
                selector.reset();
                selector.setFullSize(fullSize);
                return result.getData();
            }

        };

        final DataGridColumnDescriptor<Pony, Boolean> selectColumnDescriptor = new DataGridColumnDescriptor<Pony, Boolean>();
        selectColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Select"));
        selectColumnDescriptor.setValueProvider(new BooleanValueProvider<Pony>(false));
        selectColumnDescriptor.setCellRenderer(new CellRenderer<Pony, Boolean>() {

            @Override
            public IsPWidget render(final int row, final Pony data, final Boolean value) {
                final SelectorCheckBox<Pony> selectorCheckBox = new SelectorCheckBox<Pony>();
                selectorCheckBox.setData(data);
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
        });

        final DataGridColumnDescriptor<Pony, String> nameColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        final ComplexHeaderCellRenderer nameHeaderCellRender = new ComplexHeaderCellRenderer("Name", new StringTextBoxFormField(), "name", dataProvider);
        nameColumnDescriptor.setHeaderCellRenderer(nameHeaderCellRender);
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());

        final PListBox ageListBox = new PListBox(true);
        for (int i = 0; i < 30; i++)
            ageListBox.addItem(i + " year", i);

        final DataGridColumnDescriptor<Pony, String> ageColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));
        final ComplexHeaderCellRenderer ageHeaderCellRender = new ComplexHeaderCellRenderer("Age", new ListBoxFormField<Integer>(ageListBox), "age", dataProvider);
        ageColumnDescriptor.setHeaderCellRenderer(ageHeaderCellRender);
        ageColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());

        final DataGridColumnDescriptor<Pony, String> raceColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
        final ComplexHeaderCellRenderer raceHeaderCellRender = new ComplexHeaderCellRenderer("Race", new StringTextBoxFormField(), "race", dataProvider);
        raceColumnDescriptor.setHeaderCellRenderer(raceHeaderCellRender);
        raceColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());

        dataGrid.addDataGridColumnDescriptor(selectColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(nameColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(ageColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(raceColumnDescriptor);

        dataProvider.registerHasCriteria(ageHeaderCellRender);
        dataProvider.registerHasCriteria(nameHeaderCellRender);
        dataProvider.registerHasCriteria(raceHeaderCellRender);

        formContainer.setWidget(0, 0, selector.getActionView());

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
        formContainer.setWidget(0, 4, selector.getInfoView());

        // Build create pony form
        buildCreatePonyActivity();

        dataProvider.onPageChange(0);

        listContainer.setWidget(dataGrid);

    }

    private ExportConfiguration initExportConfiguration() {
        final ExportConfiguration exportConfiguration = new ExportConfiguration();
        exportConfiguration.addExportableField(new ExportableField("name", "Name"));
        exportConfiguration.addExportableField(new ExportableField("age", "Age"));
        exportConfiguration.addExportableField(new ExportableField("race", "type"));
        return exportConfiguration;
    }

    @Override
    public void onSubmitForm(final SubmitFormEvent event) {
        //
        // final Result<List<Pony>> result = new FindPonysCommand(new Query()).execute();
    }

    @Override
    public void onShowSubList(final ShowSubListEvent<Pony> event) {
        // if (event.isShow()) {
        // final FindPonyChildsCommand command = new FindPonyChildsCommand(event.getData().getId());
        // final Result<List<Pony>> result = command.execute();
        // if (command.isSuccessful()) {
        // complexListActivity.insertSubList(event.getRow(), result.getData());
        // }
        // } else {
        // complexListActivity.removeSubList(event.getRow());
        // }
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
