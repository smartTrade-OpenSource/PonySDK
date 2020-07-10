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

package com.ponysdk.core.server.service.query;

public class Criterion {

    private final String pojoProperty;

    private Object value;

    private ComparatorType comparator = ComparatorType.EQ;

    private SortingType sortingType = SortingType.NONE;

    public Criterion(final String key) {
        this.pojoProperty = key;
    }

    public String getPojoProperty() {
        return pojoProperty;
    }

    public SortingType getSortingType() {
        return sortingType;
    }

    public Criterion setSortingType(final SortingType sortingType) {
        this.sortingType = sortingType;
        return this;
    }

    public ComparatorType getComparator() {
        return comparator;
    }

    public Criterion setComparator(final ComparatorType comparator) {
        this.comparator = comparator;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public Criterion setValue(final Object value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return "CriteriaField [pojoProperty=" + pojoProperty + ", value=" + value + ", comparator=" + comparator + ", sortingType="
                + sortingType + "]";
    }

}
