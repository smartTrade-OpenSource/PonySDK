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

public class CsvExporterFormatter {

    // private static final TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
    // private static final String DATE_FORMAT = "MM/dd/yyyy";
    // private static final String NA = "NA";
    // private static final String SEPARATOR_KEY_VALUE = " = ";
    // private static final String SEPARATOR_COLLECTION_ELEMENT = "|";
    // private static final String SEPARATOR_END_COLLECTION = "]";
    // private static final String SEPARATOR_START_COLLECTION = "[";
    // private static final String EMPTY_SEPARATOR = " ";
    // private static final String SEPARATOR = ",";
    // private static final String EMPTY_VALUE = "";
    // private static final Logger log = Logger.getLogger(CsvExporterFormatter.class);
    //
    // private final List<CriterionField> fieldsToExports = new ArrayList<CriterionField>();
    // private final SimpleDateFormat fullDateFormatter;
    // private final SimpleDateFormat stlmtDateFormatter;
    //
    // public CsvExporterFormatter(List<CriterionField> fields, String dateFormat, TimeZone timezone) {
    // for (final CriterionField filterField : fields) {
    // if (filterField.isExportable()) {
    // fieldsToExports.add(filterField);
    // }
    // }
    // if (dateFormat == null) {
    // fullDateFormatter = new SimpleDateFormat();
    // } else {
    // fullDateFormatter = new SimpleDateFormat(dateFormat);
    // }
    // if (timezone != null) fullDateFormatter.setTimeZone(timezone);
    //
    // stlmtDateFormatter = new SimpleDateFormat(DATE_FORMAT);
    // stlmtDateFormatter.setTimeZone(TIME_ZONE_UTC);
    // }
    //
    // public void writeObject(PrintWriter out, Object bean) throws Exception {
    // if (out == null) {
    // log.error("writer is null, set writer before added object");
    // return;
    // }
    // final SimpleDateFormat usFormatUTC = new SimpleDateFormat(DATE_FORMAT);
    // usFormatUTC.setTimeZone(TIME_ZONE_UTC);
    // final List<Object> propertyValues = new ArrayList<Object>();
    // if (fieldsToExports != null) {
    // for (int kk = 0; kk < fieldsToExports.size(); kk++) {
    // final FilterField field = fieldsToExports.get(kk);
    // if (!field.isExportable()) continue;
    // final String propertyPath = field.getPojoPropertyName();
    // if (propertyPath != null) {
    // Object propertyValue = null;
    //
    // final String[] tokens = propertyPath.split("\\.");
    // propertyValue = getPropertyValue(bean, tokens[0]);
    // for (int i = 1; i < tokens.length; i++) {
    // if (PropertyUtils.isReadable(propertyValue, tokens[i])) {
    // propertyValue = PropertyUtils.getProperty(propertyValue, tokens[i]);
    // } else {
    // if (propertyValue instanceof List<?>) {
    // final List<?> propertyList = (List<?>) propertyValue;
    // final List<Object> values = new ArrayList<Object>();
    // for (final Object object : propertyList) {
    // values.add(getPropertyValue(object, tokens[i]));
    // }
    // propertyValue = values;
    // } else if (propertyValue instanceof Map<?, ?>) {
    // final Map<?, ?> propertyMap = (Map<?, ?>) propertyValue;
    // final List<Object> values = new ArrayList<Object>();
    // for (final Object object : propertyMap.values()) {
    // values.add(getPropertyValue(object, tokens[i]));
    // }
    // propertyValue = values;
    // } else {
    // propertyValue = NA;
    // }
    // }
    // }
    // propertyValues.add(propertyValue);
    // }
    // }
    // } else {
    // log.warn("Empty column definition");
    // }
    // write(out, propertyValues);
    // }
    //
    // private Object getPropertyValue(Object bean, final String propertyName) throws IllegalAccessException,
    // InvocationTargetException,
    // NoSuchMethodException {
    // Object propertyValue;
    // if (PropertyUtils.isReadable(bean, propertyName)) {
    // propertyValue = PropertyUtils.getProperty(bean, propertyName);
    // if (propertyValue == null) {
    // propertyValue = NA;
    // }
    // } else {
    // propertyValue = NA;
    // }
    // return propertyValue;
    // }
    //
    // private void write(PrintWriter out, List<Object> values) {
    // for (int i = 0; i < values.size(); i++) {
    // final Object value = values.get(i);
    // if (value == null) {
    // out.print(EMPTY_VALUE);
    // } else {
    // final CriterionField field = fieldsToExports.get(i);
    // if (value instanceof List<?>) {
    // out.print(SEPARATOR_START_COLLECTION);
    // final List<?> listOfValues = (List<?>) value;
    // for (int j = 0; j < listOfValues.size(); j++) {
    // out.print(formattedPrint(listOfValues.get(j), field));
    // if (j < listOfValues.size() - 1) {
    // out.print(SEPARATOR_COLLECTION_ELEMENT);
    // }
    // }
    // out.print(SEPARATOR_END_COLLECTION);
    // } else if (value instanceof Map<?, ?>) {
    // out.print(SEPARATOR_START_COLLECTION);
    // final Map<?, ?> listOfValues = (Map<?, ?>) value;
    // for (final Entry<?, ?> entry : listOfValues.entrySet()) {
    // out.print(formattedPrint(entry.getKey(), field) + SEPARATOR_KEY_VALUE
    // + formattedPrint(entry.getValue(), field));
    // }
    // out.print(SEPARATOR_END_COLLECTION);
    // } else {
    // final String formattedValue = formattedPrint(value, field);
    // out.print(formattedValue);
    // }
    // }
    // out.print(SEPARATOR);
    // }
    // out.println();
    // }
    //
    // private String formattedPrint(Object s, CriterionField field) {
    // if (s == null || s.toString() == null) return EMPTY_VALUE;
    // if (s instanceof Date) {
    // if (field.getKey().equals("stlmtDate")) {
    // return stlmtDateFormatter.format((Date) s);
    // }
    // return fullDateFormatter.format((Date) s);
    // }
    // return s.toString().replaceAll(SEPARATOR, EMPTY_SEPARATOR);
    // }
    //
    // public void writeHeader(PrintWriter out, List<CriterionField> fields) {
    // for (int i = 0; i < fields.size(); i++) {
    // final CriterionField field = fields.get(i);
    // if (!field.isExportable()) continue;
    // final String header = field.get();
    // out.print(header.toLowerCase());
    // if (i < fields.size() - 1) {
    // out.print(SEPARATOR);
    // }
    // }
    // out.println();
    // }
}
