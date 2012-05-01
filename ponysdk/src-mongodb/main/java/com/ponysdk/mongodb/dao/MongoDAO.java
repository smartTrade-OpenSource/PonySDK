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

package com.ponysdk.mongodb.dao;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import com.ponysdk.persistency.DAO;
import com.ponysdk.persistency.Identifiable;

public class MongoDAO implements DAO {

    protected Mongo mongo;
    protected DB db;

    public MongoDAO() {}

    @Override
    public Object save(final Object o) {
        saveOrUpdate(o);
        return null;
    }

    @Override
    public void saveOrUpdate(final Object o) {
        saveOrUpdate(o, o.getClass().getSimpleName().toLowerCase());
    }

    private void saveOrUpdate(final Object object, final String nameSpace) {
        final ObjectMapper mapper = new ObjectMapper();
        final StringWriter jsonOutput = new StringWriter();
        try {
            mapper.writeValue(jsonOutput, object);
            final DBObject dbObject = (DBObject) JSON.parse(jsonOutput.toString());
            final Identifiable m = (Identifiable) object;
            if (m.getID() != null) {
                db.getCollection(nameSpace).findAndModify(new BasicDBObject("_id", m.getID()), dbObject);
            } else {
                final ObjectId id = new ObjectId();
                dbObject.put("_id", id);
                db.getCollection(nameSpace).save(dbObject);
                m.setID(id);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void rollback() {}

    @Override
    public void commit() {}

    @Override
    public void beginTransaction() {}

    @Override
    public void delete(final Object o) {}

    public void setDB(final Mongo mongo) {
        this.mongo = mongo;
        this.db = mongo.getDB("mydb");
    }

    @Override
    public <T> List<T> find(final Object query) {
        return new ArrayList<T>();
    }
}
