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
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.form.DefaultFormView;
import com.ponysdk.ui.server.form.FormActivity;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.event.SubmitFormEvent;
import com.ponysdk.ui.server.form.event.SubmitFormHandler;
import com.ponysdk.ui.server.form.renderer.ListBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.TextBoxBaseFormFieldRenderer;
import com.ponysdk.ui.server.form.renderer.TextBoxFormFieldRenderer;
import com.ponysdk.ui.server.form.validator.IntegerFieldValidator;
import com.ponysdk.ui.server.form.validator.NotEmptyFieldValidator;
import com.ponysdk.ui.server.list.DefaultSimpleListView;
import com.ponysdk.ui.server.list.ExportConfiguration;
import com.ponysdk.ui.server.list.event.ShowSubListEvent;
import com.ponysdk.ui.server.list.event.ShowSubListHandler;
import com.ponysdk.ui.server.list.renderer.cell.StringCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.BeanValueProvider;
import com.ponysdk.ui.server.list2.DataGridActivity;
import com.ponysdk.ui.server.list2.DataGridColumnDescriptor;
import com.ponysdk.ui.server.list2.DataGridConfiguration;
import com.ponysdk.ui.server.list2.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.rich.PConfirmDialog;

public class DataGridPageActivity extends SamplePageActivity implements SubmitFormHandler, ShowSubListHandler<Pony> {

    @Autowired
    protected ApplicationView applicationView;

    private DataGridActivity<Pony> dataGrid;
    private FormField nameSearchField;
    private FormField ageSearchField;

    private PSimplePanel createPonyActivityPanel;
    private FormActivity createPonyActivity;
    private FormField raceFormField;
    private FormField nameFormField;
    private FormField ageFormField;

    public DataGridPageActivity() {
        super("Data Grid", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        final PHorizontalPanel formContainer = new PHorizontalPanel();
        final PScrollPanel gridContainer = new PScrollPanel();

        final PDockLayoutPanel dockLayoutPanel = new PDockLayoutPanel();
        dockLayoutPanel.addNorth(formContainer, 100);
        dockLayoutPanel.add(gridContainer);

        examplePanel.setWidget(dockLayoutPanel);

        // Register handler
        addHandler(SubmitFormEvent.TYPE, this);
        addHandler(ShowSubListEvent.TYPE, this);

        final DataGridConfiguration<Pony> configuration = new DataGridConfiguration<Pony>();
        configuration.setShowSubListEnabled(true);
        configuration.setSelectionEnabled(true);
        configuration.setColumnDescriptors(initListColumnDescriptors());

        dataGrid = new DataGridActivity<Pony>(configuration, new DefaultSimpleListView());
        dataGrid.start(gridContainer);

        final PButton addPonyButton = new PButton("Create new pony");
        addPonyButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                showCreatePonyPopup();
            }

        });
        addPonyButton.addStyleName(PonySDKTheme.BUTTON_GREEN);

        // Build create pony form
        buildCreatePonyActivity();

        //
        final Result<List<Pony>> result = new FindPonysCommand(new Query()).execute();
        dataGrid.setRowData(0, result.getData());
    }

    private ExportConfiguration initExportConfiguration() {
        final ExportConfiguration exportConfiguration = new ExportConfiguration();
        exportConfiguration.addExportableField(new ExportableField("name", "Name"));
        exportConfiguration.addExportableField(new ExportableField("age", "Age"));
        exportConfiguration.addExportableField(new ExportableField("race", "type"));
        return exportConfiguration;
    }

    private List<DataGridColumnDescriptor<Pony, ?>> initListColumnDescriptors() {

        final ListBoxFormFieldRenderer ageListBoxRenderer = new ListBoxFormFieldRenderer("Age");
        for (int i = 0; i < 30; i++)
            ageListBoxRenderer.addItem(i + " year", i);

        ageSearchField = new FormField(ageListBoxRenderer);
        nameSearchField = new FormField(new TextBoxBaseFormFieldRenderer("Name"));

        final List<DataGridColumnDescriptor<Pony, ?>> listColumnDescriptors = new ArrayList<DataGridColumnDescriptor<Pony, ?>>();

        final DataGridColumnDescriptor<Pony, String> nameColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        nameColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Name"));
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());

        listColumnDescriptors.add(nameColumnDescriptor);

        final DataGridColumnDescriptor<Pony, String> ageColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));
        final StringHeaderCellRenderer ageHeaderCellRender = new StringHeaderCellRenderer("Age");
        ageColumnDescriptor.setHeaderCellRenderer(ageHeaderCellRender);
        ageColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());
        listColumnDescriptors.add(ageColumnDescriptor);

        final DataGridColumnDescriptor<Pony, String> raceColumnDescriptor = new DataGridColumnDescriptor<Pony, String>();
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
        final StringHeaderCellRenderer raceHeaderCellRender = new StringHeaderCellRenderer("Race");
        raceColumnDescriptor.setHeaderCellRenderer(raceHeaderCellRender);
        raceColumnDescriptor.setCellRenderer(new StringCellRenderer<Pony, String>());
        listColumnDescriptors.add(raceColumnDescriptor);

        return listColumnDescriptors;
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
                if (createPonyActivity.isValid()) {
                    final Pony pony = new Pony(null, nameFormField.getStringValue(), ageFormField.getIntegerValue(), raceFormField.getStringValue());
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

        createPonyActivityPanel = new PSimplePanel();
        createPonyActivity = new FormActivity(new DefaultFormView("Create a Pony"));
        nameFormField = new FormField(new TextBoxFormFieldRenderer("Name"));
        ageFormField = new FormField(new TextBoxFormFieldRenderer("Age"));
        raceFormField = new FormField(new TextBoxFormFieldRenderer("Race"));

        nameFormField.addValidator(new NotEmptyFieldValidator());
        raceFormField.addValidator(new NotEmptyFieldValidator());
        ageFormField.addValidator(new NotEmptyFieldValidator());
        ageFormField.addValidator(new IntegerFieldValidator());

        createPonyActivity.addFormField(nameFormField);
        createPonyActivity.addFormField(ageFormField);
        createPonyActivity.addFormField(raceFormField);

        createPonyActivity.start(createPonyActivityPanel);
    }
}
