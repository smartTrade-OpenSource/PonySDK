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
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PConfirmDialogHandler;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.dataprovider.CriteriableField;
import com.ponysdk.ui.server.dataprovider.Pager;
import com.ponysdk.ui.server.dataprovider.QueryBuilder;
import com.ponysdk.ui.server.dataprovider.RemoteDataProvider;
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
import com.ponysdk.ui.server.list.renderer.cell.StringCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.BeanValueProvider;
import com.ponysdk.ui.server.list2.DataGridActivity;
import com.ponysdk.ui.server.list2.DataGridColumnDescriptor;
import com.ponysdk.ui.server.list2.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list2.paging.DefaultPagerView;
import com.ponysdk.ui.server.rich.PConfirmDialog;

public class DataGridPageActivity extends SamplePageActivity implements SubmitFormHandler, ShowSubListHandler<Pony> {

    @Autowired
    protected ApplicationView applicationView;

    private DataGridActivity<Pony> dataGrid;
    private TextBoxFormField<String> nameSearchField;
    private ListBoxFormField<Integer> ageSearchField;

    private PFlexTable createPonyActivityPanel;
    private Form createPony;
    private TextBoxFormField<String> raceFormField;
    private TextBoxFormField<String> nameFormField;
    private TextBoxFormField<Integer> ageFormField;

    private long count = 1000;

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
        final PHorizontalPanel testContainer = new PHorizontalPanel();
        layout.add(testContainer);
        layout.add(formContainer);
        layout.add(listContainer);
        scroll.setWidget(layout);
        examplePanel.setWidget(scroll);

        // Search
        final PListBox ageListBox = new PListBox();
        for (int i = 0; i < 30; i++)
            ageListBox.addItem(i + " year", i);

        ageSearchField = new ListBoxFormField<Integer>(ageListBox);
        nameSearchField = new StringTextBoxFormField();

        formContainer.setWidget(0, 0, new FormFieldComponent("Age", ageSearchField));
        formContainer.setWidget(0, 1, new FormFieldComponent("Name", nameSearchField));

        // Register handler
        addHandler(SubmitFormEvent.TYPE, this);
        addHandler(ShowSubListEvent.TYPE, this);

        final DataGridColumnDescriptor<Pony, String> nameColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        nameColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Name"));
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());

        final DataGridColumnDescriptor<Pony, String> ageColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));
        final StringHeaderCellRenderer ageHeaderCellRender = new StringHeaderCellRenderer("Age");
        ageColumnDescriptor.setHeaderCellRenderer(ageHeaderCellRender);
        ageColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());

        final DataGridColumnDescriptor<Pony, String> raceColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
        final StringHeaderCellRenderer raceHeaderCellRender = new StringHeaderCellRenderer("Race");
        raceColumnDescriptor.setHeaderCellRenderer(raceHeaderCellRender);
        raceColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());

        dataGrid = new DataGridActivity<Pony>(new DefaultSimpleListView());
        dataGrid.addDataGridColumnDescriptor(nameColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(ageColumnDescriptor);
        dataGrid.addDataGridColumnDescriptor(raceColumnDescriptor);

        final QueryBuilder<Pony> queryBuilder = new QueryBuilder<Pony>();
        queryBuilder.registerCriteriable(new CriteriableField(ageSearchField, "age"));
        queryBuilder.registerCriteriable(new CriteriableField(nameSearchField, "name"));

        final Pager<Pony> pager = new Pager<Pony>(new DefaultPagerView());

        final RemoteDataProvider<Pony> dataProvider = new RemoteDataProvider<Pony>(queryBuilder, pager, dataGrid) {

            @Override
            protected List<Pony> getData(final Query query) {
                final Result<List<Pony>> result = new FindPonysCommand(query).execute();
                return result.getData();
            }
        };

        final PButton refresh = new PButton("Refresh");
        refresh.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                dataProvider.onPageChange(0);
            }
        });
        formContainer.setWidget(1, 0, refresh);

        final PButton addPonyButton = new PButton("Create new pony");
        addPonyButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                showCreatePonyPopup();
            }

        });
        addPonyButton.addStyleName(PonySDKTheme.BUTTON_GREEN);
        formContainer.setWidget(1, 1, addPonyButton);

        // Build create pony form
        buildCreatePonyActivity();

        //
        listContainer.setWidget(dataGrid);

        final PTextBox rowIndex = new PTextBox();
        final PButton addRow = new PButton("Add");
        addRow.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final int index = Integer.parseInt(rowIndex.getValue());
                dataGrid.insertData(index, new Pony(count++, "Pony 1", index, "Race"));
            }
        });

        final PButton removeRow = new PButton("Remove");
        removeRow.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final int index = Integer.parseInt(rowIndex.getValue());
                dataGrid.remove(index);
            }
        });

        testContainer.add(rowIndex);
        testContainer.add(addRow);
        testContainer.add(removeRow);
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
