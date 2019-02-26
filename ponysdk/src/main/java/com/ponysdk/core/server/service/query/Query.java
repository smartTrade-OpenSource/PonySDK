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

package com.ponysdk.core.server.service.query;

import java.util.ArrayList;
import java.util.List;

public class Query {

    private final List<Criterion> criteria = new ArrayList<>();
    private int pageSize = Integer.MAX_VALUE;
    private String queryHint;
    private int pageNum;
    private QueryMode queryMode = QueryMode.PAGINATION;

    public enum QueryMode {
        PAGINATION,
        LIMIT,
        FULL_RESULT
    }

    public void setQueryHint(final String queryHint) {
        this.queryHint = queryHint;
    }

    public String getQueryHint() {
        return queryHint;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(final int pageNum) {
        this.pageNum = pageNum;
    }

    public QueryMode getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(final QueryMode queryMode) {
        this.queryMode = queryMode;
    }

    public void addCriterion(final Criterion criterion) {
        this.criteria.add(criterion);
    }

    public void addCriteria(final List<Criterion> criteria) {
        this.criteria.addAll(criteria);
    }

    public List<Criterion> getCriteria() {
        return this.criteria;
    }

    public Criterion getCriterion(final String pojoProperty) {
        for (final Criterion criterion : criteria) {
            if (criterion.getPojoProperty().equals(pojoProperty)) return criterion;
        }
        return null;
    }

    /**
     * @deprecated Use {@link #addCriteria(List)} instead
     * @since v2.8.10
     */
    @Deprecated(forRemoval = true, since = "v2.8.10")
    public void setCriteria(final List<Criterion> criteria) {
        addCriteria(criteria);
    }

}
