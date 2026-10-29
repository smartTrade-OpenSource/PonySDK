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

package com.ponysdk.sample.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.impl.spring.boot.UIScope;

/**
 * PonySDK in a Spring Boot application (docs/guide/spring-boot.md): {@code ./gradlew :sample:runSpringBoot}, then
 * http://localhost:8185/hello/. The dependency is {@code ponysdk-spring-boot-starter}, the settings are the
 * {@code ponysdk.*} properties of {@code application.properties}.
 */
@SpringBootApplication
public class SpringBootSample {

    public static void main(final String[] args) {
        SpringApplication.run(SpringBootSample.class, args);
    }

    /** An ordinary Spring bean: the entry point is given it, as any other bean would be. */
    @Service
    public static class GreetingService {

        public String greet(final String name) {
            return "Hello, " + (name.isBlank() ? "World" : name) + "!";
        }
    }

    /**
     * One entry point per browser tab: the scope {@code ui} keeps its state (here, the count) with its screen, and ends it
     * with it.
     */
    @Bean
    @Scope(UIScope.NAME)
    EntryPoint entryPoint(final GreetingService greetings) {
        return new HelloEntryPoint(greetings);
    }

    static final class HelloEntryPoint implements EntryPoint {

        private final GreetingService greetings;
        private int count;

        HelloEntryPoint(final GreetingService greetings) {
            this.greetings = greetings;
        }

        @Override
        public void start(final UIContext uiContext) {
            final PLabel label = Element.newPLabel("Hello PonySDK!");
            final PTextBox name = Element.newPTextBox();
            final PButton button = Element.newPButton("Say hello");

            // Runs on the server: the click comes over the WebSocket, the new text goes back
            button.addClickHandler(event -> label.setText(greetings.greet(name.getText()) + " (" + ++count + " in this tab)"));

            final PFlowPanel root = Element.newPFlowPanel();
            root.add(label);
            root.add(name);
            root.add(button);
            PWindow.getMain().add(root);
        }
    }
}
