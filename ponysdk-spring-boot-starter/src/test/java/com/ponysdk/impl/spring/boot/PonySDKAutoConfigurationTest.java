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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.util.compression.DeflaterPool;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.servlet.HealthServlet;
import com.ponysdk.core.ui.main.EntryPoint;

import jakarta.servlet.ServletContext;

/** What the auto-configuration makes of the properties, and how an application's own beans take its place. */
class PonySDKAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PonySDKAutoConfiguration.class))
        .withBean(EntryPoint.class, () -> uiContext -> {
        });

    @Test
    void withoutPropertiesTheConfigurationKeepsPonySDKsDefaults() {
        runner.run(context -> {
            final ApplicationConfiguration actual = context.getBean(ApplicationConfiguration.class);
            final ApplicationConfiguration defaults = new ApplicationConfiguration();
            assertEquals(defaults.getApplicationContextName(), actual.getApplicationContextName());
            assertEquals(defaults.getReconnectionTimeoutMs(), actual.getReconnectionTimeoutMs());
            assertEquals(defaults.isDebugMode(), actual.isDebugMode());
            assertEquals(defaults.isContentSecurityPolicyEnabled(), actual.isContentSecurityPolicyEnabled());
            assertEquals(defaults.getScriptSources(), actual.getScriptSources());
            assertEquals(defaults.getServedResourceExtensions(), actual.getServedResourceExtensions());
            assertEquals(defaults.getWsAllowedOrigins(), actual.getWsAllowedOrigins());
            assertEquals(defaults.isWsOriginCheckEnabled(), actual.isWsOriginCheckEnabled());
            assertEquals(defaults.getWsMaxInboundMessageSize(), actual.getWsMaxInboundMessageSize());
            assertEquals(defaults.getWsInputBufferSize(), actual.getWsInputBufferSize());
            assertEquals(defaults.getWsIdleTimeoutMs(), actual.getWsIdleTimeoutMs());
            assertEquals(defaults.getWsSendTimeoutMs(), actual.getWsSendTimeoutMs());
            assertEquals(defaults.isWsPermessageDeflateEnabled(), actual.isWsPermessageDeflateEnabled());
            assertEquals(defaults.getWsDeflateLevel(), actual.getWsDeflateLevel());
            assertEquals(defaults.isStringDictionaryEnabled(), actual.isStringDictionaryEnabled());
            assertEquals(defaults.isStringDictionaryPersistenceEnabled(), actual.isStringDictionaryPersistenceEnabled());
            assertEquals(defaults.getStringDictionaryPersistPath(), actual.getStringDictionaryPersistPath());
            assertEquals(defaults.isLegacyClassNames(), actual.isLegacyClassNames());
            assertEquals(defaults.getLanguage(), actual.getLanguage());
            assertEquals(defaults.getJavascript(), actual.getJavascript());
            assertEquals(defaults.getStyle(), actual.getStyle());
        });
    }

    @Test
    void everyPropertyReachesTheConfiguration() {
        runner.withPropertyValues(
            "ponysdk.application-id=shop",
            "ponysdk.application-name=Shop",
            "ponysdk.application-description=The shop",
            "ponysdk.application-context-name=shop",
            "ponysdk.reconnection-timeout=30s",
            "ponysdk.debug-mode=true",
            "ponysdk.legacy-class-names=true",
            "ponysdk.language=fr",
            "ponysdk.meta=name=\"viewport\" content=\"width=device-width\"",
            "ponysdk.javascript[0]=script/b.js",
            "ponysdk.javascript[1]=script/a.js",
            "ponysdk.style.first=style/1.css",
            "ponysdk.style.second=style/2.css",
            "ponysdk.wc-shared-sheets=style/shared.css",
            "ponysdk.served-resource-extensions=json,.txt",
            "ponysdk.content-security-policy.enabled=false",
            "ponysdk.content-security-policy.script-sources=https://cdn.example.com",
            "ponysdk.websocket.allowed-origins=https://portal.example.com,https://other.example.com",
            "ponysdk.websocket.origin-check=false",
            "ponysdk.websocket.max-message-size=64KB",
            "ponysdk.websocket.input-buffer-size=2KB",
            "ponysdk.websocket.idle-timeout=5m",
            "ponysdk.websocket.send-timeout=20s",
            "ponysdk.websocket.permessage-deflate=false",
            "ponysdk.websocket.deflate-level=9",
            "ponysdk.string-dictionary.enabled=false",
            "ponysdk.string-dictionary.persistence=false",
            "ponysdk.string-dictionary.persist-path=/var/lib/shop").run(context -> {
                final ApplicationConfiguration configuration = context.getBean(ApplicationConfiguration.class);
                assertEquals("shop", configuration.getApplicationID());
                assertEquals("Shop", configuration.getApplicationName());
                assertEquals("The shop", configuration.getApplicationDescription());
                assertEquals("shop", configuration.getApplicationContextName());
                assertEquals(30_000, configuration.getReconnectionTimeoutMs());
                assertTrue(configuration.isDebugMode());
                assertTrue(configuration.isLegacyClassNames());
                assertEquals("fr", configuration.getLanguage());
                assertEquals(Set.of("name=\"viewport\" content=\"width=device-width\""), configuration.getMeta());
                assertEquals(List.of("script/b.js", "script/a.js"), List.copyOf(configuration.getJavascript()), "load order kept");
                final Map<String, String> styles = new LinkedHashMap<>();
                styles.put("first", "style/1.css");
                styles.put("second", "style/2.css");
                assertEquals(styles, configuration.getStyle());
                assertEquals(List.copyOf(styles.keySet()), List.copyOf(configuration.getStyle().keySet()), "order kept");
                assertEquals(Set.of("style/shared.css"), configuration.getWcSharedSheets());
                assertTrue(configuration.getServedResourceExtensions().containsAll(Set.of("json", "txt", "js")));
                assertFalse(configuration.isContentSecurityPolicyEnabled());
                assertEquals(Set.of("https://cdn.example.com"), configuration.getScriptSources());
                assertEquals(Set.of("https://portal.example.com", "https://other.example.com"), configuration.getWsAllowedOrigins());
                assertFalse(configuration.isWsOriginCheckEnabled());
                assertEquals(64 * 1024, configuration.getWsMaxInboundMessageSize());
                assertEquals(2 * 1024, configuration.getWsInputBufferSize());
                assertEquals(300_000, configuration.getWsIdleTimeoutMs());
                assertEquals(20_000, configuration.getWsSendTimeoutMs());
                assertFalse(configuration.isWsPermessageDeflateEnabled());
                assertEquals(9, configuration.getWsDeflateLevel());
                assertFalse(configuration.isStringDictionaryEnabled());
                assertFalse(configuration.isStringDictionaryPersistenceEnabled());
                assertEquals("/var/lib/shop", configuration.getStringDictionaryPersistPath());
            });
    }

    @Test
    void theServletsAreMappedUnderTheApplicationsPath() {
        runner.withPropertyValues("ponysdk.application-context-name=shop").run(context -> {
            assertEquals(Set.of("/shop/*"), urlMappings(context.getBean("ponySDKBootstrapServlet", ServletRegistrationBean.class)));
            assertEquals(Set.of("/shop/ws/*"), urlMappings(context.getBean("ponySDKWebSocketServlet", ServletRegistrationBean.class)));
            assertEquals(Set.of("/shop/ajax"), urlMappings(context.getBean("ponySDKAjaxServlet", ServletRegistrationBean.class)));
            assertEquals(Set.of("/shop/stream"), urlMappings(context.getBean("ponySDKStreamServlet", ServletRegistrationBean.class)));
            assertEquals(Set.of("/shop/health"), urlMappings(context.getBean("ponySDKHealthServlet", ServletRegistrationBean.class)));
            assertEquals(Set.of("/shop/*"), context.getBean("ponySDKCrossOriginFilter", FilterRegistrationBean.class).getUrlPatterns());
        });
    }

    private static Set<String> urlMappings(final ServletRegistrationBean<?> registration) {
        return Set.copyOf(registration.getUrlMappings());
    }

    @Test
    void theEntryPointOfEachScreenComesFromTheContext() {
        runner.run(context -> {
            final ApplicationManager manager = context.getBean(ApplicationManager.class);
            assertInstanceOf(BootApplicationManager.class, manager);
            assertSame(context.getBean(ApplicationConfiguration.class), manager.getConfiguration());
            manager.start(); // nothing to prepare
            assertSame(context.getBean(EntryPoint.class), ((BootApplicationManager) manager).initializeEntryPoint());
        });
    }

    /** The key reaches the configuration: one of 32 bytes starts, one of another length is refused at start. */
    @Test
    void theStateTransferKeyIsDecodedFromBase64() {
        runner.withPropertyValues("ponysdk.state-transfer-key=" + java.util.Base64.getEncoder().encodeToString(new byte[32]))
            .run(context -> assertNull(context.getStartupFailure()));
        runner.withPropertyValues("ponysdk.state-transfer-key=" + java.util.Base64.getEncoder().encodeToString(new byte[16]))
            .run(context -> assertInstanceOf(IllegalArgumentException.class, rootCause(context.getStartupFailure())));
    }

    private static Throwable rootCause(final Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause;
    }

    @Test
    void theUIScopeIsRegistered() {
        runner.run(context -> assertInstanceOf(UIScope.class,
            ((ConfigurableBeanFactory) context.getBeanFactory()).getRegisteredScope(UIScope.NAME)));
    }

    @Test
    void theDrainFollowsTheServersGracefulShutdown() {
        runner.run(context -> {
            assertEquals(PonySDKDrain.PHASE, context.getBean(PonySDKDrain.class).getPhase());
            assertTrue(PonySDKDrain.PHASE > org.springframework.boot.web.server.context.WebServerApplicationContext.GRACEFUL_SHUTDOWN_PHASE);
        });
        runner.withPropertyValues("server.shutdown=graceful").run(context -> assertTrue(context.containsBean("ponySDKDrain")));
        runner.withPropertyValues("server.shutdown=immediate").run(context -> assertFalse(context.containsBean("ponySDKDrain")));
    }

    @Test
    void theWebSocketsFrameCompressionIsSetOnTheServletContext() throws Exception {
        runner.withPropertyValues("ponysdk.websocket.deflate-level=7").run(context -> {
            final ServletContext servletContext = Mockito.mock(ServletContext.class);
            context.getBean("ponySDKWebSocketDeflater", ServletContextInitializer.class).onStartup(servletContext);
            Mockito.verify(servletContext).setAttribute(ArgumentMatchers.eq(PonySDKAutoConfiguration.WEBSOCKET_DEFLATER_POOL_ATTRIBUTE),
                ArgumentMatchers.any(DeflaterPool.class));
        });
        runner.withPropertyValues("ponysdk.websocket.deflate-level=-1").run(context -> {
            final ServletContext servletContext = Mockito.mock(ServletContext.class);
            context.getBean("ponySDKWebSocketDeflater", ServletContextInitializer.class).onStartup(servletContext);
            Mockito.verifyNoInteractions(servletContext); // Jetty's default
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyJettyCanServeTheWebSocket() {
        runner.run(context -> {
            final WebServerFactoryCustomizer<ServletWebServerFactory> customizer = context.getBean("ponySDKJettyOnly",
                WebServerFactoryCustomizer.class);
            customizer.customize(new JettyServletWebServerFactory());

            final ServletWebServerFactory other = Mockito.mock(ServletWebServerFactory.class);
            final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> customizer.customize(other));
            assertTrue(failure.getMessage().contains("PonySDK needs Jetty"), failure.getMessage());
        });
    }

    @Test
    void theHealthIndicatorIsThereWithTheActuatorAndUnlessSwitchedOff() {
        runner.run(context -> assertInstanceOf(PonySDKHealthIndicator.class, context.getBean("ponysdkHealthIndicator")));
        runner.withPropertyValues("management.health.ponysdk.enabled=false")
            .run(context -> assertFalse(context.containsBean("ponysdkHealthIndicator")));
        runner.withClassLoader(new FilteredClassLoader(HealthIndicator.class)).run(context -> {
            assertFalse(context.containsBean("ponysdkHealthIndicator"));
            assertTrue(context.containsBean("ponySDKBootstrapServlet"), "the rest does not need the actuator");
        });
    }

    @Test
    void nothingIsRegisteredOutsideAServletApplication() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PonySDKAutoConfiguration.class))
            .run(context -> assertFalse(context.containsBean("ponySDKBootstrapServlet")));
    }

    @Test
    void anApplicationsOwnBeansTakeThePlaceOfTheStarters() {
        runner.withUserConfiguration(Own.class).run(context -> {
            assertSame(Own.CONFIGURATION, context.getBean(ApplicationConfiguration.class));
            assertSame(Own.MANAGER, context.getBean(ApplicationManager.class));
            assertSame(Own.HEALTH, context.getBean("ponySDKHealthServlet"));
            assertEquals(1, context.getBeansOfType(ApplicationConfiguration.class).size());
            assertEquals(1, context.getBeansOfType(ApplicationManager.class).size());
            assertFalse(context.containsBean("ponySDKApplicationConfiguration"));
            assertFalse(context.containsBean("ponySDKApplicationManager"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class Own {

        static final ApplicationConfiguration CONFIGURATION = new ApplicationConfiguration();
        static final ServletRegistrationBean<HealthServlet> HEALTH = new ServletRegistrationBean<>(new HealthServlet(), "/status");
        static final ApplicationManager MANAGER = new ApplicationManager() {

            @Override
            protected EntryPoint initializeEntryPoint() {
                return uiContext -> {
                };
            }

            @Override
            public void start() {
            }
        };

        @Bean
        ApplicationConfiguration configuration() {
            return CONFIGURATION;
        }

        @Bean
        ApplicationManager manager() {
            return MANAGER;
        }

        @Bean
        ServletRegistrationBean<HealthServlet> ponySDKHealthServlet() {
            return HEALTH;
        }
    }
}
