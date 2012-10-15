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

package com.ponysdk.ui.server.list2.renderer.header;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ponysdk.core.query.Criterion;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PGrid;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.form2.formfield.FormField;
import com.ponysdk.ui.server.form2.formfield.FormFieldListener;
import com.ponysdk.ui.server.form2.validator.ValidationResult;
import com.ponysdk.ui.server.list2.FilterListener;
import com.ponysdk.ui.server.list2.HasCriteria;
import com.ponysdk.ui.server.list2.HasFilterListeners;
import com.ponysdk.ui.server.list2.Queriable;
import com.ponysdk.ui.server.list2.Resetable;
import com.ponysdk.ui.server.list2.Sortable;
import com.ponysdk.ui.server.list2.Validable;

public class ComplexHeaderCellRenderer implements Queriable, HeaderCellRenderer, Resetable, HasCriteria, Sortable, Validable, FormFieldListener, HasFilterListeners {

    private final PGrid headerGrid = new PGrid(2, 1);
    private final PLabel title;
    private final FormField<?> formField;
    private final String key;

    private SortingType sortingType = SortingType.NONE;

    private final ListenerCollection<FilterListener> filterListeners = new ListenerCollection<FilterListener>();

    public ComplexHeaderCellRenderer(final String caption, final FormField<?> formField, final String key) {
        this(caption, formField, key, null);
    }

    public ComplexHeaderCellRenderer(final String caption, final FormField<?> formField, final String key, final FilterListener filterListener) {
        this.title = new PLabel(caption);
        this.formField = formField;
        this.key = key;

        if (filterListener != null) this.filterListeners.add(filterListener);

        builGUI();
    }

    private void builGUI() {
        headerGrid.setWidget(0, 0, title);

        headerGrid.setSizeFull();
        headerGrid.setCellPadding(0);
        headerGrid.setCellSpacing(0);
        headerGrid.addStyleName(PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX);

        title.addStyleName(PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE);
        title.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                title.addStyleName(HeaderSortingHelper.getAssociatedStyleName(sortingType));
                final SortingType nextSortingType = HeaderSortingHelper.getNextSortingType(sortingType);
                sort(nextSortingType);
                title.addStyleName(HeaderSortingHelper.getAssociatedStyleName(nextSortingType));

                for (final FilterListener filterListener : filterListeners) {
                    filterListener.onSort(ComplexHeaderCellRenderer.this);
                }
            }
        });

        headerGrid.setWidget(1, 0, formField.asWidget());
        formField.asWidget().addDomHandler(new PKeyUpFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                for (final FilterListener filterListener : filterListeners) {
                    filterListener.onFilterChange();
                }
            }
        }, PKeyUpEvent.TYPE);

        formField.addFormFieldListener(this);
    }

    @Override
    public IsPWidget render() {
        return headerGrid;
    }

    @Override
    public List<Criterion> getCriteria() {

        if (formField.getValue() != null) {
            final Criterion criterion = new Criterion(key);
            criterion.setValue(formField.getValue());
            criterion.setSortingType(sortingType);
            return Arrays.asList(criterion);
        }

        if (sortingType != SortingType.NONE) {
            final Criterion criterion = new Criterion(key);
            criterion.setSortingType(sortingType);
            return Arrays.asList(criterion);
        }

        return Collections.emptyList();
    }

    @Override
    public void reset() {
        formField.reset();
    }

    @Override
    public void sort(final SortingType newSortingType) {
        title.removeStyleName(HeaderSortingHelper.getAssociatedStyleName(sortingType));
        sortingType = newSortingType;
        title.addStyleName(HeaderSortingHelper.getAssociatedStyleName(newSortingType));
    }

    @Override
    public Sortable asSortable() {
        return this;
    }

    @Override
    public HasCriteria asHasCriteria() {
        return this;
    }

    @Override
    public Resetable asResetable() {
        return this;
    }

    @Override
    public Validable asValidable() {
        return this;
    }

    @Override
    public ValidationResult isValid() {
        return formField.isValid();
    }

    @Override
    public void afterReset(final FormField<?> formField) {
        formField.asWidget().removeStyleName("validation-error");
    }

    @Override
    public void afterValidation(final FormField<?> formField, final ValidationResult validationResult) {
        if (!validationResult.isValid() && !formField.asWidget().hasStyleName("validation-error")) formField.asWidget().addStyleName("validation-error");
        else if (validationResult.isValid() && formField.asWidget().hasStyleName("validation-error")) formField.asWidget().removeStyleName("validation-error");
    }

    @Override
    public void addFilterListener(final FilterListener listener) {
        filterListeners.register(listener);
    }

    @Override
    public Collection<FilterListener> getFilterListener() {
        return filterListeners;
    }

}
