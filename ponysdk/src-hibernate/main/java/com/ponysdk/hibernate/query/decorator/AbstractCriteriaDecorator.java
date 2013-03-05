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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.query.ComparatorType;
import com.ponysdk.core.query.Criterion;
import com.ponysdk.hibernate.query.CriteriaContext;

public abstract class AbstractCriteriaDecorator<T> implements CriteriaDecorator {

    private static Logger log = LoggerFactory.getLogger(AbstractCriteriaDecorator.class);

    @SuppressWarnings("rawtypes")
    @Override
    public void render(final CriteriaContext context) {
        final Criterion field = context.getCriterion();
        Criteria criteria = context.getOrderingCriteria();

        final List<String> propertyNamePath = Arrays.asList(field.getPojoProperty().split(REGEX_SPLIT));
        final Iterator<String> iter = propertyNamePath.iterator();
        String key = null;
        String associationPath = null;
        if (propertyNamePath.size() == 1) {
            associationPath = iter.next();
        } else while (iter.hasNext()) {
            key = iter.next();
            if (associationPath == null) {
                associationPath = new String(key);
            } else {
                associationPath += "." + key;
            }
            if (iter.hasNext()) {
                criteria = criteria.createCriteria(associationPath, key, CriteriaSpecification.INNER_JOIN);
                associationPath = new String(key);
            }
        }

        final T value = getObjectValue(field);
        ComparatorType comparator = field.getComparator();

        if (value != null) {
            if (value.toString().contains("%")) {
                comparator = ComparatorType.LIKE;
            }
        }

        if (field.getValue() != null || field.getComparator() == ComparatorType.IS_NULL || field.getComparator() == ComparatorType.IS_NOT_NULL) {

            switch (comparator) {
                case EQ:
                    criteria.add(Restrictions.eq(associationPath, value));
                    break;
                case GE:
                    criteria.add(Restrictions.ge(associationPath, value));
                    break;
                case GT:
                    criteria.add(Restrictions.gt(associationPath, value));
                    break;
                case LE:
                    criteria.add(Restrictions.le(associationPath, value));
                    break;
                case LT:
                    criteria.add(Restrictions.lt(associationPath, value));
                    break;
                case NE:
                    criteria.add(Restrictions.ne(associationPath, value));
                    break;
                case LIKE:
                    criteria.add(Restrictions.ilike(associationPath, value));
                    break;
                case IS_NULL:
                    criteria.add(Restrictions.isNull(associationPath));
                    break;
                case IS_NOT_NULL:
                    criteria.add(Restrictions.isNotNull(associationPath));
                    break;
                case IN:
                    if (value instanceof Collection) {
                        criteria.add(Restrictions.in(associationPath, (Collection) value));
                    } else if (value instanceof Object[]) {
                        criteria.add(Restrictions.in(associationPath, (Object[]) value));
                    } else {
                        log.warn("Type not allowed for IN clause: " + value.getClass() + ", value: " + value);
                    }
                    break;

                default:
                    log.warn("Restriction not supported: " + comparator);
                    break;
            }
        }

        switch (field.getSortingType()) {
            case ASCENDING:
                criteria.addOrder(Order.asc(associationPath));
                break;
            case DESCENDING:
                criteria.addOrder(Order.desc(associationPath));
                break;
            case NONE:
                break;
        }

    }

    protected abstract T getObjectValue(Criterion criterionField);
}
