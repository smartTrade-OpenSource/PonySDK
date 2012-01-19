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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.ponysdk.core.command.AbstractServiceCommand;
import com.ponysdk.core.command.Command;
import com.ponysdk.core.export.ExportContext;
import com.ponysdk.core.export.ExportableField;
import com.ponysdk.core.export.command.ExternalExportCommand;
import com.ponysdk.core.place.Place;
import com.ponysdk.core.query.CriterionField;
import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Query.QueryMode;
import com.ponysdk.core.query.Result;
import com.ponysdk.impl.query.memory.FilteringTools;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.impl.webapplication.application.ApplicationView;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.client.page.ComplexListPageActivity.Pony;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.form.FormField;
import com.ponysdk.ui.server.form.event.SubmitFormEvent;
import com.ponysdk.ui.server.form.event.SubmitFormHandler;
import com.ponysdk.ui.server.form.renderer.DateTimeBoxFormFieldRenderer;
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
import com.ponysdk.ui.server.list.renderer.header.DateRangeHeaderCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.BeanValueProvider;

public class ComplexListPageActivity extends PageActivity implements SubmitFormHandler, ShowSubListHandler<Pony> {

    @Autowired
    protected ApplicationView applicationView;

    private final List<Pony> ponyList = new ArrayList<ComplexListPageActivity.Pony>();

    private ComplexListActivity<Pony> complexListActivity;

    private Result<List<Pony>> result;

    private final PListBox listBox = new PListBox(false, true);

    private CriterionField nameCriterion;

    private FormField ageField;

    private FormField field1;

    private FormField field2;

