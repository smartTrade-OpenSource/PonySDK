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

package com.ponysdk.sample.client.playground;

import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based and unit tests for {@link PropertyCategory}.
 */
class PropertyCategoryTest {

    private enum TestVariant { PRIMARY, SECONDARY }

    private static final Map<Class<?>, PropertyCategory> EXPECTED_CATEGORIES = Map.of(
        boolean.class, PropertyCategory.BEHAVIOR,
        Boolean.class, PropertyCategory.BEHAVIOR,
        String.class, PropertyCategory.CONTENT,
        int.class, PropertyCategory.LAYOUT,
        Integer.class, PropertyCategory.LAYOUT,
        long.class, PropertyCategory.LAYOUT,
        Long.class, PropertyCategory.LAYOUT
    );

    // Feature: property-label-improvements, Property 5: Category assignment consistency
    @Property(tries = 100)
    void categoryAssignment_supportedTypes(@ForAll("typedParameterInfo") ParameterInfo param) {
        final PropertyCategory result = PropertyCategory.fromParameterInfo(param);
        assertThat(result).isNotNull();

        final Class<?> type = param.type();
        if (EXPECTED_CATEGORIES.containsKey(type)) {
            assertThat(result).isEqualTo(EXPECTED_CATEGORIES.get(type));
        } else if (type.isEnum()) {
            assertThat(result).isEqualTo(PropertyCategory.APPEARANCE);
        } else {
            assertThat(result).isEqualTo(PropertyCategory.OTHER);
        }
    }

    // Feature: property-label-improvements, Property 6: Category ordering invariant
    @Property(tries = 100)
    void categoryOrdering_sortedListRespectsOrder(@ForAll("categoryList") List<PropertyCategory> categories) {
        final List<PropertyCategory> sorted = new ArrayList<>(categories);
        sorted.sort(Comparator.comparingInt(Enum::ordinal));

        for (int i = 1; i < sorted.size(); i++) {
            assertThat(sorted.get(i).ordinal())
                .as("Category at index %d should have ordinal >= previous", i)
                .isGreaterThanOrEqualTo(sorted.get(i - 1).ordinal());
        }
    }

    @Provide
    Arbitrary<ParameterInfo> typedParameterInfo() {
        final List<Class<?>> types = List.of(
            String.class, boolean.class, Boolean.class,
            int.class, Integer.class, long.class, Long.class,
            TestVariant.class, Object.class, List.class
        );
        return Arbitraries.of(types)
            .map(type -> new ParameterInfo("param", type, false, null));
    }

    @Provide
    Arbitrary<List<PropertyCategory>> categoryList() {
        return Arbitraries.of(PropertyCategory.values())
            .list().ofMinSize(1).ofMaxSize(20);
    }

    // ── Unit tests ──

    @Test
    void null_throwsException() {
        assertThatThrownBy(() -> PropertyCategory.fromParameterInfo(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void displayNames_areCorrect() {
        assertThat(PropertyCategory.APPEARANCE.getDisplayName()).isEqualTo("Appearance");
        assertThat(PropertyCategory.CONTENT.getDisplayName()).isEqualTo("Content");
        assertThat(PropertyCategory.LAYOUT.getDisplayName()).isEqualTo("Layout");
        assertThat(PropertyCategory.BEHAVIOR.getDisplayName()).isEqualTo("Behavior");
        assertThat(PropertyCategory.OTHER.getDisplayName()).isEqualTo("Other");
    }

    @Test
    void ordinalOrder_isCorrect() {
        assertThat(PropertyCategory.APPEARANCE.ordinal()).isLessThan(PropertyCategory.CONTENT.ordinal());
        assertThat(PropertyCategory.CONTENT.ordinal()).isLessThan(PropertyCategory.LAYOUT.ordinal());
        assertThat(PropertyCategory.LAYOUT.ordinal()).isLessThan(PropertyCategory.BEHAVIOR.ordinal());
        assertThat(PropertyCategory.BEHAVIOR.ordinal()).isLessThan(PropertyCategory.OTHER.ordinal());
    }

    @Test
    void unknownType_returnsOther() {
        final ParameterInfo param = new ParameterInfo("param", Object.class, false, null);
        assertThat(PropertyCategory.fromParameterInfo(param)).isEqualTo(PropertyCategory.OTHER);
    }
}
