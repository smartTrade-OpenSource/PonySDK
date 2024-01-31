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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.service.query.Query.QueryMode;

public class QueryTest {

    private Query query;

    @Before
    public void setUp() {
        query = new Query();
    }

    /**
     * Test method for {@link com.ponysdk.core.server.service.query.Query#setQueryHint(java.lang.String)}.
     */
    @Test
    public void testQueryHint() {
        final String expected = "123";
        query.setQueryHint(expected);
        assertEquals(expected, query.getQueryHint());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.service.query.Query#setPageSize(int)}.
     */
    @Test
    public void testPageSize() {
        final int expected = 123;
        query.setPageSize(expected);
        assertEquals(expected, query.getPageSize());
    }

    /**
     * Test method for {@link com.ponysdk.core.server.service.query.Query#setPageNum(int)}.
     */
    @Test
    public void testPageNum() {
        final int expected = 321;
        query.setPageNum(expected);
        assertEquals(expected, query.getPageNum());
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.query.Query#setQueryMode(com.ponysdk.core.server.service.query.Query.QueryMode)}.
     */
    @Test
    public void testQueryMode() {
        final QueryMode expected = QueryMode.LIMIT;
        query.setQueryMode(expected);
        assertEquals(expected, query.getQueryMode());
    }

    /**
     * Test method for
     * {@link com.ponysdk.core.server.service.query.Query#addCriterion(com.ponysdk.core.server.service.query.Criterion)}.
     */
    @Test
    public void testAddCriterion() {
        final List<Criterion> expected = List.of(Mockito.mock(Criterion.class), Mockito.mock(Criterion.class));
        query.addCriteria(expected);
        final List<Criterion> criteria = query.getCriteria();
        assertEquals(2, criteria.size());
        assertEquals(expected.get(0), criteria.get(0));
        assertEquals(expected.get(1), criteria.get(1));
    }

    /**
     * Test method for {@link com.ponysdk.core.server.service.query.Query#addCriteria(java.util.List)}.
     */
    @Test
    public void testAddCriteria() {
        final String key = "pojo";
        final Criterion expected = Mockito.mock(Criterion.class);
        Mockito.when(expected.getPojoProperty()).thenReturn(key);
        query.addCriterion(expected);
        assertEquals(expected, query.getCriterion(key));
    }

}
