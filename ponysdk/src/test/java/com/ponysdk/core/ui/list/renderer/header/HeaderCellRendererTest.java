/*
 * Copyright (c) 2026 PonySDK
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.server.service.query.Criterion;
import com.ponysdk.core.server.service.query.SortingType;
import com.ponysdk.core.ui.basic.PGrid;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.form.formfield.StringTextBoxFormField;
import com.ponysdk.core.ui.form.validator.NotEmptyFieldValidator;
import com.ponysdk.core.ui.list.FilterListener;
import com.ponysdk.core.ui.list.Sortable;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class HeaderCellRendererTest extends PSuite {

    private static class RecordingFilterListener implements FilterListener {

        private int filterChanges;
        private final List<Sortable> sorts = new ArrayList<>();

        @Override
        public void onFilterChange() {
            filterChanges++;
        }

        @Override
        public void onSort(final Sortable sortingCriteria) {
            sorts.add(sortingCriteria);
        }
    }

    @Test
    public void testHeaderSortingHelper() {
        assertEquals(SortingType.ASCENDING, HeaderSortingHelper.getNextSortingType(SortingType.NONE));
        assertEquals(SortingType.DESCENDING, HeaderSortingHelper.getNextSortingType(SortingType.ASCENDING));
        assertEquals(SortingType.ASCENDING, HeaderSortingHelper.getNextSortingType(SortingType.DESCENDING));
        assertEquals("ascending", HeaderSortingHelper.getAssociatedStyleName(SortingType.ASCENDING));
        assertEquals("descending", HeaderSortingHelper.getAssociatedStyleName(SortingType.DESCENDING));
        assertEquals("none", HeaderSortingHelper.getAssociatedStyleName(SortingType.NONE));
        assertEquals(SortingType.NONE, HeaderSortingHelper.getNextSortingType(null));
        assertThrows(NullPointerException.class, () -> HeaderSortingHelper.getAssociatedStyleName(null));
        assertNotNull(new HeaderSortingHelper(), "public, implicitly constructible");
    }

    @Test
    public void testStringHeaderCellRenderer() {
        final StringHeaderCellRenderer renderer = new StringHeaderCellRenderer("Caption");
        assertEquals("Caption", ((PLabel) renderer.render()).getText());
        assertSame(renderer.render(), renderer.render());
    }

    @Test
    public void testSortableHeaderCellRenderer() {
        final RecordingFilterListener listener = new RecordingFilterListener();
        final SortableHeaderCellRenderer renderer = new SortableHeaderCellRenderer("Name", "name", listener);
        final PLabel title = (PLabel) renderer.render();
        assertEquals("Name", title.getText());
        assertTrue(title.hasStyleName("sortable"));
        assertEquals("name", renderer.getKey());
        assertEquals(SortingType.NONE, renderer.getSortingType());
        assertSame(renderer, renderer.asSortable());
        assertSame(renderer, renderer.asHasCriteria());
        assertNull(renderer.asResetable());
        assertNull(renderer.asValidable());

        PEmulator.click(title);
        assertEquals(SortingType.ASCENDING, renderer.getSortingType());
        assertTrue(title.hasStyleName("ascending"));
        assertEquals(List.of(renderer), listener.sorts);

        PEmulator.click(title);
        assertEquals(SortingType.DESCENDING, renderer.getSortingType());
        assertTrue(title.hasStyleName("descending"));
        assertFalse(title.hasStyleName("ascending"));

        final List<Criterion> criteria = renderer.getCriteria();
        assertEquals(1, criteria.size());
        assertEquals("name", criteria.get(0).getPojoProperty());
        assertEquals(SortingType.DESCENDING, criteria.get(0).getSortingType());

        // Going back to NONE does not notify the listener
        renderer.sort(SortingType.NONE);
        assertEquals(2, listener.sorts.size());

        final RecordingFilterListener other = new RecordingFilterListener();
        renderer.setFilterListener(other);
        renderer.sort(SortingType.ASCENDING);
        assertEquals(1, other.sorts.size());
        assertEquals(2, listener.sorts.size());

        final SortableHeaderCellRenderer withoutListener = new SortableHeaderCellRenderer("Age", "age");
        withoutListener.sort(SortingType.ASCENDING);
        assertEquals(SortingType.ASCENDING, withoutListener.getSortingType());
    }

    @Test
    public void testComplexHeaderCellRenderer() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        final RecordingFilterListener listener = new RecordingFilterListener();
        final ComplexHeaderCellRenderer renderer = new ComplexHeaderCellRenderer("Name", formField, "name", listener);
        assertSame(renderer, renderer.asSortable());
        assertSame(renderer, renderer.asHasCriteria());
        assertSame(renderer, renderer.asResetable());
        assertSame(renderer, renderer.asValidable());
        assertEquals(1, renderer.getFilterListener().size());
        renderer.addFilterListener(null);
        assertEquals(1, renderer.getFilterListener().size());

        final PGrid grid = (PGrid) renderer.render();
        assertSame(formField.asWidget(), grid.getWidget(1, 0));
        final PLabel caption = (PLabel) grid.getWidget(0, 0);
        assertEquals("Name", caption.getText());

        assertTrue(renderer.getCriteria().isEmpty());

        PEmulator.click(caption);
        assertTrue(caption.hasStyleName("ascending"));
        assertEquals(List.of(renderer), listener.sorts);
        List<Criterion> criteria = renderer.getCriteria();
        assertEquals(1, criteria.size());
        assertEquals(SortingType.ASCENDING, criteria.get(0).getSortingType());
        assertNull(criteria.get(0).getValue());

        formField.setValue("john");
        criteria = renderer.getCriteria();
        assertEquals("john", criteria.get(0).getValue());
        assertEquals(SortingType.ASCENDING, criteria.get(0).getSortingType());

        formField.asWidget().onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.KEY_UP.getValue())
            .add(ClientToServerModel.VALUE_KEY.toStringValue(), 13).build());
        assertEquals(1, listener.filterChanges);

        renderer.reset();
        assertNull(formField.getValue());
    }

    @Test
    public void testComplexHeaderWithoutValueHolderFiltersOnSortOnly() {
        final StringTextBoxFormField formField = new StringTextBoxFormField() {

            @Override
            public com.ponysdk.core.ui.basic.HasPValue<String> asHasPValue() {
                return null;
            }
        };
        formField.setValue("ignored");
        final ComplexHeaderCellRenderer renderer = new ComplexHeaderCellRenderer("Name", formField, "name");
        assertTrue(renderer.getCriteria().isEmpty());
        renderer.sort(SortingType.DESCENDING);
        final List<Criterion> criteria = renderer.getCriteria();
        assertEquals(1, criteria.size());
        assertNull(criteria.get(0).getValue());
        assertEquals(SortingType.DESCENDING, criteria.get(0).getSortingType());
    }

    @Test
    public void testComplexHeaderValidationStyle() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        formField.setValidator(new NotEmptyFieldValidator());
        final ComplexHeaderCellRenderer renderer = new ComplexHeaderCellRenderer("Name", formField, "name");
        assertTrue(renderer.getFilterListener().isEmpty());

        assertFalse(renderer.isValid().isValid());
        assertTrue(formField.asWidget().hasStyleName("validation-error"));
        // Still invalid: the style is kept once
        renderer.isValid();
        assertTrue(formField.asWidget().hasStyleName("validation-error"));

        formField.setValue("value");
        assertTrue(renderer.isValid().isValid());
        assertFalse(formField.asWidget().hasStyleName("validation-error"));
        // Still valid: nothing to remove
        assertTrue(renderer.isValid().isValid());
        assertFalse(formField.asWidget().hasStyleName("validation-error"));

        formField.setValue("");
        renderer.isValid();
        assertTrue(formField.asWidget().hasStyleName("validation-error"));
        renderer.reset();
        assertFalse(formField.asWidget().hasStyleName("validation-error"));
    }

    @Test
    public void testFilterableHeaderCellRenderer() {
        final StringTextBoxFormField formField = new StringTextBoxFormField();
        final FilterableHeaderCellRenderer renderer = new FilterableHeaderCellRenderer("Name", formField, "name");
        assertNull(renderer.asSortable());
        final PGrid grid = (PGrid) renderer.render();
        final PLabel caption = (PLabel) grid.getWidget(0, 0);
        assertEquals("Name", caption.getText());
        assertFalse(caption.hasStyleName("sortable"));
        formField.setValue("x");
        assertEquals("x", renderer.getCriteria().get(0).getValue());
    }
}
