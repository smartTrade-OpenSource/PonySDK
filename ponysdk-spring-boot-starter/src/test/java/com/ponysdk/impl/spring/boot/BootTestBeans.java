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

package com.ponysdk.impl.spring.boot;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

/** The beans of a Boot application of one screen: a label, and a button that changes it. */
@Configuration(proxyBeanMethods = false)
public class BootTestBeans {

    /** A bean the entry point is given: what the screens share. */
    public static final class Greeting {

        public String text() {
            return "Hello";
        }
    }

    /** The entry points created and destroyed: one per screen, destroyed with it. */
    static final AtomicInteger CREATED = new AtomicInteger();
    static final AtomicInteger DESTROYED = new AtomicInteger();

    public static final class HelloEntryPoint implements EntryPoint, DisposableBean {

        private final Greeting greeting;

        HelloEntryPoint(final Greeting greeting) {
            this.greeting = greeting;
            CREATED.incrementAndGet();
        }

        @Override
        public void destroy() {
            DESTROYED.incrementAndGet();
        }

        @Override
        public void start(final UIContext uiContext) {
            final PLabel label = Element.newPLabel(greeting.text());
            final PButton button = Element.newPButton("Go");
            button.addClickHandler(event -> label.setText("Clicked"));
            PWindow.getMain().add(label);
            PWindow.getMain().add(button);
        }
    }

    @Bean
    Greeting greeting() {
        return new Greeting();
    }

    @Bean
    @Scope(UIScope.NAME)
    EntryPoint entryPoint(final Greeting greeting) {
        return new HelloEntryPoint(greeting);
    }
}
