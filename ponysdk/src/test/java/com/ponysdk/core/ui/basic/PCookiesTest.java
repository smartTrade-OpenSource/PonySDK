/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PCookiesTest extends PSuite {

    @Test
    public void testInit() {
        final PCookies cookies = UIContext.get().getCookies();
        assertNotNull(cookies);
        assertNull(cookies.get());

        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        cookies.addInitializeListener(e -> handlerFiredCount.incrementAndGet());
        assertEquals(0, handlerFiredCount.get());
        assertEquals(false, cookies.isInitialized());

        PEmulator.cookies("cookie=value");

        assertEquals(1, handlerFiredCount.get());
        assertEquals(true, cookies.isInitialized());
        assertEquals("cookie=value", cookies.get());

        PEmulator.cookies("cookie=value2");
        assertEquals(1, handlerFiredCount.get());
        assertEquals(true, cookies.isInitialized());
        assertEquals("cookie=value2", cookies.get());
    }

    @Test
    public void testSetCookies() {
        UIContext.get().getCookies().set("name=value");
        assertEquals("name=value", UIContext.get().getCookies().get());

        UIContext.get().getCookies().set("name=value2");
        assertEquals("name=value2", UIContext.get().getCookies().get());
    }

    @Test
    public void testEraseCookies() {
        UIContext.get().getCookies().set(null);
        assertNull(UIContext.get().getCookies().get());

        UIContext.get().getCookies().set("name=value");
        assertEquals("name=value", UIContext.get().getCookies().get());

        UIContext.get().getCookies().set(null);
        assertNull(UIContext.get().getCookies().get());
    }

    @Test
    public void testValueChangeHandler() {
        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        UIContext.get().getCookies().addValueChangeHandler(e -> handlerFiredCount.incrementAndGet());
        assertEquals(0, handlerFiredCount.get());
        UIContext.get().getCookies().set("cookie=value");
        assertEquals(0, handlerFiredCount.get());

        PEmulator.cookies("cookie=value2");
        assertEquals(1, handlerFiredCount.get());

        PEmulator.cookies("cookie=value2");
        assertEquals(1, handlerFiredCount.get());

        PEmulator.cookies("cookie=value3");
        assertEquals(2, handlerFiredCount.get());

        UIContext.get().getCookies().set("cookie=value");
        PEmulator.cookies("cookie=value");
        assertEquals(2, handlerFiredCount.get());
    }

}
