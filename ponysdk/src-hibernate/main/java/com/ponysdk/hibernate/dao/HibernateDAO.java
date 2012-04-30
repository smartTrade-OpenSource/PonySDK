/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.hibernate.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.persistency.DAO;

public class HibernateDAO implements DAO {

    private static final Logger log = LoggerFactory.getLogger(HibernateDAO.class);

    protected SessionFactory sessionFactory;

    public HibernateDAO(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Serializable save(final Object object) {
        try {
            return sessionFactory.getCurrentSession().save(object);
        } catch (final RuntimeException e) {
            log.error("Persist failed", e);
            throw e;
        }
    }

    @Override
    public void saveOrUpdate(final Object object) {
        try {
            sessionFactory.getCurrentSession().saveOrUpdate(object);
        } catch (final RuntimeException e) {
            log.error("Persist failed", e);
            throw e;
        }
    }

    public void merge(final Object object) {
        try {
            sessionFactory.getCurrentSession().merge(object);
        } catch (final RuntimeException e) {
            log.error("Merge failed", e);
            throw e;
        }
    }

    @Override
    public void delete(final Object object) {
        try {
            sessionFactory.getCurrentSession().delete(object);
        } catch (final RuntimeException e) {
            log.error("Delete failed", e);
            throw e;
        }
    }

    public void flush() {
        sessionFactory.getCurrentSession().flush();
    }

    public void evict(final Object object) {
        sessionFactory.getCurrentSession().evict(object);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(final Class<T> _class) {
        final Criteria criteria = createCriteria(_class);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(final Class<T> _class, final int maxResults, final int firstResult) {
        final Criteria criteria = createCriteria(_class);
        return criteria.setMaxResults(maxResults).setFirstResult(firstResult).list();
    }

    @Override
    public void beginTransaction() {
        sessionFactory.getCurrentSession().beginTransaction();
    }

    @Override
    public void commit() {
        sessionFactory.getCurrentSession().getTransaction().commit();
    }

    @Override
    public void rollback() {
        sessionFactory.getCurrentSession().getTransaction().rollback();
    }

    public Criteria createCriteria(final Class<?> persistentClass) {
        return sessionFactory.getCurrentSession().createCriteria(persistentClass);
    }

    public Criteria createCriteria(final Class<?> pojoName, final String alias) {
        return sessionFactory.getCurrentSession().createCriteria(pojoName, alias);
    }

    public void closeTransaction() {
        sessionFactory.getCurrentSession().close();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}
