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
package com.ponysdk.hibernate.query.decorator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ponysdk.core.query.ComparatorType;
import com.ponysdk.core.query.CriterionField;
import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Query.QueryMode;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.hibernate.query.CriteriaContext;
import com.ponysdk.hibernate.query.OrderingCriteria;
import com.ponysdk.hibernate.query.QueryGenerator;

public class DefaultQueryGenerator implements QueryGenerator {

    private final Map<String, CriteriaDecorator> criteriaDecoratorByPojoPropertyKey = new HashMap<String, CriteriaDecorator>();
    private CriteriaDecorator sortCriteriaDecorator;
    private final String defaultSortingProperty;
    private final OrderingCriteria criteria;
    private SortingType sortingType = SortingType.NONE;

    public DefaultQueryGenerator(OrderingCriteria criteria) {
        this(criteria, null, SortingType.NONE);
    }

    public DefaultQueryGenerator(OrderingCriteria criteria, String defaultSortingProperty, SortingType sortingType) {
        this.defaultSortingProperty = defaultSortingProperty;
        this.sortingType = sortingType;

        sortCriteriaDecorator = new DefaultSortCriteriaDecorator();

        this.criteria = criteria;
    }

    public void putDecorator(String pojoPropertyKey, CriteriaDecorator criteriaDecorator) {
        criteriaDecoratorByPojoPropertyKey.put(pojoPropertyKey, criteriaDecorator);
    }

    public void setSortCriteriaDecorator(CriteriaDecorator sortCriteriaDecorator) {
        this.sortCriteriaDecorator = sortCriteriaDecorator;
    }

    @Override
    public OrderingCriteria generate(Query query) {
        String ordering = null;
        final List<CriterionField> fields = query.getCriteria();
        if (fields != null) {
            for (final CriterionField criterion : fields) {
                final CriteriaContext context = new CriteriaContext();
                context.setCriterion(criterion);
                context.setSTCriteria(criteria);

                if (criterion.getValue() != null || criterion.getComparator() == ComparatorType.IS_NULL || criterion.getComparator() == ComparatorType.IS_NOT_NULL) {
                    CriteriaDecorator criteriaDecorator = criteriaDecoratorByPojoPropertyKey.get(criterion.getPojoProperty());
                    if (criteriaDecorator == null) {
                        criteriaDecorator = new DefaultCriteriaDecorator();
                    }
                    criteriaDecorator.render(context);
                }
                if (criterion.getSortingType() != SortingType.NONE) {
                    sortCriteriaDecorator.render(context);
                    ordering = criterion.getPojoProperty();
                }
            }
        }

        // sort forced on the default property
        if (ordering == null || !ordering.equals(defaultSortingProperty)) {
            if (defaultSortingProperty != null) {
                final CriterionField field = new CriterionField(defaultSortingProperty);
                field.setSortingType(sortingType);
                final CriteriaContext context = new CriteriaContext();
                context.setCriterion(field);
                context.setSTCriteria(criteria);
                sortCriteriaDecorator.render(context);
            }
        }

        // handle scrolling when too many data
        if (!QueryMode.FULL_RESULT.equals(query.getQueryMode())) {
            criteria.setMaxResults(query.getPageSize()).setFirstResult(query.getPageNum() * query.getPageSize());
        }
        return criteria;
    }
}
