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

package com.ponysdk.ui.terminal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Property implements Serializable {

    private static final long serialVersionUID = -2137273148825763591L;

    private PropertyKey key = PropertyKey.ROOT;

    private String customKey;

    private String value;

    private List<String> values;

    protected Map<String, Property> childProperties = new HashMap<String, Property>();

    public Property() {}

    public Property(PropertyKey key, String value) {
        this.key = key;
        this.value = value;
    }

    public Property(PropertyKey key, List<String> values) {
        this.key = key;
        this.values = values;
    }

    public Property(PropertyKey key, int value) {
        this(key, String.valueOf(value));
    }

    public Property(PropertyKey key, long value) {
        this(key, String.valueOf(value));
    }

    public Property(PropertyKey key, double value) {
        this(key, String.valueOf(value));
    }

    public Property(PropertyKey key, boolean value) {
        this(key, String.valueOf(value));
    }

    public Property(PropertyKey key, int... values) {
        this(key, asStringList(values));
    }

    public void setKey(PropertyKey key) {
        this.key = key;
    }

    public PropertyKey getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public void setProperty(PropertyKey propertyKey, String value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setProperty(PropertyKey propertyKey, int value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setProperty(PropertyKey propertyKey, long value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setProperty(PropertyKey propertyKey, double value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setProperty(PropertyKey propertyKey, boolean value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public Property getChildProperty(PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name());
    }

    public String getStringProperty(PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name()).getValue();
    }

    public int getIntProperty(PropertyKey propertyKey) {
        return Integer.parseInt(childProperties.get(propertyKey.name()).getValue());
    }

    public long getLongProperty(PropertyKey propertyKey) {
        return Long.parseLong(childProperties.get(propertyKey.name()).getValue());
    }

    public double getDoubleProperty(PropertyKey propertyKey) {
        return Double.parseDouble(childProperties.get(propertyKey.name()).getValue());
    }

    public boolean getBooleanProperty(PropertyKey propertyKey) {
        return Boolean.parseBoolean(childProperties.get(propertyKey.name()).getValue());
    }

    public List<String> getListStringProperty(PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name()).getValues();
    }

    public List<Integer> getListIntegerProperty(PropertyKey propertyKey) {
        final List<String> values = childProperties.get(propertyKey.name()).getValues();
        return asIntegerList(values);
    }

    public boolean hasChildProperty(PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name()) != null;
    }

    public int getIntValue() {
        return Integer.parseInt(value);
    }

    public long getLongValue() {
        return Long.parseLong(value);
    }

    public double getDoubleValue() {
        return Double.parseDouble(value);
    }

    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }

    public Map<String, Property> getChildProperties() {
        return childProperties;
    }

    public void setProperties(List<Property> properties) {
        for (final Property property : properties) {
            this.childProperties.put(property.getKey().name(), property);
        }
    }

    public String getCustomKey() {
        return customKey;
    }

    public void setCustomKey(String customKey) {
        this.customKey = customKey;
    }

    @Override
    public String toString() {
        return "Property [key=" + key + ", value=" + value + ", custom key=" + customKey + ", childProperties=" + childProperties + "]";
    }

    private static List<String> asStringList(int... values) {
        final List<String> stringList = new ArrayList<String>(values.length);
        for (final Integer integer : values) {
            stringList.add(String.valueOf(integer));
        }
        return stringList;
    }

    private static List<Integer> asIntegerList(List<String> values) {
        final List<Integer> integerList = new ArrayList<Integer>(values.size());
        for (final String value : values) {
            integerList.add(Integer.parseInt(value));
        }
        return integerList;
    }

}
