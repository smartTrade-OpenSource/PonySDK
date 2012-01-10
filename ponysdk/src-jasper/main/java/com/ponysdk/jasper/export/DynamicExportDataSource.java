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

package com.ponysdk.jasper.export;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.apache.commons.beanutils.PropertyUtils;

public class DynamicExportDataSource implements JRDataSource {

    private final Collection<?> list;

    private final Iterator<?> iterator;

    private Object currentItem;

    public DynamicExportDataSource(Collection<?> collection) {
        super();
        this.list = collection;
        this.iterator = collection.iterator();
    }

    public DynamicExportDataSource(Map<?, ?> map) {
        super();
        this.list = map.values();
        this.iterator = this.list.iterator();
    }

    @Override
    public boolean next() throws JRException {
        if (iterator == null) return false;

        if (iterator.hasNext()) {
            currentItem = iterator.next();
            return true;
        }
        return false;
    }

    @Override
    public Object getFieldValue(JRField field) throws JRException {
        try {
            return PropertyUtils.getProperty(currentItem, field.getName());
        } catch (final Exception e) {
            throw new JRException("Cannot extract field value # " + field.getName(), e);
        }
    }

}
