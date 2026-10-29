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

package com.ponysdk.core.ui.binder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.ponysdk.test.PSuite;

/**
 * The binder where Bean Validation is not there: its API missing from the classpath, or present with no provider
 * (a library brings the API along, and nobody brought Hibernate Validator). The binder's classes are loaded again by
 * a loader that hides what the application would not have.
 */
public class BinderWithoutBeanValidationTest extends PSuite {

    private static final String PREFIX = "com.ponysdk.core.ui.binder.";
    /** Classes with a code source: the coverage agent ignores the others. */
    private static final java.security.ProtectionDomain DOMAIN = Binder.class.getProtectionDomain();
    private static final String PROVIDERS = "META-INF/services/jakarta.validation.spi.ValidationProvider";

    /** Defines the binder's classes (and optionally Jakarta Validation's) itself, the others come from the parent. */
    private static final class Isolation extends ClassLoader {

        private final boolean hideApi;

        Isolation(final ClassLoader parent, final boolean hideApi) {
            super(parent);
            this.hideApi = hideApi;
        }

        boolean isLoaded(final String name) {
            return findLoadedClass(name) != null;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (hideApi && name.startsWith("jakarta.validation.")) throw new ClassNotFoundException(name);
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null && (name.startsWith(PREFIX) || name.startsWith("jakarta.validation."))) loaded = define(name);
                if (loaded == null) return super.loadClass(name, resolve);
                return loaded;
            }
        }

        private Class<?> define(final String name) throws ClassNotFoundException {
            try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (in == null) throw new ClassNotFoundException(name);
                final byte[] bytes = in.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length, DOMAIN);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Enumeration<java.net.URL> getResources(final String name) throws IOException {
            return PROVIDERS.equals(name) ? Collections.emptyEnumeration() : super.getResources(name);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> run(final Isolation loader) throws ReflectiveOperationException {
        final Thread thread = Thread.currentThread();
        final ClassLoader before = thread.getContextClassLoader();
        thread.setContextClassLoader(loader); // where Bean Validation looks for its provider
        try {
            final Supplier<List<String>> scenario =
                (Supplier<List<String>>) loader.loadClass(PREFIX + "BinderScenario").getConstructor().newInstance();
            return scenario.get();
        } finally {
            thread.setContextClassLoader(before);
        }
    }

    @Test
    public void theBinderWorksWhenTheBeanValidationApiIsNotOnTheClasspath() throws ReflectiveOperationException {
        final Isolation loader = new Isolation(getClass().getClassLoader(), true);

        assertEquals(List.of("true", "null", "abcdef", "true"), run(loader)); // "abcdef" is not refused
        assertFalse(loader.isLoaded(PREFIX + "BeanValidation"), "the class that needs the API was never loaded");
        assertFalse(loader.isLoaded("jakarta.validation.Validation"));
    }

    @Test
    public void theBinderWorksWhenTheApiHasNoProvider() throws ReflectiveOperationException {
        final Isolation loader = new Isolation(getClass().getClassLoader(), false);

        assertEquals(List.of("true", "null", "abcdef", "true"), run(loader)); // the constraint is not applied
        assertTrue(loader.isLoaded(PREFIX + "BeanValidation"), "the API is there, its provider is not");
    }
}
