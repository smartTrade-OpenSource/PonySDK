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

import com.ponysdk.core.command.Command;
import com.ponysdk.core.export.ExportContext;
import com.ponysdk.core.export.ExportableField;
import com.ponysdk.core.export.command.ExportCommand;
import com.ponysdk.core.query.Criterion;
import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Result;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.impl.webapplication.application.ApplicationView;
import com.ponysdk.sample.client.datamodel.Pony;
import com.ponysdk.sample.command.pony.CreatePonyCommand;
import com.ponysdk.sample.command.pony.FindPonyChildsCommand;
import com.ponysdk.sample.command.pony.FindPonysCommand;
import com.ponysdk.sample.event.pony.PonyCreatedEvent;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PConfirmDialogHandler;
import com.ponysdk.ui.server.basic.PDialogBox;
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
import com.ponysdk.ui.server.list.ComplexListActivity;
import com.ponysdk.ui.server.list.ComplexListCommandFactory;
import com.ponysdk.ui.server.list.ComplexListConfiguration;
import com.ponysdk.ui.server.list.ComplexListView;
import com.ponysdk.ui.server.list.DefaultComplexListView;
import com.ponysdk.ui.server.list.ExportConfiguration;
import com.ponysdk.ui.server.list.ListColumnDescriptor;
import com.ponysdk.ui.server.list.event.ShowSubListEvent;
import com.ponysdk.ui.server.list.event.ShowSubListHandler;
import com.ponysdk.ui.server.list.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.BeanValueProvider;
import com.ponysdk.ui.server.rich.PConfirmDialog;

public class ComplexListPageActivity extends SamplePageActivity implements SubmitFormHandler, ShowSubListHandler<Pony> {

    @Autowired
    protected ApplicationView applicationView;

    private ComplexListActivity<Pony> complexListActivity;
    private FormField nameSearchField;
    private FormField ageSearchField;

    private PSimplePanel createPonyActivityPanel;
    private FormActivity createPonyActivity;
    private FormField raceFormField;
    private FormField nameFormField;
    private FormField ageFormField;

    public ComplexListPageActivity() {
        super("Complex List", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        final PScrollPanel scrolPanel = new PScrollPanel();
        examplePanel.setWidget(scrolPanel);

        // Register handler
        addHandler(SubmitFormEvent.TYPE, this);
        addHandler(ShowSubListEvent.TYPE, this);

        final ComplexListConfiguration<Pony> complexListConfiguration = new ComplexListConfiguration<Pony>();
        complexListConfiguration.setEnableForm(true);
        complexListConfiguration.setFormLayout(new PHorizontalPanel());
        complexListConfiguration.setShowSubListColumnEnabled(true);
        complexListConfiguration.setSelectionColumnEnabled(true);
        complexListConfiguration.setPageSize(20);
        complexListConfiguration.setTableName("Pony List");
        complexListConfiguration.setExportConfiguration(initExportConfiguration());
        complexListConfiguration.setColumnDescriptors(initListColumnDescriptors());
        complexListConfiguration.setCustomColumnEnabled(true, Pony.class);
        complexListConfiguration.setShowPreferences(true);

        final ComplexListView complexListView = new DefaultComplexListView();
        complexListView.setFloatableToolBar(scrolPanel);

        complexListActivity = new ComplexListActivity<Pony>(complexListConfiguration, complexListView, getRootEventBus());

        complexListActivity.registerSearchCriteria(new Criterion("name"), nameSearchField);
        complexListActivity.registerSearchCriteria(new Criterion("age"), ageSearchField);

        complexListActivity.setCommandFactory(new ComplexListCommandFactory<Pony>() {

            @Override
            public Command<Result<List<Pony>>> newFindCommand(final ComplexListActivity<Pony> complexListActivity, final Query query) {
                return new FindPonysCommand(query) {

                    @Override
                    protected void doAfterSuccess(final Result<List<Pony>> result) {
                        complexListActivity.setData(result);
                    }
                };
            }

            @Override
            public Command<String> newExportCommand(final ComplexListActivity<Pony> complexListActivity, final ExportContext<Pony> exportContext) {
                return new ExportCommand<Pony>(exportContext);
            }

        });

        complexListActivity.start(scrolPanel);

        complexListActivity.getForm().addFormField(nameSearchField);
        complexListActivity.getForm().addFormField(ageSearchField);

        final PButton addPonyButton = new PButton("Create new pony");
        addPonyButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                showCreatePonyPopup();
            }

        });
        addPonyButton.addStyleName(PonySDKTheme.BUTTON_GREEN);
        complexListView.getToolbarLayout().add(addPonyButton);

        // Load initial datas
        complexListActivity.refresh();

        // Build create pony form
        buildCreatePonyActivity();
    }

    private ExportConfiguration initExportConfiguration() {
        final ExportConfiguration exportConfiguration = new ExportConfiguration();
        exportConfiguration.addExportableField(new ExportableField("name", "Name"));
        exportConfiguration.addExportableField(new ExportableField("age", "Age"));
        exportConfiguration.addExportableField(new ExportableField("race", "type"));
        return exportConfiguration;
    }

    private List<ListColumnDescriptor<Pony, ?>> initListColumnDescriptors() {

        final ListBoxFormFieldRenderer ageListBoxRenderer = new ListBoxFormFieldRenderer("Age");
        for (int i = 0; i < 30; i++)
            ageListBoxRenderer.addItem(i + " year", i);

        ageSearchField = new FormField(ageListBoxRenderer);
        nameSearchField = new FormField(new TextBoxBaseFormFieldRenderer("Name"));

        final List<ListColumnDescriptor<Pony, ?>> listColumnDescriptors = new ArrayList<ListColumnDescriptor<Pony, ?>>();

        final ListColumnDescriptor<Pony, String> nameColumnDescriptor = new ListColumnDescriptor<Pony, String>();
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Name", "name"));
        listColumnDescriptors.add(nameColumnDescriptor);

        final ListColumnDescriptor<Pony, String> ageColumnDescriptor = new ListColumnDescriptor<Pony, String>("Age");
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));
        ageColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Age", "age"));
        listColumnDescriptors.add(ageColumnDescriptor);

        final ListColumnDescriptor<Pony, String> raceColumnDescriptor = new ListColumnDescriptor<Pony, String>("Race");
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
        raceColumnDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Race"));
        listColumnDescriptors.add(raceColumnDescriptor);

        return listColumnDescriptors;
    }

    @Override
    public void onSubmitForm(final SubmitFormEvent event) {
        complexListActivity.refresh();
    }

    @Override
    public void onShowSubList(final ShowSubListEvent<Pony> event) {
        if (event.isShow()) {
            final FindPonyChildsCommand command = new FindPonyChildsCommand(event.getData().getId());
            final Result<List<Pony>> result = command.execute();
            if (command.isSuccessfull()) {
                complexListActivity.insertSubList(event.getRow(), result.getData());
            }
        } else {
            complexListActivity.removeSubList(event.getRow());
        }
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
