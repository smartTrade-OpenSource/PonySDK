/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.server.service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PResultSetTest {

    /** Serves a fixed sequence of pages, one per call to next(). */
    private static final class PagedResultSet implements PResultSet<Integer> {

        private final List<List<Integer>> pages;
        private int index = -1;
        private boolean closed;

        PagedResultSet(final List<List<Integer>> pages) {
            this.pages = pages;
        }

        @Override
        public boolean next() {
            if (index + 1 >= pages.size()) return false;
            index++;
            return true;
        }

        @Override
        public List<Integer> getData() {
            return pages.get(index);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    public void listResultSetServesItsDataOnceThenStops() throws Exception {
        final List<String> data = List.of("a", "b");
        final PResultSet<String> resultSet = PResultSet.of(data);

        assertTrue(resultSet.next());
        assertEquals(data, resultSet.getData());
        assertFalse(resultSet.next());
        assertFalse(resultSet.next());
        resultSet.close();
        assertEquals(data, resultSet.getData());
    }

    @Test
    public void withLimitTruncatesThePageThatCrossesTheLimit() {
        final PagedResultSet delegate = new PagedResultSet(List.of(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7, 8, 9)));
        final PResultSet<Integer> limited = delegate.withLimit(5);

        assertTrue(limited.next());
        assertEquals(List.of(1, 2, 3), limited.getData());
        assertTrue(limited.next());
        assertEquals(List.of(4, 5), limited.getData());
        assertFalse(limited.next());
    }

    @Test
    public void withLimitOnAPageBoundaryKeepsTheWholePageAndStops() {
        final PagedResultSet delegate = new PagedResultSet(List.of(List.of(1, 2, 3), List.of(4, 5, 6)));
        final PResultSet<Integer> limited = delegate.withLimit(3);

        assertTrue(limited.next());
        assertEquals(List.of(1, 2, 3), limited.getData());
        assertFalse(limited.next());
    }

    @Test
    public void withLimitStopsWhenTheDelegateRunsOutFirst() {
        final PagedResultSet delegate = new PagedResultSet(List.of(List.of(1, 2)));
        final PResultSet<Integer> limited = delegate.withLimit(10);

        assertTrue(limited.next());
        assertEquals(List.of(1, 2), limited.getData());
        assertFalse(limited.next());
    }

    @Test
    public void withLimitClosesTheDelegate() throws Exception {
        final PagedResultSet delegate = new PagedResultSet(List.of());
        delegate.withLimit(1).close();
        assertTrue(delegate.closed);
    }

}
