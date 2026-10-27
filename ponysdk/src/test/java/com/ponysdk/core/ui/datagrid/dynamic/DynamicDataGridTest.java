/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.datagrid.dynamic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.datagrid.ColumnDescriptor;
import com.ponysdk.core.ui.datagrid.impl.DefaultView;
import com.ponysdk.test.PSuite;

public class DynamicDataGridTest extends PSuite {

    public static class Bean {

        private final String name;
        private final int age;

        public Bean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public String getBroken() {
            throw new IllegalStateException("broken");
        }

        public void setNothing() {
        }

        public String format(final String pattern) {
            return pattern;
        }
    }

    private static final Comparator<Bean> BY_AGE = Comparator.comparingInt(Bean::getAge);

    /** Cell texts of a row, keyed by header caption. */
    private static Map<String, String> row(final DefaultView view, final int row, final int columnCount) {
        final Map<String, String> values = new HashMap<>();
        for (int c = 0; c < columnCount; c++) {
            values.put(((PLabel) view.getHeader(c)).getText(), ((PLabel) view.getCell(row, c)).getText());
        }
        return values;
    }

    @Test
    public void testConfiguration() {
        final Configuration<Bean> configuration = new Configuration<>(Bean.class);
        assertSame(Bean.class, configuration.getType());
        assertEquals("NAME", configuration.getCaptionTransform().apply("getName"));
        assertEquals("VALID", configuration.getCaptionTransform().apply("isValid"));
        assertEquals("VALUE", configuration.getCaptionTransform().apply("setValue"));

        final Function<String, String> transform = String::toLowerCase;
        final Predicate<Method> filter = m -> m.getName().equals("getName");
        configuration.setCaptionTransform(transform);
        configuration.setFilter(filter);
        assertSame(transform, configuration.getCaptionTransform());
        assertSame(filter, configuration.getFilter());
    }

    @Test
    public void testColumnsFromGetters() {
        final DefaultView view = new DefaultView();
        final DynamicDataGrid<Bean> grid = new DynamicDataGrid<>(new Configuration<>(Bean.class), view, b -> b.name, BY_AGE);

        final Set<String> captions = new TreeSet<>();
        grid.getColumns().forEach(c -> captions.add(((PLabel) c.getHeaderRenderer().render()).getText()));
        // Synthetic methods added by a coverage agent are not part of the bean
        captions.removeIf(caption -> caption.startsWith("$"));
        assertEquals(Set.of("AGE", "BROKEN", "NAME"), captions);

        grid.addData(new Bean("bob", 40));
        grid.addData(new Bean("alice", 30));
        final int columnCount = grid.getColumns().size();
        final Map<String, String> first = row(view, 0, columnCount);
        assertEquals("alice", first.get("NAME"));
        assertEquals("30", first.get("AGE"));
        // A getter that throws is rendered as "x"
        assertEquals("x", first.get("BROKEN"));
        assertEquals("bob", row(view, 1, columnCount).get("NAME"));
    }

    @Test
    public void testCustomConfiguration() {
        final Configuration<Bean> configuration = new Configuration<>(Bean.class);
        configuration.setFilter(m -> m.getName().equals("getName"));
        configuration.setCaptionTransform(name -> "Caption " + name);

        final DynamicDataGrid<Bean> grid = new DynamicDataGrid<>(configuration, BY_AGE);
        assertEquals(1, grid.getColumns().size());
        final ColumnDescriptor<Bean> column = grid.getColumns().get(0);
        assertEquals("Caption getName", ((PLabel) column.getHeaderRenderer().render()).getText());

        final DynamicDataGrid<Bean> byKey = new DynamicDataGrid<>(configuration, (Function<Bean, ?>) b -> b.name, BY_AGE);
        byKey.addData(new Bean("a", 1));
        byKey.addData(new Bean("a", 1));
        assertEquals(1, byKey.getData().size());
    }

    @Test
    public void testNaturalOrderConstructors() {
        final Configuration<String> configuration = new Configuration<>(String.class);
        configuration.setFilter(m -> m.getName().equals("toUpperCase") && m.getParameterCount() == 0);

        final DynamicDataGrid<String> grid = new DynamicDataGrid<>(configuration);
        grid.addData("b");
        grid.addData("a");
        assertEquals("[a, b]", grid.getData().toString());
        final PFlexTable table = (PFlexTable) grid.asWidget();
        assertEquals("TOUPPERCASE", ((PLabel) table.getWidget(0, 0)).getText());
        assertEquals("A", ((PLabel) table.getWidget(1, 0)).getText());

        final DynamicDataGrid<String> byLength = new DynamicDataGrid<>(configuration, (Function<String, ?>) String::length);
        byLength.addData("aa");
        assertTrue(byLength.getData().contains("aa"));
    }
}
