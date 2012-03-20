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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Property implements Serializable {

    private static final long serialVersionUID = -2137273148825763591L;

    private String key = PropertyKey.ROOT.getCode();
    private String value;

    private List<String> values;

    protected Map<String, Property> childProperties = new HashMap<String, Property>();

    public Property() {}

    public Property(final PropertyKey key, final String value) {
        this.key = key.getCode();
        this.value = value;
    }

    public Property(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public Property(final PropertyKey key, final List<String> values) {
        this.key = key.getCode();
        this.values = values;
    }

    public Property(final String key, final List<String> values) {
        this.key = key;
        this.values = values;
    }

    public Property(final PropertyKey key, final int value) {
        this(key, String.valueOf(value));
    }

    public Property(final String key, final int value) {
        this(key, String.valueOf(value));
    }

    public Property(final PropertyKey key, final long value) {
        this(key, String.valueOf(value));
    }

    public Property(final String key, final long value) {
        this(key, String.valueOf(value));
    }

    public Property(final PropertyKey key, final double value) {
        this(key, String.valueOf(value));
    }

    public Property(final String key, final double value) {
        this(key, String.valueOf(value));
    }

    public Property(final PropertyKey key, final boolean value) {
        this(key, String.valueOf(value));
    }

    public Property(final String key, final boolean value) {
        this(key, String.valueOf(value));
    }

    public Property(final PropertyKey key, final int... values) {
        this(key, asStringList(values));
    }

    public Property(final String key, final int... values) {
        this(key, asStringList(values));
    }

    public void setPropertyKey(final PropertyKey key) {
        this.key = key.getCode();
    }

    public PropertyKey getPropertyKey() {
        return PropertyKey.from(key);
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(final List<String> values) {
        this.values = values;
    }

    public void setProperty(final PropertyKey propertyKey, final String value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setAddonProperty(final String propertyKey, final String value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey, property);
    }

    public void setProperty(final PropertyKey propertyKey, final int value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setAddonProperty(final String propertyKey, final int value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey, property);
    }

    public void setProperty(final PropertyKey propertyKey, final long value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setAddonProperty(final String propertyKey, final long value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey, property);
    }

    public void setProperty(final PropertyKey propertyKey, final double value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setAddonProperty(final String propertyKey, final double value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey, property);
    }

    public void setProperty(final PropertyKey propertyKey, final boolean value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey.name(), property);
    }

    public void setAddonProperty(final String propertyKey, final boolean value) {
        final Property property = new Property(propertyKey, value);
        childProperties.put(propertyKey, property);
    }

    public boolean containsChildProperty(final PropertyKey propertyKey) {
        return childProperties.containsKey(propertyKey.name());
    }

    public boolean containsChildAddonProperty(final String propertyKey) {
        return childProperties.containsKey(propertyKey);
    }

    public Property getChildProperty(final PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name());
    }

    public Property getChildAddonProperty(final String propertyKey) {
        return childProperties.get(propertyKey);
    }

    public String getStringPropertyValue(final PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name()).getValue();
    }

    public String getStringAddonPropertyValue(final String propertyKey) {
        return childProperties.get(propertyKey).getValue();
    }

    public int getIntPropertyValue(final PropertyKey propertyKey) {
        return Integer.parseInt(childProperties.get(propertyKey.name()).getValue());
    }

    public int getIntAddonPropertyValue(final String propertyKey) {
        return Integer.parseInt(childProperties.get(propertyKey).getValue());
    }

    public long getLongPropertyValue(final PropertyKey propertyKey) {
        return Long.parseLong(childProperties.get(propertyKey.name()).getValue());
    }

    public long getLongAddonPropertyValue(final String propertyKey) {
        return Long.parseLong(childProperties.get(propertyKey).getValue());
    }

    public double getDoublePropertyValue(final PropertyKey propertyKey) {
        return Double.parseDouble(childProperties.get(propertyKey.name()).getValue());
    }

    public double getDoubleAddonPropertyValue(final String propertyKey) {
        return Double.parseDouble(childProperties.get(propertyKey).getValue());
    }

    public boolean getBooleanPropertyValue(final PropertyKey propertyKey) {
        return Boolean.parseBoolean(childProperties.get(propertyKey.name()).getValue());
    }

    public boolean getBooleanAddonPropertyValue(final String propertyKey) {
        return Boolean.parseBoolean(childProperties.get(propertyKey).getValue());
    }

    public List<String> getListStringProperty(final PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name()).getValues();
    }

    public List<String> getListAddonStringProperty(final String propertyKey) {
        return childProperties.get(propertyKey).getValues();
    }

    public List<Integer> getListIntegerProperty(final PropertyKey propertyKey) {
        final List<String> values = childProperties.get(propertyKey.name()).getValues();
        return asIntegerList(values);
    }

    public List<Integer> getListAddonIntegerProperty(final String propertyKey) {
        final List<String> values = childProperties.get(propertyKey).getValues();
        return asIntegerList(values);
    }

    public boolean hasChildProperty(final PropertyKey propertyKey) {
        return childProperties.get(propertyKey.name()) != null;
    }

    public boolean hasChildAddonProperty(final String propertyKey) {
        return childProperties.get(propertyKey) != null;
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

    public void setProperties(final Collection<Property> properties) {
        for (final Property property : properties) {
            this.childProperties.put(property.getKey(), property);
        }
    }

    private static List<String> asStringList(final int... values) {
        final List<String> stringList = new ArrayList<String>(values.length);
        for (final Integer integer : values) {
            stringList.add(String.valueOf(integer));
        }
        return stringList;
    }

    private static List<Integer> asIntegerList(final List<String> values) {
        final List<Integer> integerList = new ArrayList<Integer>(values.size());
        for (final String value : values) {
            integerList.add(Integer.parseInt(value));
        }
        return integerList;
    }

    @Override
    public String toString() {
        return "Property [key=" + key + ", value=" + value + ", values=" + values + ", childProperties=" + childProperties + "]";
    }

}
