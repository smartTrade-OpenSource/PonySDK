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

package com.ponysdk.core.ui.list.renderer.header;

import com.ponysdk.core.server.query.Criterion;
import com.ponysdk.core.server.query.SortingType;
import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.form.formfield.FormField;
import com.ponysdk.core.ui.form.formfield.FormFieldListener;
import com.ponysdk.core.ui.form.validator.ValidationResult;
import com.ponysdk.core.ui.list.*;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.core.util.SetUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ComplexHeaderCellRenderer
        implements Queriable, HeaderCellRenderer, Resetable, HasCriteria, Sortable, Validable, FormFieldListener, HasFilterListeners {

    protected final FormField formField;
    protected final String key;
    protected final Set<FilterListener> filterListeners = SetUtils.newArraySet();
    protected PGrid panel;
    protected PLabel caption;
    protected SortingType sortingType = SortingType.NONE;

    public ComplexHeaderCellRenderer(final String caption, final FormField formField, final String key) {
        this(caption, formField, key, null);
    }

    public ComplexHeaderCellRenderer(final String caption, final FormField formField, final String key,
                                     final FilterListener filterListener) {
        this.formField = formField;
        this.key = key;
        builGUI(caption);
        addFilterListener(filterListener);
    }

    protected void builGUI(final String s) {
        buildGrid();
        buildCaption(s);

        formField.asWidget().addKeyUpHandler(new PKeyUpHandler() {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                filterListeners.forEach(FilterListener::onFilterChange);
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[]{PKeyCodes.ENTER};
            }
        });

        formField.addFormFieldListener(this);
    }

    protected void buildGrid() {
        panel = Element.newPGrid(2, 1);
        panel.addStyleName("pony-ComplexList-ComplexHeader");
        panel.setWidget(1, 0, formField.asWidget());
    }

    protected void buildCaption(final String s) {
        caption = Element.newPLabel(s);
        caption.addStyleName("sortable");
        caption.addClickHandler(event -> {
            caption.addStyleName(HeaderSortingHelper.getAssociatedStyleName(sortingType));
            final SortingType nextSortingType = HeaderSortingHelper.getNextSortingType(sortingType);
            sort(nextSortingType);
            caption.addStyleName(HeaderSortingHelper.getAssociatedStyleName(nextSortingType));

            for (final FilterListener filterListener : filterListeners) {
                filterListener.onSort(ComplexHeaderCellRenderer.this);
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
        final HasPValue<?> hasPValue = formField.asHasPValue();

        if (hasPValue != null && hasPValue.getValue() != null) {
            final Criterion criterion = new Criterion(key);
            criterion.setValue(hasPValue.getValue());
            criterion.setSortingType(sortingType);
            return Collections.singletonList(criterion);
        }

        if (sortingType != SortingType.NONE) {
            final Criterion criterion = new Criterion(key);
            criterion.setSortingType(sortingType);
            return Collections.singletonList(criterion);
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
    public void afterReset(final FormField formField) {
        formField.asWidget().removeStyleName("validation-error");
    }

    @Override
    public void afterValidation(final FormField formField, final ValidationResult validationResult) {
        if (!validationResult.isValid() && !formField.asWidget().hasStyleName("validation-error"))
            formField.asWidget().addStyleName("validation-error");
        else if (validationResult.isValid() && formField.asWidget().hasStyleName("validation-error"))
            formField.asWidget().removeStyleName("validation-error");
    }

    @Override
    public void addFilterListener(final FilterListener listener) {
        if (listener == null) return;
        filterListeners.add(listener);
    }

    @Override
    public Collection<FilterListener> getFilterListener() {
        return filterListeners;
    }

}
