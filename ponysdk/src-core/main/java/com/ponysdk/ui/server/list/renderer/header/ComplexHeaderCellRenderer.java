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

package com.ponysdk.ui.server.list.renderer.header;

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
import com.ponysdk.ui.server.form.formfield.FormField;
import com.ponysdk.ui.server.form.formfield.FormFieldListener;
import com.ponysdk.ui.server.form.validator.ValidationResult;
import com.ponysdk.ui.server.list.FilterListener;
import com.ponysdk.ui.server.list.HasCriteria;
import com.ponysdk.ui.server.list.HasFilterListeners;
import com.ponysdk.ui.server.list.Queriable;
import com.ponysdk.ui.server.list.Resetable;
import com.ponysdk.ui.server.list.Sortable;
import com.ponysdk.ui.server.list.Validable;

public class ComplexHeaderCellRenderer implements Queriable, HeaderCellRenderer, Resetable, HasCriteria, Sortable, Validable, FormFieldListener, HasFilterListeners {

    protected final FormField<?> formField;
    protected final String key;

    protected PGrid panel;
    protected PLabel caption;

    protected SortingType sortingType = SortingType.NONE;

    protected final ListenerCollection<FilterListener> filterListeners = new ListenerCollection<>();

    public ComplexHeaderCellRenderer(final String caption, final FormField<?> formField, final String key) {
        this(caption, formField, key, null);
    }

    public ComplexHeaderCellRenderer(final String caption, final FormField<?> formField, final String key, final FilterListener filterListener) {
        this.formField = formField;
        this.key = key;
        builGUI(caption);
        addFilterListener(filterListener);
    }

    protected void builGUI(final String s) {
        buildGrid();
        buildCaption(s);

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

    protected void buildGrid() {
        panel = new PGrid(2, 1);
        panel.addStyleName(PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX);
        panel.setWidget(1, 0, formField.asWidget());
    }

    protected void buildCaption(final String s) {
        caption = new PLabel(s);
        caption.addStyleName(PonySDKTheme.COMPLEXLIST_HEADERCELLRENDERER_COMPLEX_SORTABLE);
        caption.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                caption.addStyleName(HeaderSortingHelper.getAssociatedStyleName(sortingType));
                final SortingType nextSortingType = HeaderSortingHelper.getNextSortingType(sortingType);
                sort(nextSortingType);
                caption.addStyleName(HeaderSortingHelper.getAssociatedStyleName(nextSortingType));

                for (final FilterListener filterListener : filterListeners) {
                    filterListener.onSort(ComplexHeaderCellRenderer.this);
                }
            }
        });
        panel.setWidget(0, 0, caption);
    }

    @Override
    public IsPWidget render() {
        return panel;
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
        caption.removeStyleName(HeaderSortingHelper.getAssociatedStyleName(sortingType));
        sortingType = newSortingType;
        caption.addStyleName(HeaderSortingHelper.getAssociatedStyleName(newSortingType));
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
        if (listener == null) return;
        filterListeners.register(listener);
    }

    @Override
    public Collection<FilterListener> getFilterListener() {
        return filterListeners;
    }

}
