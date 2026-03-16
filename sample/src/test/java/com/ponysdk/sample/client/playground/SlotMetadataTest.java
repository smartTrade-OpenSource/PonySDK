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

import static org.assertj.core.api.Assertions.*;

import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests and property-based tests for SlotMetadata.
 * <p>
 * **Validates: Requirements 4.2, 8.1**
 * </p>
 */
public class SlotMetadataTest {

    // ========== Unit tests ==========

    @Test
    void isDefaultSlotReturnsTrueForEmptyName() {
        final SlotMetadata metadata = new SlotMetadata("", "desc");
        assertThat(metadata.isDefaultSlot()).isTrue();
    }

    @Test
    void isDefaultSlotReturnsTrueForNullName() {
        final SlotMetadata metadata = new SlotMetadata(null, "desc");
        assertThat(metadata.isDefaultSlot()).isTrue();
    }

    @Test
    void isDefaultSlotReturnsFalseForNonEmptyName() {
        final SlotMetadata metadata = new SlotMetadata("header", "desc");
        assertThat(metadata.isDefaultSlot()).isFalse();
    }

    @Test
    void getDisplayNameReturnsDefaultForEmptyName() {
        final SlotMetadata metadata = new SlotMetadata("", "desc");
        assertThat(metadata.getDisplayName()).isEqualTo("(default)");
    }

    @Test
    void getDisplayNameReturnsDefaultForNullName() {
        final SlotMetadata metadata = new SlotMetadata(null, "desc");
        assertThat(metadata.getDisplayName()).isEqualTo("(default)");
    }

    @Test
    void getDisplayNameReturnsNameForNonEmptyName() {
        final SlotMetadata metadata = new SlotMetadata("header", "desc");
        assertThat(metadata.getDisplayName()).isEqualTo("header");
    }

    // ========== Property-based tests ==========

    /**
     * Property 7: Default slot detection consistency.
     * <p>
     * isDefaultSlot() is true if and only if name is null or empty.
     * </p>
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 7: Default slot detection consistency")
    public void defaultSlotDetection(@ForAll("slotName") String name) {
        final SlotMetadata metadata = new SlotMetadata(name, "desc");
        final boolean expectedDefault = (name == null || name.isEmpty());
        assertThat(metadata.isDefaultSlot()).isEqualTo(expectedDefault);
    }

    /**
     * Property 8: Display name consistency.
     * <p>
     * If isDefaultSlot() then getDisplayName() == "(default)", else getDisplayName() == name.
     * </p>
     * <p>
     * **Validates: Requirements 4.2, 8.1**
     * </p>
     */
    @Property(tries = 100)
    @Tag("Feature: functional-slot-system, Property 8: Display name consistency")
    public void displayNameConsistency(@ForAll("slotName") String name) {
        final SlotMetadata metadata = new SlotMetadata(name, "desc");
        if (metadata.isDefaultSlot()) {
            assertThat(metadata.getDisplayName()).isEqualTo("(default)");
        } else {
            assertThat(metadata.getDisplayName()).isEqualTo(name);
        }
    }

    @Provide
    Arbitrary<String> slotName() {
        return Arbitraries.oneOf(
            Arbitraries.just(null),
            Arbitraries.just(""),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
        );
    }
}
