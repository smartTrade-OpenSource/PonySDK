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

package com.ponysdk.core.ui.datagrid.dynamic;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

public class Configuration<DataType> {

    private static final Function<String, String> DEFAULT_TRANSFORM = s -> s.replaceAll("^get|^set|^is", "").toUpperCase();
    private static final Predicate<Method> DEFAULT_FILTER = m -> m.getReturnType() != Void.TYPE && 0 == m.getParameterCount();

    private final Class<DataType> type;

    private Function<String, String> captionTransform;

    private Predicate<Method> filter;

    public Configuration(final Class<DataType> type) {
        this.type = type;
        this.captionTransform = DEFAULT_TRANSFORM;
        this.filter = DEFAULT_FILTER;
    }

    public Class<DataType> getType() {
        return type;
    }

    public Function<String, String> getCaptionTransform() {
        return captionTransform;
    }

    public void setCaptionTransform(final Function<String, String> captionTransform) {
        this.captionTransform = captionTransform;
    }

    public Predicate<Method> getFilter() {
        return filter;
    }

    public void setFilter(final Predicate<Method> filter) {
        this.filter = filter;
    }

}