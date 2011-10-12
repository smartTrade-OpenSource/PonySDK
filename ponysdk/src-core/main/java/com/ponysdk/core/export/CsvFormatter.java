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
package com.ponysdk.core.export;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.commons.beanutils.PropertyUtils;

public class CsvFormatter {

    private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final String NA = "NA";
    private static final String SEPARATOR_KEY_VALUE = " = ";
    private static final String SEPARATOR_COLLECTION_ELEMENT = "|";
    private static final String SEPARATOR_END_COLLECTION = "]";
    private static final String SEPARATOR_START_COLLECTION = "[";
    private static final String EMPTY_SEPARATOR = " ";
    private static final String SEPARATOR = ";";
    private static final String EMPTY_VALUE = "";

    private final List<ExportableField> exportableFields;
    private final SimpleDateFormat fullDateFormatter;
    private final SimpleDateFormat stlmtDateFormatter;

    public CsvFormatter(List<ExportableField> exportableFields, String dateFormat, TimeZone timezone) {
        this.exportableFields = exportableFields;
        if (dateFormat == null) {
            fullDateFormatter = new SimpleDateFormat();
        } else {
            fullDateFormatter = new SimpleDateFormat(dateFormat);
        }
        if (timezone != null)
            fullDateFormatter.setTimeZone(timezone);

        stlmtDateFormatter = new SimpleDateFormat(DATE_FORMAT);
        stlmtDateFormatter.setTimeZone(TIME_ZONE_UTC);
    }

    public String formatObject(Object bean) throws Exception {

        final SimpleDateFormat usFormatUTC = new SimpleDateFormat(DATE_FORMAT);
        usFormatUTC.setTimeZone(TIME_ZONE_UTC);
        final List<Object> propertyValues = new ArrayList<Object>();

        for (final ExportableField exportableField : exportableFields) {
            final String propertyPath = exportableField.getKey();
            if (propertyPath != null) {
                Object propertyValue = null;

                final String[] tokens = propertyPath.split("\\.");
                propertyValue = getPropertyValue(bean, tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    if (tokens[i - 1].equals("customFields")) {
                        final Method method = bean.getClass().getMethod("getCustomField", String.class);
                        propertyValue = method.invoke(bean, tokens[i]);
                    } else if (PropertyUtils.isReadable(propertyValue, tokens[i])) {
                        propertyValue = PropertyUtils.getProperty(propertyValue, tokens[i]);
                    } else {
                        if (propertyValue instanceof List<?>) {
                            final List<?> propertyList = (List<?>) propertyValue;
                            final List<Object> values = new ArrayList<Object>();
                            for (final Object object : propertyList) {
                                values.add(getPropertyValue(object, tokens[i]));
                            }
                            propertyValue = values;
                        } else if (propertyValue instanceof Map<?, ?>) {
                            final Map<?, ?> propertyMap = (Map<?, ?>) propertyValue;
                            final List<Object> values = new ArrayList<Object>();
                            for (final Object object : propertyMap.values()) {
                                values.add(getPropertyValue(object, tokens[i]));
                            }
                            propertyValue = values;
                        } else {
                            propertyValue = NA;
                        }
                    }
                }
                propertyValues.add(propertyValue);
            }
        }
        return getFormatedObject(propertyValues);
    }

    private Object getPropertyValue(Object bean, final String propertyName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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

    private String getFormatedObject(List<Object> values) {
        final StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < values.size(); i++) {
            final Object value = values.get(i);
            if (value == null) {
                stringBuffer.append(EMPTY_VALUE);
            } else {
                final ExportableField exportableField = exportableFields.get(i);
                if (value instanceof List<?>) {
                    stringBuffer.append(SEPARATOR_START_COLLECTION);
                    final List<?> listOfValues = (List<?>) value;
                    for (int j = 0; j < listOfValues.size(); j++) {
                        stringBuffer.append(formattedPrint(listOfValues.get(j), exportableField));
                        if (j < listOfValues.size() - 1) {
                            stringBuffer.append(SEPARATOR_COLLECTION_ELEMENT);
                        }
                    }
                    stringBuffer.append(SEPARATOR_END_COLLECTION);
                } else if (value instanceof Map<?, ?>) {
                    stringBuffer.append(SEPARATOR_START_COLLECTION);
                    final Map<?, ?> listOfValues = (Map<?, ?>) value;
                    for (final Entry<?, ?> entry : listOfValues.entrySet()) {
                        stringBuffer.append(formattedPrint(entry.getKey(), exportableField) + SEPARATOR_KEY_VALUE + formattedPrint(entry.getValue(), exportableField));
                    }
                    stringBuffer.append(SEPARATOR_END_COLLECTION);
                } else {
                    final String formattedValue = formattedPrint(value, exportableField);
                    stringBuffer.append(formattedValue);
                }
            }
            stringBuffer.append(SEPARATOR);
        }
        return stringBuffer.toString();
    }

    private String formattedPrint(Object s, ExportableField exportableField) {
        if (s == null || s.toString() == null)
            return EMPTY_VALUE;
        if (s instanceof Date) {
            if (exportableField.getKey().equals("stlmtDate")) {
                return stlmtDateFormatter.format((Date) s);
            }
            return fullDateFormatter.format((Date) s);
        }
        return s.toString().replaceAll(SEPARATOR, EMPTY_SEPARATOR);
    }

    public String getHeader() {
        final StringBuffer buffer = new StringBuffer();
        final Iterator<ExportableField> iter = exportableFields.iterator();

        while (iter.hasNext()) {
            final ExportableField exportableField = iter.next();
            final String header = exportableField.getCaption();
            buffer.append(header);
            if (iter.hasNext())
                buffer.append(SEPARATOR);
        }
        return buffer.toString();
    }
}
