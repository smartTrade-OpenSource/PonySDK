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

import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HistoryTest extends PSuite {

    @Test
    public void test() {
        assertNull(UIContextImpl.get().getHistory().getToken());

        AtomicInteger handlerFiredCount = new AtomicInteger(0);
        PValueChangeHandler<String> handler = e -> handlerFiredCount.incrementAndGet();
        UIContextImpl.get().getHistory().addValueChangeHandler(handler);
        UIContextImpl.get().getHistory().newItem("token1", false);

        assertEquals(0, handlerFiredCount.get());
        assertEquals("token1", UIContextImpl.get().getHistory().getToken());

        UIContextImpl.get().getHistory().newItem("token2", true);
        assertEquals(1, handlerFiredCount.get());
        assertEquals("token2", UIContextImpl.get().getHistory().getToken());

        UIContextImpl.get().getHistory().newItem("token2");
        assertEquals(1, handlerFiredCount.get());
        assertEquals("token2", UIContextImpl.get().getHistory().getToken());

        UIContextImpl.get().getHistory().newItem("token3");
        assertEquals(2, handlerFiredCount.get());
        assertEquals("token3", UIContextImpl.get().getHistory().getToken());
    }

}
