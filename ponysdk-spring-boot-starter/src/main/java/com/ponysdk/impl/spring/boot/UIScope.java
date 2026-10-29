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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.ponysdk.core.server.application.UIContext;

/**
 * The scope {@code "ui"}: one instance of the bean per screen (per {@link UIContext}, i.e. per browser tab), created
 * when the screen first asks for it and destroyed, with its destruction callbacks, when the screen ends. What the
 * per-session Spring context of {@code SpringApplicationManager} gave, without a context per screen.
 * <p>
 * It is the scope of an entry point that holds the state of its screen, and of what that screen's beans share:
 * {@code @Scope(UIScope.NAME)}. A singleton that needs such a bean takes it through a scoped proxy
 * ({@code @Scope(value = UIScope.NAME, proxyMode = ScopedProxyMode.TARGET_CLASS)}). Used outside a screen (a thread of the
 * application's own), it throws.
 */
public final class UIScope implements Scope {

    public static final String NAME = "ui";

    /** What the screen keeps of this scope, as an attribute of its {@link UIContext}. */
    private static final String ATTRIBUTE = UIScope.class.getName();

    @Override
    public Object get(final String name, final ObjectFactory<?> objectFactory) {
        final Beans beans = beans();
        Object bean = beans.instances.get(name);
        if (bean == null) {
            bean = objectFactory.getObject();
            beans.instances.put(name, bean);
        }
        return bean;
    }

    @Override
    public Object remove(final String name) {
        final Beans beans = beans();
        beans.destructions.remove(name);
        return beans.instances.remove(name);
    }

    @Override
    public void registerDestructionCallback(final String name, final Runnable callback) {
        beans().destructions.put(name, callback);
    }

    @Override
    public Object resolveContextualObject(final String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return String.valueOf(context().getID());
    }

    private static UIContext context() {
        final UIContext context = UIContext.get();
        if (context == null) {
            throw new IllegalStateException("No screen on this thread: a bean of the scope \"" + NAME
                    + "\" is created and used by the thread of a screen (an entry point, an event handler, UIContext.execute)");
        }
        return context;
    }

    private static Beans beans() {
        final UIContext context = context();
        Beans beans = context.getAttribute(ATTRIBUTE);
        if (beans == null) {
            beans = new Beans();
            context.setAttribute(ATTRIBUTE, beans);
            final Beans ended = beans;
            context.addContextDestroyListener(destroyed -> ended.destroy());
        }
        return beans;
    }

    /** The instances of one screen, and what to run when it ends. */
    private static final class Beans {

        final Map<String, Object> instances = new HashMap<>();
        final Map<String, Runnable> destructions = new HashMap<>();

        void destroy() {
            final List<Runnable> callbacks = new ArrayList<>(destructions.values());
            instances.clear();
            destructions.clear();
            callbacks.forEach(Runnable::run);
        }
    }
}
