/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SynchronizedWeakCacheTest {

    @Test
    public void answersTheCachedInstanceOfAnEqualKey() {
        final SynchronizedWeakCache<String> cache = new SynchronizedWeakCache<>();
        final String first = new String("value");
        assertSame(first, cache.getOrCompute(first, k -> k));

        final String equal = new String("value");
        assertSame(first, cache.getOrCompute(equal, k -> fail("must not compute a cached value")));
    }

    @Test
    public void keepsTheInstanceInsertedWhileComputing() {
        final SynchronizedWeakCache<String> cache = new SynchronizedWeakCache<>(4);
        final String winner = new String("v");
        // Another caller caches an equal value between the lookup and the insertion
        final String result = cache.getOrCompute("v", k -> {
            cache.getOrCompute(winner, w -> w);
            return new String(k);
        });
        assertSame(winner, result);
        assertSame(winner, cache.getOrCompute("v", k -> fail("cached")));
    }
}