    public ComplexListPageActivity() {
        super("Complex List", "Rich UI Components");
        loadData();
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(Place place) {}

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

        final FormField nameField = new FormField("Name");
        final ListBoxFormFieldRenderer ageListBoxRenderer = new ListBoxFormFieldRenderer("Age");
        ageListBoxRenderer.addItem("1 year");
        ageListBoxRenderer.addItem("2 years");
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

        complexListActivity.setCommandFactory(new ComplexListCommandFactory<ComplexListPageActivity.Pony>() {

            @Override
            public Command newFindCommand(final ComplexListActivity<Pony> complexListActivity, final Query query) {
                return new Command() {

                    @Override
                    public void execute() {
                        result = getResult(query);
                        for (final Pony pony : result.getData()) {
                            listBox.addItem(pony.getName(), pony);
                        }
                        complexListActivity.setData(result);
                    }
                };
            }

            @Override
            public Command newExportCommand(ComplexListActivity<Pony> complexListActivity, final ExportContext<Pony> exportContext) {
                final AbstractServiceCommand<Result<List<Pony>>> findCommand = new AbstractServiceCommand<Result<List<Pony>>>() {

                    @Override
                    protected Result<List<Pony>> execute0() throws Exception {
                        return getResult(exportContext.getQuery());
                    }

                    @Override
                    protected void doAfterSuccess(Result<List<Pony>> result) {

                    }
                };

                return new ExternalExportCommand<Pony, Result<List<Pony>>>("pony", exportContext, findCommand);
            };
        });

        complexListActivity.registerSearchCriteria(new CriterionField("AZ"), field1);
        complexListActivity.registerSearchCriteria(new CriterionField("AD"), field2);
        complexListActivity.registerSearchCriteria(nameCriterion, nameField);
        complexListActivity.registerSearchCriteria(new CriterionField("age"), ageField);
        complexListActivity.start(listPanel);
        complexListActivity.getForm().addFormField(nameField);
        complexListActivity.getForm().addFormField(ageField);

        complexListView.setFloatableToolBar((PScrollPanel) pageView.getBody());

        final PButton addPonyButton = new PButton("Create new pony");
        addPonyButton.addStyleName(PonySDKTheme.BUTTON_GREEN);
        complexListView.getToolbarLayout().add(addPonyButton);

        // Load initial datas
        complexListActivity.refresh();

        listBox.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(Object source, int selectedIndex) {
                final List<Pony> list = new ArrayList<Pony>();
                for (final int index : listBox.getSelectedItems()) {
                    list.add((Pony) listBox.getValue(index));
                    complexListActivity.enableSelectedData((Pony) listBox.getValue(index), false);
                }

                complexListActivity.setSelectedData(list);
            }
        });

        complexListActivity.getComplexListView().getToolbarLayout().add(new PLabel("SelectData : "));
        complexListActivity.getComplexListView().getToolbarLayout().add(listBox);

    }

    protected Result<List<Pony>> getResult(Query query) {
        List<Pony> searchResult = FilteringTools.filter(ponyList, query.getCriteria());
        final Result<List<Pony>> result = new Result<List<Pony>>();
        result.setFullSize(searchResult.size());
        if (!QueryMode.FULL_RESULT.equals(query.getQueryMode())) {
            searchResult = FilteringTools.getPage(query.getPageSize(), query.getPageNum(), searchResult);
        }
        result.setData(searchResult);
        return result;
    }

    private ExportConfiguration initExportConfiguration() {
        final ExportConfiguration exportConfiguration = new ExportConfiguration();
        exportConfiguration.addExportableField(new ExportableField("name", "Name"));
        exportConfiguration.addExportableField(new ExportableField("age", "Age"));
        exportConfiguration.addExportableField(new ExportableField("race", "type"));
        // exportConfiguration.addExportType(ExportType.CSV);
        // exportConfiguration.addExportType(ExportType.PDF);
        // exportConfiguration.addExportType(ExportType.XML);
        return exportConfiguration;
    }

    private List<ListColumnDescriptor<Pony, ?>> initListColumnDescriptors() {
        final List<ListColumnDescriptor<Pony, ?>> listColumnDescriptors = new ArrayList<ListColumnDescriptor<Pony, ?>>();

        final ListColumnDescriptor<ComplexListPageActivity.Pony, String> nameColumnDescriptor = new ListColumnDescriptor<ComplexListPageActivity.Pony, String>();

        field1 = new FormField(new DateTimeBoxFormFieldRenderer());
        field2 = new FormField(new DateTimeBoxFormFieldRenderer());

        DateRangeHeaderCellRenderer headerCellRender = new DateRangeHeaderCellRenderer("Caption", field1, field2, "");
        headerCellRender.setDateFormat(new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss"));

        nameColumnDescriptor.setHeaderCellRenderer(headerCellRender);
        nameColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("name"));
        listColumnDescriptors.add(nameColumnDescriptor);

        final ListColumnDescriptor<ComplexListPageActivity.Pony, String> ageColumnDescriptor = new ListColumnDescriptor<ComplexListPageActivity.Pony, String>("Age");
        ageColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("age"));

        final ComplexHeaderCellRenderer headerCellRenderer = new ComplexHeaderCellRenderer("Age", ageField, "age");
        ageColumnDescriptor.setHeaderCellRenderer(headerCellRenderer);

        listColumnDescriptors.add(ageColumnDescriptor);

        final ListColumnDescriptor<ComplexListPageActivity.Pony, String> raceColumnDescriptor = new ListColumnDescriptor<ComplexListPageActivity.Pony, String>("Race");
        raceColumnDescriptor.setValueProvider(new BeanValueProvider<Pony, String>("race"));
        listColumnDescriptors.add(raceColumnDescriptor);

        return listColumnDescriptors;
    }

    @Override
    public void onSubmitForm(SubmitFormEvent event) {
        complexListActivity.refresh();
    }

    private void loadData() {

        for (int i = 0; i < 10; i++) {

            ponyList.add(new Pony("Altai horseBengin", "2 years", "ferus caballus"));
            ponyList.add(new Pony("American Warmblood", "3 years", "Equus ferus caballus"));
            ponyList.add(new Pony("Falabella", "1 year", "Equus ferus caballus"));
            ponyList.add(new Pony("Friesian horse", "4 years", "Equus ferus caballus"));
            ponyList.add(new Pony("Mustang", "1 year", "Equus ferus caballus"));
            ponyList.add(new Pony("Altai horse", "2 years", "Equus ferus caballus"));
        }
    }

    public class Pony {

        private final String name;

        private final String age;

        private final String race;

        public Pony(String name, String age, String race) {
            this.name = name;
            this.age = age;
            this.race = race;
        }

        public String getName() {
            return name;
        }

        public String getAge() {
            return age;
        }

        public String getRace() {
            return race;
        }
    }

    @Override
    public void onShowSubList(ShowSubListEvent<Pony> event) {
        if (event.isShow()) {
            final List<Pony> subPonyList = new ArrayList<ComplexListPageActivity.Pony>();
            subPonyList.add(new Pony("SubPony 1", "7 years", "Equus ferus caballus"));
            subPonyList.add(new Pony("SubPony 2", "8 years", "Equus ferus caballus"));
            subPonyList.add(new Pony("SubPony 3", "9 years", "Equus ferus caballus"));
            complexListActivity.insertSubList(event.getRow(), subPonyList);
        } else {
            complexListActivity.removeSubList(event.getRow());
        }
    }

}
