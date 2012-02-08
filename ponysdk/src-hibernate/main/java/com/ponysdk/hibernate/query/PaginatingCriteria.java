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

public class PaginatingCriteria implements OrderingCriteria {

    private final Criteria mainCriteria;

    private final Criteria cloneCriteria;

    boolean isOrderedByIDProperty = false;

    private final String IDProperty;

    private Map<String, OrderingCriteria> subCriteriaByAlias = new HashMap<String, OrderingCriteria>();

    public PaginatingCriteria(Class<?> clazz, Session session, String IDProperty) {
        mainCriteria = session.createCriteria(clazz);
        mainCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        cloneCriteria = session.createCriteria(clazz);
        cloneCriteria.setProjection(Projections.rowCount());
        cloneCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        this.IDProperty = IDProperty;
    }

    public PaginatingCriteria(Criteria mainCriteria, Criteria clonedCriteria, String IDProperty, Map<String, OrderingCriteria> subCriteriaByAlias) {
        this.mainCriteria = mainCriteria;
        this.cloneCriteria = clonedCriteria;
        this.IDProperty = IDProperty;
        this.subCriteriaByAlias = subCriteriaByAlias;
    }

    @Override
    public String getAlias() {
        return mainCriteria.getAlias();
    }

    @Override
    public OrderingCriteria setProjection(Projection projection) {
        mainCriteria.setProjection(projection);
        return this;
    }

    @Override
    public OrderingCriteria add(Criterion criterion) {
        cloneCriteria.add(criterion);
        mainCriteria.add(criterion);
        return this;
    }

    @Override
    public OrderingCriteria addOrder(Order order) {
        if (order.toString().contains(IDProperty)) {
            isOrderedByIDProperty = true;
        }
        mainCriteria.addOrder(order);
        return this;
    }

    @Override
    public OrderingCriteria setFetchMode(String associationPath, FetchMode mode) throws HibernateException {
        cloneCriteria.setFetchMode(associationPath, mode);
        mainCriteria.setFetchMode(associationPath, mode);
        return this;
    }

    @Override
    public OrderingCriteria setLockMode(LockMode lockMode) {
        mainCriteria.setLockMode(lockMode);
        return this;
    }

    @Override
    public OrderingCriteria setLockMode(String alias, LockMode lockMode) {
        mainCriteria.setLockMode(alias, lockMode);
        cloneCriteria.setLockMode(alias, lockMode);
        return this;
    }

    @Override
    public OrderingCriteria createAlias(String associationPath, String alias) throws HibernateException {
        final OrderingCriteria orderingCriteria = subCriteriaByAlias.get(alias);
        if (orderingCriteria != null) return orderingCriteria;
        mainCriteria.createAlias(associationPath, alias);
        cloneCriteria.createAlias(associationPath, alias);
        subCriteriaByAlias.put(alias, this);
        return this;
    }

    @Override
    public OrderingCriteria createAlias(String arg0, String arg1, int arg2) throws HibernateException {
        final OrderingCriteria orderingCriteria = subCriteriaByAlias.get(arg1);
        if (orderingCriteria != null) return orderingCriteria;
        mainCriteria.createAlias(arg0, arg1, arg2);
        cloneCriteria.createAlias(arg0, arg1, arg2);
        subCriteriaByAlias.put(arg1, this);
        return this;
    }

    @Override
    public OrderingCriteria createCriteria(String associationPath) throws HibernateException {
        return new PaginatingCriteria(mainCriteria.createCriteria(associationPath), cloneCriteria.createCriteria(associationPath), IDProperty, subCriteriaByAlias);
    }

    @Override
    public OrderingCriteria createCriteria(String arg0, int arg1) throws HibernateException {
        return new PaginatingCriteria(mainCriteria.createCriteria(arg0, arg1), cloneCriteria.createCriteria(arg0, arg1), IDProperty, subCriteriaByAlias);
    }

    @Override
    public OrderingCriteria createCriteria(String associationPath, String alias) throws HibernateException {
        mainCriteria.createCriteria(associationPath, alias);
        cloneCriteria.createCriteria(associationPath, alias);
        return this;
    }

    @Override
    public OrderingCriteria createCriteria(String arg0, String arg1, int arg2) throws HibernateException {

        OrderingCriteria orderingCriteria = subCriteriaByAlias.get(arg1);
        if (orderingCriteria == null) {
            orderingCriteria = new PaginatingCriteria(mainCriteria.createCriteria(arg0, arg1, arg2), cloneCriteria.createCriteria(arg0, arg1, arg2), IDProperty, subCriteriaByAlias);
            subCriteriaByAlias.put(arg1, orderingCriteria);
        }
        return orderingCriteria;
    }

    @Override
    public OrderingCriteria setResultTransformer(ResultTransformer resultTransformer) {
        mainCriteria.setResultTransformer(resultTransformer);
        cloneCriteria.setResultTransformer(resultTransformer);
        return this;
    }

    @Override
    public OrderingCriteria setMaxResults(int maxResults) {
        mainCriteria.setMaxResults(maxResults);
        return this;
    }

    @Override
    public OrderingCriteria setFirstResult(int firstResult) {
        mainCriteria.setFirstResult(firstResult);
        return this;
    }

    @Override
    public OrderingCriteria setFetchSize(int fetchSize) {
        mainCriteria.setFetchSize(fetchSize);
        return this;
    }

    @Override
    public OrderingCriteria setTimeout(int timeout) {
        mainCriteria.setTimeout(timeout);
        return this;
    }

    @Override
    public OrderingCriteria setCacheable(boolean cacheable) {
        mainCriteria.setCacheable(cacheable);
        return this;
    }

    @Override
    public OrderingCriteria setCacheRegion(String cacheRegion) {
        mainCriteria.setCacheRegion(cacheRegion);
        return this;
    }

    @Override
    public OrderingCriteria setComment(String comment) {
        cloneCriteria.setComment(comment);
        return this;
    }

    @Override
    public OrderingCriteria setFlushMode(FlushMode flushMode) {
        mainCriteria.setFlushMode(flushMode);
        return this;
    }

    @Override
    public OrderingCriteria setCacheMode(CacheMode cacheMode) {
        mainCriteria.setCacheMode(cacheMode);
        return this;
    }

    @Override
    public List<?> list() throws HibernateException {
        if (!isOrderedByIDProperty) mainCriteria.addOrder(Order.asc(IDProperty));
        mainCriteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        return mainCriteria.list();
    }

    @Override
    public ScrollableResults scroll() throws HibernateException {
        return mainCriteria.scroll();
    }

    @Override
    public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
        return mainCriteria.scroll(scrollMode);
    }

    @Override
    public Object uniqueResult() throws HibernateException {
        mainCriteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        return mainCriteria.uniqueResult();
    }

    @Override
    public int count() throws HibernateException {
        cloneCriteria.setResultTransformer(DISTINCT_ROOT_ENTITY);
        final Long count = (Long) cloneCriteria.uniqueResult();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Criteria createAlias(String arg0, String arg1, int arg2, Criterion arg3) throws HibernateException {
        mainCriteria.createAlias(arg0, arg1, arg2, arg3);
        cloneCriteria.createAlias(arg0, arg1, arg2, arg3);
        return this;
    }

    @Override
    public Criteria createCriteria(String arg0, String arg1, int arg2, Criterion arg3) throws HibernateException {
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
    public Criteria setReadOnly(boolean arg0) {
        return mainCriteria.setReadOnly(arg0);
    }
}
