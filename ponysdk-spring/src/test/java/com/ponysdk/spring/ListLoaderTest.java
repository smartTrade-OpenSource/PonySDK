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

package com.ponysdk.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

@SuppressWarnings("deprecation")
public class ListLoaderTest {

    public interface Plugin {
    }

    public static final class FirstPlugin implements Plugin {
    }

    public static final class SecondPlugin implements Plugin {
    }

    @Test
    public void collectsEveryBeanOfTheConfiguredType() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean("first", FirstPlugin.class);
            context.registerBean("second", SecondPlugin.class);
            context.registerBean("other", StringBuilder.class);
            context.refresh();

            final ListLoader loader = new ListLoader();
            loader.setType(Plugin.class);
            loader.setApplicationContext(context);

            assertEquals(2, loader.size());
            assertEquals(Set.of(context.getBean("first"), context.getBean("second")), Set.copyOf(loader));
        }
    }

    @Test
    public void staysEmptyWhenNoBeanMatches() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean("other", StringBuilder.class);
            context.refresh();

            final ListLoader loader = new ListLoader();
            loader.setType(Plugin.class);
            loader.setApplicationContext(context);

            assertTrue(loader.isEmpty());
        }
    }

}
