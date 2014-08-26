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

package com.ponysdk.ui.server.list;

import com.ponysdk.ui.server.basic.PPanel;

@Deprecated
public class ComplexListConfiguration<T> extends ListConfiguration<T> {

    private int pageSize = 30;

    private boolean formEnabled;// FormConfiguration with Layout ?

    private PPanel formLayout;

    private ExportConfiguration<T> exportConfiguration;

    private boolean selectionColumnEnabled;

    private boolean showSubListColumnEnabled;

    private boolean searchFormMustBeValid;

    private boolean customColumnEnabled;

    private Class<T> clas;

    private boolean showPreferences;

    private ComplexListActivity<T> complexListActivity;

    public boolean isSearchFormMustBeValid() {
        return searchFormMustBeValid;
    }

    public void setSearchFormMustBeValid(final boolean searchFormMustBeValid) {
        this.searchFormMustBeValid = searchFormMustBeValid;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isEnableForm() {
        return formEnabled;
    }

    public void setEnableForm(final boolean enableForm) {
        this.formEnabled = enableForm;
    }

    public PPanel getFormLayout() {
        return formLayout;
    }

    public void setFormLayout(final PPanel formLayout) {
        this.formLayout = formLayout;
    }

    public ExportConfiguration<T> getExportConfiguration() {
        return exportConfiguration;
    }

    public void setExportConfiguration(final ExportConfiguration<T> exportConfiguration) {
        this.exportConfiguration = exportConfiguration;
    }

    public boolean isSelectionColumnEnabled() {
        return selectionColumnEnabled;
    }

    public void setSelectionColumnEnabled(final boolean isSelectionColumnEnabled) {
        this.selectionColumnEnabled = isSelectionColumnEnabled;
    }

    public boolean isShowSubListColumnEnabled() {
        return showSubListColumnEnabled;
    }

    public void setShowSubListColumnEnabled(final boolean showSubListColumnEnabled) {
        this.showSubListColumnEnabled = showSubListColumnEnabled;
    }

    public void setCustomColumnEnabled(final boolean customColumnEnabled, final Class<T> clas) {
        this.customColumnEnabled = customColumnEnabled;
        this.clas = clas;
    }

    public boolean isCustomColumnEnabled() {
        return customColumnEnabled;
    }

    public void setClas(final Class<T> clas) {
        this.clas = clas;
    }

    public Class<T> getClas() {
        return clas;
    }

    public void setShowPreferences(final boolean showPreferences) {
        this.showPreferences = showPreferences;
    }

    public boolean isShowPreferences() {
        return showPreferences;
    }

    public ComplexListActivity<T> getComplexListActivity() {
        return complexListActivity;
    }

    public void setComplexListActivity(final ComplexListActivity<T> complexListActivity) {
        this.complexListActivity = complexListActivity;
    }

    @Override
    public void addColumnDescriptor(final ListColumnDescriptor<T, ?> listColumnDescriptor) {
        if (this.complexListActivity != null) complexListActivity.addDescriptor(listColumnDescriptor);
        else super.addColumnDescriptor(listColumnDescriptor);
    }

    public boolean isFormEnabled() {
        return formEnabled;
    }

}
