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
import com.ponysdk.core.place.Place;
import com.ponysdk.core.query.CriterionField;
import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Result;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.impl.webapplication.application.ApplicationView;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.client.datamodel.Pony;
import com.ponysdk.sample.command.pony.CreatePonyCommand;
import com.ponysdk.sample.command.pony.FindPonyChildsCommand;
import com.ponysdk.sample.command.pony.FindPonysCommand;
import com.ponysdk.sample.event.pony.PonyCreatedEvent;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.event.SubmitFormEvent;
import com.ponysdk.ui.server.form.event.SubmitFormHandler;
import com.ponysdk.ui.server.form.renderer.ListBoxFormFieldRenderer;
import com.ponysdk.ui.server.list.ComplexListActivity;
import com.ponysdk.ui.server.list.ComplexListCommandFactory;
import com.ponysdk.ui.server.list.ComplexListConfiguration;
import com.ponysdk.ui.server.list.ComplexListView;
import com.ponysdk.ui.server.list.DefaultComplexListView;
import com.ponysdk.ui.server.list.ExportConfiguration;
import com.ponysdk.ui.server.list.ListColumnDescriptor;
import com.ponysdk.ui.server.list.event.ShowSubListEvent;
import com.ponysdk.ui.server.list.event.ShowSubListHandler;
import com.ponysdk.ui.server.list.renderer.header.ComplexHeaderCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.BeanValueProvider;

public class ComplexListPageActivity extends PageActivity implements SubmitFormHandler, ShowSubListHandler<Pony> {

    @Autowired
    protected ApplicationView applicationView;
    private ComplexListActivity<Pony> complexListActivity;

    private final PListBox listBox = new PListBox(false, true);
    private CriterionField nameCriterion;
    private FormField nameField;
    private FormField ageField;

    public ComplexListPageActivity() {
        super("Complex List", "Rich UI Components");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(final Place place) {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {

        // Register handler
        addHandler(SubmitFormEvent.TYPE, this);
        addHandler(ShowSubListEvent.TYPE, this);

        // Layout the page with a search panel and a list panel
        final PSimplePanel searchPanel = new PSimplePanel();
        final PSimplePanel listPanel = new PSimplePanel();
        final PVerticalPanel layout = new PVerticalPanel();
        layout.setSizeFull();
        layout.add(searchPanel);
        layout.add(listPanel);
        pageView.getBody().setWidget(layout);

        final ListBoxFormFieldRenderer ageListBoxRenderer = new ListBoxFormFieldRenderer("Age");
        for (int i = 0; i < 15; i++) {
            ageListBoxRenderer.addItem(i + " year", i);
        }
        ageField = new FormField(ageListBoxRenderer);

        nameCriterion = new CriterionField("name");

        final ComplexListConfiguration<Pony> complexListConfiguration = new ComplexListConfiguration<Pony>();
        complexListConfiguration.setEnableForm(true);
        final PHorizontalPanel formPanel = new PHorizontalPanel();
        complexListConfiguration.setFormLayout(formPanel);
        complexListConfiguration.setShowSubListColumnEnabled(true);
        complexListConfiguration.setSelectionColumnEnabled(true);
        complexListConfiguration.setPageSize(40);
        complexListConfiguration.setTableName("ComplexList");
        complexListConfiguration.setExportConfiguration(initExportConfiguration());
        complexListConfiguration.setColumnDescriptors(initListColumnDescriptors());
        complexListConfiguration.setCustomColumnEnabled(true, Pony.class);
        complexListConfiguration.setShowPreferences(true);

        final ComplexListView complexListView = new DefaultComplexListView();
        complexListActivity = new ComplexListActivity<Pony>(complexListConfiguration, complexListView, getRootEventBus());

        complexListActivity.setCommandFactory(new ComplexListCommandFactory<Pony>() {

            @Override
            public Command newFindCommand(final ComplexListActivity<Pony> complexListActivity, final Query query) {

                return new Command() {

                    @Override
                    public void execute() {

                        new FindPonysCommand(query) {

                            @Override
                            protected void doAfterSuccess(final Result<List<Pony>> result) {
                                complexListActivity.setData(result);

                                listBox.clear();
                                for (Pony p : result.getData()) {
                                    listBox.addItem(p.getName(), p);
                                }
                            }
                        }.execute();
                    }
                };
            }

            @Override
            public Command newExportCommand(final ComplexListActivity<Pony> complexListActivity, final ExportContext<Pony> exportContext) {
                return new ExportCommand<Pony>(exportContext);
            };
        });

        complexListActivity.registerSearchCriteria(nameCriterion, nameField);
        complexListActivity.registerSearchCriteria(new CriterionField("age"), ageField);
        complexListActivity.start(listPanel);
        complexListActivity.getForm().addFormField(nameField);
        complexListActivity.getForm().addFormField(ageField);

        complexListView.setFloatableToolBar((PScrollPanel) pageView.getBody());

        final PButton addPonyButton = new PButton("Create new pony");
        addPonyButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                Pony pony = new Pony(null, "A dynamic pony", 1, "Equus ferus caballus");
                new CreatePonyCommand(pony) {

                    @Override
                    protected void doAfterSuccess(final Pony result) {
                        PonyCreatedEvent event = new PonyCreatedEvent(this, result);
                        event.setBusinessMessage("Pony '" + result.getName() + "' has been added");
                        fireEvent(event);
                    }
                }.execute();
            }
        });
        addPonyButton.addStyleName(PonySDKTheme.BUTTON_GREEN);
        complexListView.getToolbarLayout().add(addPonyButton);

