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

package com.ponysdk.core.query;

public class CriterionField {

    private final String pojoProperty;

    private Object value;

    private ComparatorType comparator = ComparatorType.EQ;

    private SortingType sortingType = SortingType.NONE;

    public CriterionField(String key) {
        this.pojoProperty = key;
    }

    public String getPojoProperty() {
        return pojoProperty;
    }

    public SortingType getSortingType() {
        return sortingType;
    }

    public void setSortingType(SortingType sortingType) {
        this.sortingType = sortingType;
    }

    public ComparatorType getComparator() {
        return comparator;
    }

    public void setComparator(ComparatorType comparator) {
        this.comparator = comparator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "CriteriaField [pojoProperty=" + pojoProperty + ", value=" + value + ", comparator=" + comparator + ", sortingType=" + sortingType + "]";
    }

    public boolean isVisibleFilterInForm() {
        return true;
    }

}
