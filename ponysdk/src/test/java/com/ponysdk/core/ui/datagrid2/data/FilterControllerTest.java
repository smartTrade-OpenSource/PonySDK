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
package com.ponysdk.core.ui.datagrid2.data;

import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FilterControllerTest {

    private FilterController<String> controller;

    @BeforeEach
    public void setUp() {
        controller = new FilterController<>();
    }

    @Test
    public void addFilterReturnsReplacedFilter() {
        final AbstractFilter<String> first = filter(null);
        final AbstractFilter<String> second = filter(null);

        Assertions.assertNull(controller.addFilter("f", first));
        Assertions.assertSame(first, controller.addFilter("f", second));
        Assertions.assertEquals(1, controller.getFilterGroups().size());
    }

    @Test
    public void ungroupedFiltersFormOneGroupEach() {
        final AbstractFilter<String> a = filter(null);
        final AbstractFilter<String> b = filter(null);
        controller.addFilter("a", a);
        controller.addFilter("b", b);

        final Set<Set<AbstractFilter<String>>> groups = asSets(controller.getFilterGroups());

        Assertions.assertEquals(Set.of(Set.of(a), Set.of(b)), groups);
        Assertions.assertEquals("a", controller.getGroupName("a"));
    }

    @Test
    public void filtersRegisteredInSameGroupAreGroupedTogether() {
        final AbstractFilter<String> a = filter(null);
        final AbstractFilter<String> b = filter(null);
        final AbstractFilter<String> c = filter(null);
        controller.registerFilterInGroup("g", "a");
        controller.registerFilterInGroup("g", "b");
        controller.addFilter("a", a);
        controller.addFilter("b", b);
        controller.addFilter("c", c);

        Assertions.assertEquals("g", controller.getGroupName("a"));
        Assertions.assertEquals(Set.of(Set.of(a, b), Set.of(c)), asSets(controller.getFilterGroups()));
    }

    @Test
    public void clearFilterRemovesOnlyKnownKey() {
        controller.addFilter("a", filter(null));

        Assertions.assertFalse(controller.clearFilter("unknown"));
        Assertions.assertTrue(controller.clearFilter("a"));
        Assertions.assertTrue(controller.getFilterGroups().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void clearFiltersOfColumnKeepsOtherFilters() {
        final ColumnDefinition<String> column = Mockito.mock(ColumnDefinition.class);
        final ColumnDefinition<String> other = Mockito.mock(ColumnDefinition.class);
        final AbstractFilter<String> onColumn = filter(column);
        final AbstractFilter<String> onOther = filter(other);
        final AbstractFilter<String> general = filter(null);
        controller.addFilter("a", onColumn);
        controller.addFilter("b", onOther);
        controller.addFilter("c", general);

        controller.clearFilters(column);

        Assertions.assertEquals(Set.of(Set.of(onOther), Set.of(general)), asSets(controller.getFilterGroups()));
    }

    @Test
    public void clearFiltersRemovesAll() {
        controller.addFilter("a", filter(null));
        controller.addFilter("b", filter(null));

        controller.clearFilters();

        Assertions.assertTrue(controller.getFilterGroups().isEmpty());
    }

    private static Set<Set<AbstractFilter<String>>> asSets(final Collection<Collection<AbstractFilter<String>>> groups) {
        final Set<Set<AbstractFilter<String>>> result = new HashSet<>();
        for (final Collection<AbstractFilter<String>> group : groups) {
            result.add(new HashSet<>(group));
        }
        return result;
    }

    private static AbstractFilter<String> filter(final ColumnDefinition<String> column) {
        return new AbstractFilter<>() {

            @Override
            public boolean test(final DefaultRow<String> row) {
                return true;
            }

            @Override
            public ColumnDefinition<String> getColumnDefinition() {
                return column;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        };
    }
}
