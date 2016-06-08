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

package com.ponysdk.core.export.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

public class PropertyUtil {

    private static final String NA = "NA";

    public static String getProperty(final Object bean, final String property)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final String propertyPath = property;
        Object propertyValue = NA;
        if (propertyPath != null) {

            final String[] tokens = propertyPath.split("\\.");
            propertyValue = getPropertyValue(bean, tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i].equals("toString")) {
                    propertyValue = propertyValue.toString();
                } else if (PropertyUtils.isReadable(propertyValue, tokens[i])) {
                    propertyValue = PropertyUtils.getProperty(propertyValue, tokens[i]);
                } else {
                    if (propertyValue instanceof List<?>) {
                        final List<?> propertyList = (List<?>) propertyValue;
                        final List<Object> values = new ArrayList<>();
                        for (final Object object : propertyList) {
                            values.add(getPropertyValue(object, tokens[i]));
                        }
                        if (values.isEmpty()) propertyValue = NA;
                        else propertyValue = values;
                    } else if (propertyValue instanceof Map<?, ?>) {
                        final Map<?, ?> propertyMap = (Map<?, ?>) propertyValue;
                        final List<Object> values = new ArrayList<>();
                        for (final Object object : propertyMap.values()) {
                            values.add(getPropertyValue(object, tokens[i]));
                        }
                        propertyValue = values;
                    } else {
                        propertyValue = NA;
                    }
                }
            }
        }
        return String.valueOf(propertyValue == null ? NA : propertyValue);
    }

    public static Object getPropertyValue(final Object bean, final String propertyName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object propertyValue;
        if (PropertyUtils.isReadable(bean, propertyName)) {
            propertyValue = PropertyUtils.getProperty(bean, propertyName);
            if (propertyValue == null) {
                propertyValue = NA;
            }
        } else {
            propertyValue = NA;
        }
        return propertyValue;
    }
}
