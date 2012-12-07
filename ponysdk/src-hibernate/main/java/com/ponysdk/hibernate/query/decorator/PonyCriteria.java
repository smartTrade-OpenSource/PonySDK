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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Query.QueryMode;
import com.ponysdk.core.query.Result;
import com.ponysdk.hibernate.query.OrderingCriteria;
import com.ponysdk.hibernate.query.PaginatingCriteria;

public class PonyCriteria<T> {

    private static final Logger log = LoggerFactory.getLogger(PonyCriteria.class);

    private final Class<T> clazz;

    private final Session session;

    private final Query query;

    private final List<Criterion> criterions = new ArrayList<Criterion>();
    private final Map<String, CriteriaDecorator> decoratorsByPojoPropertyName = new HashMap<String, CriteriaDecorator>();

    public PonyCriteria(final Session session, final Class<T> clazz, final Query query) {
        this(session, clazz, query, null);
    }

    public void addCriteriaDecorator(final String pojoPropertyName, final CriteriaDecorator criteriaDecorator) {
        decoratorsByPojoPropertyName.put(pojoPropertyName, criteriaDecorator);
    }

    public PonyCriteria(final Session session, final Class<T> clazz, final Query query, final String propertyKey) {
        this.session = session;
        this.query = query;
        this.clazz = clazz;
    }

    public Result<List<T>> listAndCommit() {
        session.beginTransaction();
        try {
            final Result<List<T>> result = list();
            session.getTransaction().commit();
            return result;
        } catch (final Exception e) {
            final String message = "Cannot find " + clazz.getSimpleName();
            log.error(message, e);
            session.getTransaction().rollback();
            throw new RuntimeException(message, e);
        }
    }

    public void add(final Criterion criterion) {
        criterions.add(criterion);
    }

    @SuppressWarnings("unchecked")
    public Result<List<T>> list() {

        final DefaultQueryGenerator<T> queryGenerator = new DefaultQueryGenerator<T>(new PaginatingCriteria<T>(session, clazz));

        if (!decoratorsByPojoPropertyName.isEmpty()) {
            for (final Entry<String, CriteriaDecorator> entry : decoratorsByPojoPropertyName.entrySet()) {
                queryGenerator.putDecorator(entry.getKey(), entry.getValue());
            }
        }

        final OrderingCriteria criteria = queryGenerator.generate(query);

        for (final Criterion criterion : criterions) {
            criteria.add(criterion);
        }

        final int count = criteria.count();

        if (!QueryMode.FULL_RESULT.equals(query.getQueryMode())) {
            criteria.setMaxResults(query.getPageSize()).setFirstResult(query.getPageNum() * query.getPageSize());
        }

        final Result<List<T>> result = new Result<List<T>>();
        result.setData(criteria.list());
        result.setFullSize(count);
        return result;
    }

}
