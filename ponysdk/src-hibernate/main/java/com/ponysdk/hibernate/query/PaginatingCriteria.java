/*
 * Copyright (c) 2011 PonySDK
 *  Ownersimport java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.java.swing.plaf.nimbus.AbstractRegionPainter.PaintContext.CacheMode;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Session;
Licensed under the Apache License, Version 2.0 (the "License"); you may not
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

package com.ponysdk.hibernate.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.transform.ResultTransformer;

public class PaginatingCriteria<T> implements OrderingCriteria {

    private final Criteria mainCriteria;

    private final Criteria cloneCriteria;

    private Map<String, OrderingCriteria> subCriteriaByAlias = new HashMap<String, OrderingCriteria>();

    public PaginatingCriteria(final Session session, final Class<T> persistentClass) {
        this.mainCriteria = session.createCriteria(persistentClass);
        this.mainCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        this.cloneCriteria = session.createCriteria(persistentClass);
        this.cloneCriteria.setProjection(Projections.rowCount());
        this.cloneCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
    }

    private PaginatingCriteria(final Criteria mainCriteria, final Criteria clonedCriteria, final Map<String, OrderingCriteria> subCriteriaByAlias) {
        this.mainCriteria = mainCriteria;
        this.cloneCriteria = clonedCriteria;
        this.subCriteriaByAlias = subCriteriaByAlias;
    }

    @Override
    public String getAlias() {
        return mainCriteria.getAlias();
    }

    @Override
    public OrderingCriteria setProjection(final Projection projection) {
        mainCriteria.setProjection(projection);
        return this;
    }

    @Override
    public OrderingCriteria add(final Criterion criterion) {
        cloneCriteria.add(criterion);
        mainCriteria.add(criterion);
        return this;
    }

    @Override
    public OrderingCriteria addOrder(final Order order) {
        mainCriteria.addOrder(order);
        return this;
    }

    @Override
    public OrderingCriteria setFetchMode(final String associationPath, final FetchMode mode) throws HibernateException {
        cloneCriteria.setFetchMode(associationPath, mode);
        mainCriteria.setFetchMode(associationPath, mode);
        return this;
    }

    @Override
    public OrderingCriteria setLockMode(final LockMode lockMode) {
        mainCriteria.setLockMode(lockMode);
        return this;
    }

    @Override
    public OrderingCriteria setLockMode(final String alias, final LockMode lockMode) {
        mainCriteria.setLockMode(alias, lockMode);
        cloneCriteria.setLockMode(alias, lockMode);
        return this;
    }

    @Override
    public OrderingCriteria createAlias(final String associationPath, final String alias) throws HibernateException {
        final OrderingCriteria orderingCriteria = subCriteriaByAlias.get(alias);
        if (orderingCriteria != null) return orderingCriteria;
        mainCriteria.createAlias(associationPath, alias);
        cloneCriteria.createAlias(associationPath, alias);
        subCriteriaByAlias.put(alias, this);
        return this;
    }

    @Override
    public OrderingCriteria createAlias(final String arg0, final String arg1, final int arg2) throws HibernateException {
        final OrderingCriteria orderingCriteria = subCriteriaByAlias.get(arg1);
        if (orderingCriteria != null) return orderingCriteria;
        mainCriteria.createAlias(arg0, arg1, arg2);
        cloneCriteria.createAlias(arg0, arg1, arg2);
        subCriteriaByAlias.put(arg1, this);
        return this;
    }

    @Override
    public OrderingCriteria createCriteria(final String associationPath) throws HibernateException {
        return new PaginatingCriteria<T>(mainCriteria.createCriteria(associationPath), cloneCriteria.createCriteria(associationPath), subCriteriaByAlias);
    }

    @Override
    public OrderingCriteria createCriteria(final String arg0, final int arg1) throws HibernateException {
        return new PaginatingCriteria<T>(mainCriteria.createCriteria(arg0, arg1), cloneCriteria.createCriteria(arg0, arg1), subCriteriaByAlias);
    }

    @Override
    public OrderingCriteria createCriteria(final String associationPath, final String alias) throws HibernateException {
        mainCriteria.createCriteria(associationPath, alias);
        cloneCriteria.createCriteria(associationPath, alias);
        return this;
    }

    @Override
    public OrderingCriteria createCriteria(final String arg0, final String arg1, final int arg2) throws HibernateException {

        OrderingCriteria orderingCriteria = subCriteriaByAlias.get(arg1);
        if (orderingCriteria == null) {
            orderingCriteria = new PaginatingCriteria<T>(mainCriteria.createCriteria(arg0, arg1, arg2), cloneCriteria.createCriteria(arg0, arg1, arg2), subCriteriaByAlias);
            subCriteriaByAlias.put(arg1, orderingCriteria);
        }
        return orderingCriteria;
    }

    @Override
    public OrderingCriteria setResultTransformer(final ResultTransformer resultTransformer) {
        mainCriteria.setResultTransformer(resultTransformer);
        cloneCriteria.setResultTransformer(resultTransformer);
        return this;
    }

    @Override
    public OrderingCriteria setMaxResults(final int maxResults) {
        mainCriteria.setMaxResults(maxResults);
        return this;
    }

    @Override
    public OrderingCriteria setFirstResult(final int firstResult) {
        mainCriteria.setFirstResult(firstResult);
        return this;
    }

    @Override
    public OrderingCriteria setFetchSize(final int fetchSize) {
        mainCriteria.setFetchSize(fetchSize);
        return this;
    }

    @Override
    public OrderingCriteria setTimeout(final int timeout) {
        mainCriteria.setTimeout(timeout);
        return this;
    }

    @Override
    public OrderingCriteria setCacheable(final boolean cacheable) {
        mainCriteria.setCacheable(cacheable);
        return this;
    }

    @Override
    public OrderingCriteria setCacheRegion(final String cacheRegion) {
        mainCriteria.setCacheRegion(cacheRegion);
        return this;
    }

    @Override
    public OrderingCriteria setComment(final String comment) {
        cloneCriteria.setComment(comment);
        return this;
    }

    @Override
    public OrderingCriteria setFlushMode(final FlushMode flushMode) {
        mainCriteria.setFlushMode(flushMode);
        return this;
    }

    @Override
    public OrderingCriteria setCacheMode(final CacheMode cacheMode) {
        mainCriteria.setCacheMode(cacheMode);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> list() throws HibernateException {
        mainCriteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        return mainCriteria.list();
    }

    @Override
    public ScrollableResults scroll() throws HibernateException {
        return mainCriteria.scroll();
    }

    @Override
    public ScrollableResults scroll(final ScrollMode scrollMode) throws HibernateException {
        return mainCriteria.scroll(scrollMode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T uniqueResult() throws HibernateException {
        mainCriteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        return (T) mainCriteria.uniqueResult();
    }

    @Override
    public int count() throws HibernateException {
        cloneCriteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        final Long count = (Long) cloneCriteria.uniqueResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Criteria createAlias(final String arg0, final String arg1, final int arg2, final Criterion arg3) throws HibernateException {
        mainCriteria.createAlias(arg0, arg1, arg2, arg3);
        cloneCriteria.createAlias(arg0, arg1, arg2, arg3);
        return this;
    }

    @Override
    public Criteria createCriteria(final String arg0, final String arg1, final int arg2, final Criterion arg3) throws HibernateException {
        mainCriteria.createCriteria(arg0, arg1, arg2, arg3);
        cloneCriteria.createCriteria(arg0, arg1, arg2, arg3);
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return mainCriteria.isReadOnly();
    }

    @Override
    public boolean isReadOnlyInitialized() {
        return mainCriteria.isReadOnlyInitialized();
    }

    @Override
    public Criteria setReadOnly(final boolean arg0) {
        return mainCriteria.setReadOnly(arg0);
    }
}
