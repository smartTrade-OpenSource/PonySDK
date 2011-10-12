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
package com.ponysdk.impl.query.hibernate.decorator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;

import com.ponysdk.core.query.CriterionField;
import com.ponysdk.core.query.SortingType;
import com.ponysdk.impl.query.hibernate.CriteriaContext;

public class DefaultSortCriteriaDecorator implements CriteriaDecorator {

    @Override
    public void render(CriteriaContext context) {
        final CriterionField field = context.getCriterion();

        if (field.getSortingType() == SortingType.NONE)
            return;

        Criteria criteria = context.getSTCriteria();

        final List<String> propertyNamePath = Arrays.asList(field.getPojoProperty().split(REGEX_SPLIT));
        final Iterator<String> iter = propertyNamePath.iterator();
        String key = null;
        String associationPath = null;
        if (propertyNamePath.size() == 1) {
            associationPath = iter.next();
        } else
            while (iter.hasNext()) {
                key = iter.next();
                if (associationPath == null) {
                    associationPath = new String(key);
                } else {
                    associationPath += "." + key;
                }
                if (iter.hasNext()) {
                    criteria = criteria.createCriteria(associationPath, key, CriteriaSpecification.LEFT_JOIN);
                    associationPath = new String(key);
                }
            }
        criteria = context.getSTCriteria();
        if (field.getSortingType() == SortingType.ASCENDING) {
            criteria.addOrder(Order.asc(associationPath));
        } else if (field.getSortingType() == SortingType.DESCENDING) {
            criteria.addOrder(Order.desc(associationPath));
        }
    }
}