        // Load initial datas
        complexListActivity.refresh();

        listBox.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final Object source, final int selectedIndex) {
                final List<Pony> list = new ArrayList<Pony>();
                for (final int index : listBox.getSelectedItems()) {
                    list.add((Pony) listBox.getValue(index));
                    complexListActivity.enableSelectedData((Pony) listBox.getValue(index), false);
                    complexListActivity.updateData((Pony) listBox.getValue(index));
                }

                complexListActivity.setSelectedData(list);
            }
        });

        complexListActivity.getComplexListView().getToolbarLayout().add(new PLabel("SelectData : "));
        complexListActivity.getComplexListView().getToolbarLayout().add(listBox);

    }

    private ExportConfiguration initExportConfiguration() {
        final ExportConfiguration exportConfiguration = new ExportConfiguration();
        exportConfiguration.addExportableField(new ExportableField("name", "Name"));
        exportConfiguration.addExportableField(new ExportableField("age", "Age"));
        exportConfiguration.addExportableField(new ExportableField("race", "type"));
        return exportConfiguration;
    }

    private List<ListColumnDescriptor<Pony, ?>> initListColumnDescriptors() {
        final List<ListColumnDescriptor<Pony, ?>> listColumnDescriptors = new ArrayList<ListColumnDescriptor<Pony, ?>>();

        final ListColumnDescriptor<Pony, String> nameColumnDescriptor = new ListColumnDescriptor<Pony, String>();

        nameField = new FormField();

        final ComplexHeaderCellRenderer nameHeaderCellRenderer = new ComplexHeaderCellRenderer("Name", nameField, "name");
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        nameColumnDescriptor.setHeaderCellRenderer(nameHeaderCellRenderer);
        listColumnDescriptors.add(nameColumnDescriptor);

        final ListColumnDescriptor<Pony, String> ageColumnDescriptor = new ListColumnDescriptor<Pony, String>("Age");
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));

        final ComplexHeaderCellRenderer headerCellRenderer = new ComplexHeaderCellRenderer("Age", ageField, "age");
        ageColumnDescriptor.setHeaderCellRenderer(headerCellRenderer);

        listColumnDescriptors.add(ageColumnDescriptor);

        final ListColumnDescriptor<Pony, String> raceColumnDescriptor = new ListColumnDescriptor<Pony, String>("Race");
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
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
            new FindPonyChildsCommand(event.getData().getId()) {

                @Override
                protected void doAfterSuccess(final Result<List<Pony>> result) {
                    complexListActivity.insertSubList(event.getRow(), result.getData());
                }
            };
        } else {
            complexListActivity.removeSubList(event.getRow());
        }
    }

}
