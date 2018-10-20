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

package com.ponysdk.core.ui.datagrid;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import com.ponysdk.core.ui.datagrid.ColumnDescriptor;
import com.ponysdk.core.ui.datagrid.DataGrid;
import com.ponysdk.core.ui.datagrid.View;
import com.ponysdk.core.ui.datagrid.impl.DefaultView;

public class ColumnHelper {

    private static final Function<String, String> DEFAULT_TRANSFORM = s -> s.replaceAll("^get|^set|^is", "").toUpperCase();
    private static final Predicate<Method> DEFAULT_FILTER = m -> m.getReturnType() != Void.TYPE && 0 == m.getParameterCount();


    public static <DataType> Collection<ColumnDescriptor<DataType>> buildColumnDescriptor(Class<DataType> type) {
        return buildColumnDescriptor(type, DEFAULT_FILTER, DEFAULT_TRANSFORM);
    }

    public static <DataType> Collection<ColumnDescriptor<DataType>> buildColumnDescriptor(Class<DataType> type, Predicate<Method> filter, Function<String, String> captionTransform) {
        List<ColumnDescriptor<DataType>> descriptors = new ArrayList<>();
        Arrays.stream(type.getDeclaredMethods()).filter(filter).forEach(method -> {
            method.setAccessible(true);
            descriptors.add(ColumnDescriptor.newDefault(captionTransform.apply(method.getName()), data -> invoke(method, data)));
        });
        return descriptors;
    }

    private static String invoke(final Method method, final Object data) {
        try {
            return method.invoke(data).toString();
        } catch (final Exception e) {
            return "x";
        }
    }
}
