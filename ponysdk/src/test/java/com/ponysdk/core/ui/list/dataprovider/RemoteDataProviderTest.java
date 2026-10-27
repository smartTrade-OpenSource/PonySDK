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

package com.ponysdk.core.ui.list.dataprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.server.service.query.Criterion;
import com.ponysdk.core.server.service.query.Query;
import com.ponysdk.core.server.service.query.Query.QueryMode;
import com.ponysdk.core.server.service.query.SortingType;
import com.ponysdk.core.ui.form.validator.ValidationResult;
import com.ponysdk.core.ui.list.HasCriteria;
import com.ponysdk.core.ui.list.Queriable;
import com.ponysdk.core.ui.list.Resetable;
import com.ponysdk.core.ui.list.Sortable;
import com.ponysdk.core.ui.list.Validable;
import com.ponysdk.core.ui.list.paging.DefaultPagerView;
import com.ponysdk.core.ui.list.paging.Pager;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;

public class RemoteDataProviderTest extends PSuite {

    private static class TestProvider extends RemoteDataProvider<String> {

        private final List<Query> queries = new ArrayList<>();

        TestProvider(final Pager<String> pager, final List<List<String>> sink) {
            super(pager, sink::add);
        }

        @Override
        protected List<String> getData(final Query query) {
            queries.add(query);
            return List.of("page" + query.getPageNum());
        }

        @Override
        protected List<String> getFullData(final Query query) {
            queries.add(query);
            return List.of("full");
        }
    }

    private static class TestQueriable implements Queriable, Sortable, HasCriteria, Resetable, Validable {

        private final String key;
        private SortingType sortingType = SortingType.ASCENDING;
        private boolean valid = true;
        private int resets;

        TestQueriable(final String key) {
            this.key = key;
        }

        @Override
        public void sort(final SortingType sortingType) {
            this.sortingType = sortingType;
        }

        @Override
        public List<Criterion> getCriteria() {
            return List.of(new Criterion(key).setSortingType(sortingType));
        }

        @Override
        public void reset() {
            resets++;
        }

        @Override
        public ValidationResult isValid() {
            return valid ? ValidationResult.newOKValidationResult() : ValidationResult.newFailedValidationResult("ko");
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
    }

    @Test
    public void testQueriesAndPaging() {
        final DefaultPagerView pagerView = new DefaultPagerView();
        final Pager<String> pager = new Pager<>(pagerView, 10);
        final List<List<String>> sink = new ArrayList<>();
        final TestProvider provider = new TestProvider(pager, sink);
        final TestQueriable name = new TestQueriable("name");
        provider.registerHasCriteria(name);

        assertEquals(List.of("page0"), provider.getData());
        final Query query = provider.queries.get(0);
        assertEquals(0, query.getPageNum());
        assertEquals(10, query.getPageSize());
        assertEquals("name", query.getCriteria().get(0).getPojoProperty());

        assertEquals(List.of("full"), provider.getFullData());
        assertEquals(QueryMode.FULL_RESULT, provider.queries.get(1).getQueryMode());

        // The provider listens to the pager
        pager.process(100);
        PEmulator.click(pagerView.getWidget(3));
        assertEquals(1, pager.getCurrentPage());
        assertEquals(List.of(List.of("page1")), sink);

        provider.onPageChange(4);
        assertEquals(4, pager.getCurrentPage());
        assertEquals(List.of("page4"), sink.get(1));
    }

    @Test
    public void testFilterSortAndReset() {
        final Pager<String> pager = new Pager<>(new DefaultPagerView(), 10);
        final List<List<String>> sink = new ArrayList<>();
        final TestProvider provider = new TestProvider(pager, sink);
        final TestQueriable name = new TestQueriable("name");
        final TestQueriable age = new TestQueriable("age");
        provider.registerHasCriteria(name);
        provider.registerHasCriteria(age);
        assertEquals(2, provider.buildQuery().getCriteria().size());

        pager.setCurrentPage(3);
        provider.onFilterChange();
        assertEquals(0, pager.getCurrentPage());
        assertEquals(List.of(List.of("page0")), sink);

        age.valid = false;
        assertFalse(provider.isValid());
        provider.onFilterChange();
        assertEquals(1, sink.size());
        age.valid = true;
        assertTrue(provider.isValid());

        provider.onSort(name);
        assertEquals(SortingType.ASCENDING, name.sortingType);
        assertEquals(SortingType.NONE, age.sortingType);
        assertEquals(2, sink.size());

        provider.reset();
        assertEquals(1, name.resets);
        assertEquals(1, age.resets);
    }

    @Test
    public void testQueriableWithoutCapabilities() {
        final TestProvider provider = new TestProvider(new Pager<>(new DefaultPagerView()), new ArrayList<>());
        provider.registerHasCriteria(new Queriable() {

            @Override
            public Sortable asSortable() {
                return null;
            }

            @Override
            public HasCriteria asHasCriteria() {
                return null;
            }

            @Override
            public Resetable asResetable() {
                return null;
            }

            @Override
            public Validable asValidable() {
                return null;
            }
        });
        assertTrue(provider.isValid());
        assertTrue(provider.buildQuery().getCriteria().isEmpty());
        provider.reset();
        assertNull(provider.buildQuery().getCriterion("name"));
    }
}
