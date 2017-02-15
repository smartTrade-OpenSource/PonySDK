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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum ComparatorType {
    EQ("="),
    NE("!="),
    GT(">"),
    LT("<"),
    LE("<="),
    GE(">="),
    IN("IN"),
    LIKE("LIKE"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    OR("OR");

    private static final Map<ComparatorType, String> names = new HashMap<>();
    private static final Map<String, ComparatorType> comparatorTypeByName = new HashMap<>();

    static {
        for (final ComparatorType comparatorType : ComparatorType.values()) {
            names.put(comparatorType, comparatorType.getName());
            comparatorTypeByName.put(comparatorType.getName(), comparatorType);
        }
    }

    private String name;

    ComparatorType(final String name) {
        this.name = name;
    }

    public static Collection<String> getNames() {
        return names.values();
    }

    public static ComparatorType fromName(final String name) {
        return comparatorTypeByName.get(name);
    }

    public String getName() {
        return name;
    }

}
