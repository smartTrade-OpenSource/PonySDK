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

package com.ponysdk.core.query;

import java.util.ArrayList;
import java.util.List;

public class Query {

    public enum QueryMode {
        PAGINATION, FULL_RESULT;
    }

    private int pageSize = Integer.MAX_VALUE;

    private int pageNum = 0;

    private QueryMode queryMode = QueryMode.PAGINATION; // TODO nciaravola avoid to breaking existing queries

    private final List<CriterionField> criteria = new ArrayList<CriterionField>();

    // private final Map<String, CriterionField> criteria = new HashMap<String, CriterionField>();

    public Query() {
        super();
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void addCriterion(CriterionField criteriField) {
        criteria.add(criteriField);
    }

    public List<CriterionField> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<CriterionField> criteria) {
        for (CriterionField criterionField : criteria) {
            addCriterion(criterionField);
        }
    }

    public CriterionField getCriterion(String pojoProperty) {
        // temp return a list
        for (CriterionField criterion : criteria) {
            if (criterion.getPojoProperty().equals(pojoProperty)) return criterion;
        }
        return null;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public QueryMode getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(QueryMode queryMode) {
        this.queryMode = queryMode;
    }
}
