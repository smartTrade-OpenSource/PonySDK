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

package com.ponysdk.impl.query.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.query.Criterion;
import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Query.QueryMode;
import com.ponysdk.core.query.Result;
import com.ponysdk.core.query.SortingType;

public final class FilteringTools {

    private static final String DOT_REGEX = "\\.";

    private static final Logger log = LoggerFactory.getLogger(FilteringTools.class);

    private static final String REGEX_END = "$";

    private static final String REGEX_MEMORY_SYNTAXE = ".*";

    private static final String REGEX_DATABASE_SYNTAXE = "%";

    private static final String REGEX_BEGIN = "^";

    private static final String EMPTY = "";

    private static final String KEYWORD_KEYS = "keys";

    private static final String KEYWORD_VALUES = "values";

    @SuppressWarnings("unchecked")
    public static <T> List<T> sortByPropertyName(final List<T> data, final String propertyName) {
        if (data == null) return null;
        try {
            Collections.sort(data, getPropertyComparator(propertyName));
        } catch (final ClassCastException e) {
            log.error("Failed to sort data collection [Property :" + propertyName + "]", e);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> sortByPropertyName(final List<T> data, final String propertyName, final Comparator<T> comparator) {
        if (data == null) return null;
        try {
            Collections.sort(data, getPropertyComparator(propertyName, comparator));
        } catch (final ClassCastException e) {
            log.error("Failed to sort data collection [Property :" + propertyName + "]", e);
        }
        return data;
    }

    public static final BeanComparator getPropertyComparator(final String propertyName) {
        return new BeanComparator((null != propertyName) ? propertyName : "name");
    }

    public static <U> BeanComparator getPropertyComparator(final String propertyName, final Comparator<U> comparator) {
        return new BeanComparator((null != propertyName) ? propertyName : "name", comparator) {

            private static final long serialVersionUID = 5957817419869265091L;

            @SuppressWarnings("unchecked")
            @Override
            public int compare(final Object o1, final Object o2) {
                final String property = getProperty();
                if (property == null) { return getComparator().compare(o1, o2); }

                try {
                    final Object value1 = getValue(o1, property.split(DOT_REGEX));
                    final Object value2 = getValue(o2, property.split(DOT_REGEX));
                    return getComparator().compare(value1, value2);
                } catch (final Exception e) {
                    return getComparator().compare(o1, o2);
                }
            }
        };
    }

    public static String emptyIfNull(final String str) {
        return (str != null) ? str : EMPTY;
    }

    public static List<String> filter(final List<String> datas, final String patternMatching) {
        if (patternMatching == null || datas == null) { return datas; }
        final List<String> validData = new ArrayList<>();
        try {
            for (final String data : datas) {
                if (data == null) continue;
                if (data.equalsIgnoreCase(patternMatching)) {
                    validData.add(data);
                    continue;
                }
                // Now we can filter our data against the pattern
                final String text = normalisePattern(patternMatching.trim());
                final Pattern pattern = Pattern.compile(REGEX_BEGIN + text + REGEX_END, Pattern.CASE_INSENSITIVE);
                Matcher matcher = null;
                matcher = pattern.matcher(data);
                if ((matcher != null) && matcher.find()) {
                    validData.add(data);
                } else {
                    matcher = pattern.matcher("");
                    if ((matcher != null) && matcher.find()) {
                        validData.add(data);
                    }
                }
            }
        } catch (final PatternSyntaxException e) {
            if (log.isDebugEnabled()) {
                log.debug("bad pattern : " + patternMatching);
            }
        } catch (final Exception e) {
            log.error("Filter Error => pattern : " + patternMatching, e);
        }
        return validData;
    }

    public static <T> List<T> filter(final List<T> datas, final String fieldKey, final Object value) {
        if (value == null || datas == null || fieldKey.equals(EMPTY)) { return datas; }
        final List<T> validData = new ArrayList<>();
        try {
            final String[] pathDetails = fieldKey.split(DOT_REGEX);
            for (final T data : datas) {
                final Object val = getValue(data, pathDetails);
                if (val == null) continue;
                if (value.equals(val)) {
                    validData.add(data);
                    continue;
                }
                // Now we can filter our data against the pattern
                final String text = normalisePattern(value.toString().trim());
                final Pattern pattern = Pattern.compile(REGEX_BEGIN + text + REGEX_END, Pattern.CASE_INSENSITIVE);
                Matcher matcher = null;
                if (val instanceof Collection<?>) {
                    final Collection<?> collection = (Collection<?>) val;
                    for (final Object item : collection) {
                        matcher = pattern.matcher(item.toString());
                        if ((matcher != null) && matcher.find()) {
                            validData.add(data);
                            break;
                        }
                    }
                    if (collection.isEmpty()) {
                        matcher = pattern.matcher("");
                        if ((matcher != null) && matcher.find()) {
                            validData.add(data);
                        }
                    }
                } else if (val.toString() != null) {
                    matcher = pattern.matcher(val.toString());
                    if ((matcher != null) && matcher.find()) {
                        validData.add(data);
                    } else {
                        matcher = pattern.matcher("");
                        if ((matcher != null) && matcher.find()) {
                            validData.add(data);
                        }
                    }
                }
            }
        } catch (final PatternSyntaxException e) {
            if (log.isDebugEnabled()) {
                log.debug("bad pattern : " + value);
            }
        } catch (final Exception e) {
            log.error("Filter Error => pattern : " + value + " , property : " + fieldKey, e);
        }
        return validData;
    }

    /**
     * Used to Filter a list of data, scoped by a <code>propertyPath</code>, according to a
     * <code>patternName</code>. The property path is used to go as deep as one wants into each object of the
     * <code>datas</code>. It takes form as a string representing attributes separated by dots, e.g.
     * <code>attribute1.attribute2.attribute3</code><br/>
     * <br/>
     * If an attribute is a map, the tokens <code>keys</code> or <code>values</code> can be used to retrieve
     * the corresponding data as a Collection. E.g. <code>attribute1.mapAttribute.keys.attribute2</code>
     * 
     * @param <T>
     *            the type of the data obtained with the propertyPath that is tested against the patternName
     * @param datas
     *            the list of data to be filtered
     * @param propertyPath
     *            the chain of attribute representing a path deep into the data objects
     * @param patternName
     *            a string used to filter data
     * @return the list of filtered data
     */
    public static <T> List<T> filter(final List<T> datas, final String propertyPath, final String patternName) {
        if (datas == null || patternName.equals(EMPTY) || propertyPath.equals(EMPTY)) { return datas; }
        final List<T> validData = new ArrayList<>();
        try {
            final String[] pathDetails = propertyPath.split(DOT_REGEX);
            for (final T data : datas) {
                final Object val = getValue(data, pathDetails);
                if (val == null) continue;
                // Now we can filter our data against the pattern
                final String value = normalisePattern(patternName.trim());
                final Pattern pattern = Pattern.compile(REGEX_BEGIN + value + REGEX_END, Pattern.CASE_INSENSITIVE);
                Matcher matcher = null;
                if (val instanceof Collection<?>) {
                    final Collection<?> collection = (Collection<?>) val;
                    for (final Object item : collection) {
                        matcher = pattern.matcher(item.toString());
                        if ((matcher != null) && matcher.find()) {
                            validData.add(data);
                            break;
                        }
                    }
                    if (collection.isEmpty()) {
                        matcher = pattern.matcher("");
                        if ((matcher != null) && matcher.find()) {
                            validData.add(data);
                        }
                    }
                } else if (val.toString() != null) {
                    matcher = pattern.matcher(val.toString());
                    if ((matcher != null) && matcher.find()) {
                        validData.add(data);
                    } else {
                        matcher = pattern.matcher("");
                        if ((matcher != null) && matcher.find()) {
                            validData.add(data);
                        }
                    }
                }
            }
        } catch (final PatternSyntaxException e) {
            if (log.isDebugEnabled()) {
                log.debug("bad pattern : " + patternName);
            }
        } catch (final Exception e) {
            log.error("Filter Error => pattern : " + patternName + " , property : " + propertyPath, e);
        }
        return validData;
    }

    @SuppressWarnings("unchecked")
    public static <T> Object getValue(final T currentData, final String[] pathDetails) throws Exception {
        final int lastIndex = pathDetails.length - 1;
        int curIndex = 0;
        Object val = null;
        Object tmpVal = PropertyUtils.getProperty(currentData, pathDetails[curIndex]);
        if (tmpVal == null) return null;
        curIndex++;
        // We parse the pathDetails array to extract the target object
        while (curIndex <= lastIndex) {
            if (pathDetails[curIndex].equals(KEYWORD_KEYS) && tmpVal instanceof Map) {
                tmpVal = ((Map<?, ?>) tmpVal).keySet();
            } else if (pathDetails[curIndex].equals(KEYWORD_VALUES) && tmpVal instanceof Map) {
                tmpVal = ((Map<?, ?>) tmpVal).values();
            } else if (tmpVal instanceof Collection) {
                Object value = null;
                for (final Object o : (Collection<?>) tmpVal) {
                    if (o instanceof Entry) {
                        final Entry<?, ?> entry = (Entry<?, ?>) o;
                        if (entry.getKey().equals(pathDetails[curIndex])) {
                            value = entry.getValue();
                        }
                    } else {
                        if (value == null) value = new ArrayList<>();
                        ((ArrayList<Object>) value).add(PropertyUtils.getProperty(o, pathDetails[curIndex]));
                    }
                }
                tmpVal = value;
            } else {
                tmpVal = PropertyUtils.getProperty(tmpVal, pathDetails[curIndex]);
            }
            curIndex++;
        }
        // Once we have our final object we should be able to cast it
        val = tmpVal;
        return val;
    }

    public static <T> List<T> filter(List<T> datas, final Map<String, String> fields) {
        for (final Map.Entry<String, String> entry : fields.entrySet()) {
            datas = filter(datas, entry.getKey(), entry.getValue());
        }
        return datas;
    }

    public static <T> List<T> filter(List<T> datas, final List<Criterion> criteria) {
        if (criteria == null) return datas;

        for (final Criterion criterion : criteria) {
            datas = filter(datas, criterion.getPojoProperty(), criterion.getValue());
        }
        return datas;
    }

    public static List<String> filterStringCollection(List<String> datas, final List<Criterion> criteria) {
        if (criteria == null) return datas;

        for (final Criterion criterion : criteria) {
            datas = filter(datas, (String) criterion.getValue());
        }
        return datas;
    }

    // TODO nciaravola must be a criterion into SmartCC Criteria
    public static <T> List<T> filterByDisjunction(final List<T> datas, final List<Criterion> criteria) {
        if (criteria == null || criteria.isEmpty()) return datas;

        final List<T> result = new ArrayList<>();
        for (final Criterion criterion : criteria) {
            result.addAll(filter(datas, criterion.getPojoProperty(), criterion.getValue()));
        }
        return result;
    }

    public static <T> List<T> sort(List<T> datas, final List<Criterion> criteria) {
        if (criteria == null) return datas;

        for (final Criterion criterion : criteria) {
            final SortingType sortingType = criterion.getSortingType();
            if (sortingType != null && sortingType != SortingType.NONE) {
                final Comparator<T> comparator = new Comparator<T>() {

                    @Override
                    public int compare(final T o1, final T o2) {
                        if (o1 == null && o2 == null) return 0;
                        if (o1 == null) {
                            if (sortingType == SortingType.ASCENDING) return -1;
                            return 1;
                        }
                        if (o2 == null) {
                            if (sortingType == SortingType.ASCENDING) return 1;
                            return -1;
                        }
                        if (o1.equals(o2)) return 0;
                        if (sortingType == SortingType.ASCENDING) { return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase()); }
                        return o2.toString().toLowerCase().compareTo(o1.toString().toLowerCase());
                    }

                };
                datas = sortByPropertyName(datas, criterion.getPojoProperty(), comparator);
            }
        }
        return datas;
    }

    public static List<String> sortStringCollection(final List<String> datas, final List<Criterion> criteria) {
        if (criteria == null) return datas;

        for (final Criterion criterion : criteria) {
            final SortingType sortingType = criterion.getSortingType();
            if (sortingType != null && sortingType != SortingType.NONE) {
                final Comparator<String> comparator = new Comparator<String>() {

                    @Override
                    public int compare(final String o1, final String o2) {
                        if (o1 == null && o2 == null) return 0;
                        if (o1 == null) {
                            if (sortingType == SortingType.ASCENDING) return -1;
                            return 1;
                        }
                        if (o2 == null) {
                            if (sortingType == SortingType.ASCENDING) return 1;
                            return -1;
                        }
                        if (o1.equals(o2)) return 0;
                        if (sortingType == SortingType.ASCENDING) { return o1.toLowerCase().compareTo(o2.toLowerCase()); }
                        return o2.toLowerCase().compareTo(o1.toLowerCase());
                    }

                };
                Collections.sort(datas, comparator);
            }
        }
        return datas;
    }

    public static <T> List<T> getPage(final int pageSize, final int page, final List<T> result) {
        if ((result == null) || (result.size() == 0)) { return result; }
        if (result.size() < pageSize) return result;
        if ((page * pageSize) > result.size()) {
            // return last page
            final int lastPage = result.size() / pageSize;
            return result.subList(lastPage * pageSize, result.size());
        }
        return new ArrayList<>(result.subList(page * pageSize, Math.min(result.size(), page * pageSize + pageSize)));
    }

    public static <T> Result<List<T>> select(final Query query, List<T> data) {
        data = FilteringTools.filter(data, query.getCriteria());
        data = FilteringTools.sort(data, query.getCriteria());

        final int count = data.size();

        if (!QueryMode.FULL_RESULT.equals(query.getQueryMode())) {
            data = FilteringTools.getPage(query.getPageSize(), query.getPageNum(), data);
        }

        final Result<List<T>> result = new Result<>(data);
        result.setFullSize(count);
        return result;
    }

    /**
     * $ => //$
     */
    private static String normalisePattern(final String pattern) {
        return pattern.replaceAll("\\$", "\\\\\\$").replaceAll(REGEX_DATABASE_SYNTAXE, REGEX_MEMORY_SYNTAXE);
    }
}
