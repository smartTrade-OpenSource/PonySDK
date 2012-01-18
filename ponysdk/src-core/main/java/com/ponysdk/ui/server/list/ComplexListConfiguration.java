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

public class ComplexListConfiguration<T> extends ListConfiguration<T> {

    private int pageSize = 30;
    private boolean formEnabled;// FormConfiguration with Layout ?
    private PPanel formLayout;
    private ExportConfiguration exportConfiguration;
    private boolean selectionColumnEnabled;
    private boolean showSubListColumnEnabled;
    private boolean searchFormMustBeValid;
    private boolean customColumnEnabled;
    private Class<T> clas;
	private boolean showPreferences;

    public boolean isSearchFormMustBeValid() {
        return searchFormMustBeValid;
    }

    public void setSearchFormMustBeValid(boolean searchFormMustBeValid) {
        this.searchFormMustBeValid = searchFormMustBeValid;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isEnableForm() {
        return formEnabled;
    }

    public void setEnableForm(boolean enableForm) {
        this.formEnabled = enableForm;
    }

    public PPanel getFormLayout() {
        return formLayout;
    }

    public void setFormLayout(PPanel formLayout) {
        this.formLayout = formLayout;
    }

    public ExportConfiguration getExportConfiguration() {
        return exportConfiguration;
    }

    public void setExportConfiguration(ExportConfiguration exportConfiguration) {
        this.exportConfiguration = exportConfiguration;
    }

    public boolean isSelectionColumnEnabled() {
        return selectionColumnEnabled;
    }

    public void setSelectionColumnEnabled(boolean isSelectionColumnEnabled) {
        this.selectionColumnEnabled = isSelectionColumnEnabled;
    }

    public boolean isShowSubListColumnEnabled() {
        return showSubListColumnEnabled;
    }

    public void setShowSubListColumnEnabled(boolean showSubListColumnEnabled) {
        this.showSubListColumnEnabled = showSubListColumnEnabled;
    }

    public void setCustomColumnEnabled(boolean customColumnEnabled, Class<T> clas) {
        this.customColumnEnabled = customColumnEnabled;
        this.clas = clas;
    }

    public boolean isCustomColumnEnabled() {
        return customColumnEnabled;
    }

    public void setClas(Class<T> clas) {
        this.clas = clas;
    }

    public Class<T> getClas() {
        return clas;
    }

	public void setShowPreferences(boolean showPreferences) {
		this.showPreferences = showPreferences;
	}

	public boolean isShowPreferences() {
		return showPreferences;
	}

}
