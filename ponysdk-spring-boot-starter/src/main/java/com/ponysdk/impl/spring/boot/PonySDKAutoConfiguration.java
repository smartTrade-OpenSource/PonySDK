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

import java.time.Duration;

import org.eclipse.jetty.util.compression.CompressionPool;
import org.eclipse.jetty.util.compression.DeflaterPool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import com.ponysdk.core.model.MappingPath;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.servlet.AjaxServlet;
import com.ponysdk.core.server.servlet.CrossOriginFilter;
import com.ponysdk.core.server.servlet.HealthServlet;
import com.ponysdk.core.server.servlet.StreamServiceServlet;
import com.ponysdk.core.server.websocket.WebSocketServlet;
import com.ponysdk.core.ui.main.EntryPoint;

/**
 * PonySDK in a Spring Boot application (ADR-0023): the servlets of {@code PonySDKServer} registered in Boot's own Jetty
 * under {@code /<ponysdk.application-context-name>/}, configured from the {@code ponysdk.*} properties, the entry point
 * taken from the application's context, and a graceful shutdown that moves the live pages to the other servers first.
 * Every bean yields to one of the application's own.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ EntryPoint.class, ServletRegistrationBean.class })
@EnableConfigurationProperties(PonySDKProperties.class)
public class PonySDKAutoConfiguration {

    /** Servlet-context attribute Jetty reads its WebSocket DeflaterPool from (the same as PonySDKServer's). */
    static final String WEBSOCKET_DEFLATER_POOL_ATTRIBUTE = "jetty.websocket.deflater";

    @Bean
    @ConditionalOnMissingBean
    ApplicationConfiguration ponySDKApplicationConfiguration(final PonySDKProperties properties) {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        final PropertyMapper map = PropertyMapper.get();
        map.from(properties::getApplicationId).to(configuration::setApplicationID);
        map.from(properties::getApplicationName).to(configuration::setApplicationName);
        map.from(properties::getApplicationDescription).to(configuration::setApplicationDescription);
        map.from(properties::getApplicationContextName).to(configuration::setApplicationContextName);
        map.from(properties::getReconnectionTimeout).as(Duration::toMillis).to(configuration::setReconnectionTimeoutMs);
        map.from(properties::getDebugMode).to(configuration::setDebugMode);
        map.from(properties::getStateTransferKey).as(java.util.Base64.getDecoder()::decode).to(configuration::setStateTransferKey);
        map.from(properties::getLegacyClassNames).to(configuration::setLegacyClassNames);
        map.from(properties::getLanguage).to(configuration::setLanguage);
        map.from(properties::getMeta).to(configuration::setMeta);
        map.from(properties::getJavascript).to(configuration::setJavascript);
        map.from(properties::getStyle).to(configuration::setStyle);
        map.from(properties::getWcSharedSheets).to(configuration::setWcSharedSheets);
        map.from(properties::getServedResourceExtensions)
            .to(extensions -> configuration.addServedResourceExtensions(extensions.toArray(String[]::new)));

        final PonySDKProperties.ContentSecurityPolicy policy = properties.getContentSecurityPolicy();
        map.from(policy::getEnabled).to(configuration::setContentSecurityPolicyEnabled);
        map.from(policy::getScriptSources).to(sources -> configuration.addScriptSources(sources.toArray(String[]::new)));
        map.from(policy::getWebAssembly).to(configuration::setWebAssemblyEnabled);

        final PonySDKProperties.WebSocket websocket = properties.getWebsocket();
        map.from(websocket::getAllowedOrigins).to(configuration::setWsAllowedOrigins);
        map.from(websocket::getOriginCheck).to(configuration::setWsOriginCheckEnabled);
        map.from(websocket::getMaxMessageSize).as(DataSize::toBytes).as(Long::intValue).to(configuration::setWsMaxInboundMessageSize);
        map.from(websocket::getInputBufferSize).as(DataSize::toBytes).as(Long::intValue).to(configuration::setWsInputBufferSize);
        map.from(websocket::getIdleTimeout).as(Duration::toMillis).to(configuration::setWsIdleTimeoutMs);
        map.from(websocket::getSendTimeout).as(Duration::toMillis).to(configuration::setWsSendTimeoutMs);
        map.from(websocket::getPermessageDeflate).to(configuration::setWsPermessageDeflateEnabled);
        map.from(websocket::getDeflateLevel).to(configuration::setWsDeflateLevel);

        final PonySDKProperties.StringDictionary dictionary = properties.getStringDictionary();
        map.from(dictionary::getEnabled).to(configuration::setStringDictionaryEnabled);
        map.from(dictionary::getPersistence).to(configuration::setStringDictionaryPersistenceEnabled);
        map.from(dictionary::getPersistPath).to(configuration::setStringDictionaryPersistPath);
        return configuration;
    }

    /** Starts each screen with the application's {@link EntryPoint} bean. */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    ApplicationManager ponySDKApplicationManager(final ApplicationConfiguration configuration,
                                                 final ObjectProvider<EntryPoint> entryPoints) {
        return new BootApplicationManager(configuration, entryPoints);
    }

    /** The scope {@link UIScope#NAME}; static, as the bean factory post-processor it is. */
    @Bean
    @ConditionalOnMissingBean(name = "ponySDKUIScopeConfigurer")
    static CustomScopeConfigurer ponySDKUIScopeConfigurer() {
        final CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.addScope(UIScope.NAME, new UIScope());
        return configurer;
    }

    @Bean
    @ConditionalOnMissingBean(name = "ponySDKBootstrapServlet")
    ServletRegistrationBean<PonySDKBootstrapServlet> ponySDKBootstrapServlet(final ApplicationConfiguration configuration) {
        return new ServletRegistrationBean<>(new PonySDKBootstrapServlet(configuration), prefix(configuration) + "/*");
    }

    @Bean
    @ConditionalOnMissingBean(name = "ponySDKWebSocketServlet")
    ServletRegistrationBean<WebSocketServlet> ponySDKWebSocketServlet(final ApplicationConfiguration configuration,
                                                                      final ApplicationManager applicationManager) {
        final ServletRegistrationBean<WebSocketServlet> registration = new ServletRegistrationBean<>(
            new WebSocketServlet(applicationManager), prefix(configuration) + "/" + MappingPath.WEBSOCKET + "/*");
        registration.setLoadOnStartup(1); // a server that cannot hold the WebSocket fails at start, not at the first page
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(name = "ponySDKAjaxServlet")
    ServletRegistrationBean<AjaxServlet> ponySDKAjaxServlet(final ApplicationConfiguration configuration) {
        return new ServletRegistrationBean<>(new AjaxServlet(), prefix(configuration) + "/" + MappingPath.AJAX);
    }

    @Bean
    @ConditionalOnMissingBean(name = "ponySDKStreamServlet")
    ServletRegistrationBean<StreamServiceServlet> ponySDKStreamServlet(final ApplicationConfiguration configuration) {
        return new ServletRegistrationBean<>(new StreamServiceServlet(), prefix(configuration) + "/" + MappingPath.STREAM);
    }

    /** A load balancer's health check: 200 until the drain, then 503. */
    @Bean
    @ConditionalOnMissingBean(name = "ponySDKHealthServlet")
    ServletRegistrationBean<HealthServlet> ponySDKHealthServlet(final ApplicationConfiguration configuration) {
        return new ServletRegistrationBean<>(new HealthServlet(), prefix(configuration) + "/health");
    }

    /** The pages allowed to embed the application in a {@code <pony-app>} element (ADR-0008). */
    @Bean
    @ConditionalOnMissingBean(name = "ponySDKCrossOriginFilter")
    FilterRegistrationBean<CrossOriginFilter> ponySDKCrossOriginFilter(final ApplicationConfiguration configuration) {
        final FilterRegistrationBean<CrossOriginFilter> registration = new FilterRegistrationBean<>(
            new CrossOriginFilter(configuration::getWsAllowedOrigins));
        registration.addUrlPatterns(prefix(configuration) + "/*");
        return registration;
    }

    /** The compression level of the WebSocket frames: Jetty takes its pool from this attribute of the servlet context. */
    @Bean
    @ConditionalOnMissingBean(name = "ponySDKWebSocketDeflater")
    ServletContextInitializer ponySDKWebSocketDeflater(final ApplicationConfiguration configuration) {
        return servletContext -> {
            final int level = configuration.getWsDeflateLevel();
            if (level >= 0) { // the configuration accepts -1 (Jetty's default) to 9
                servletContext.setAttribute(WEBSOCKET_DEFLATER_POOL_ATTRIBUTE,
                    new DeflaterPool(CompressionPool.DEFAULT_CAPACITY, level, true));
            }
        };
    }

    /** PonySDK's WebSocket servlet is Jetty's: Tomcat or Undertow would start and fail at the first page. */
    @Bean
    @ConditionalOnMissingBean(name = "ponySDKJettyOnly")
    WebServerFactoryCustomizer<ServletWebServerFactory> ponySDKJettyOnly() {
        return factory -> {
            if (!(factory instanceof JettyServletWebServerFactory)) {
                throw new IllegalStateException("PonySDK needs Jetty as the web server, and this one is " + factory.getClass().getName()
                        + ": use spring-boot-starter-jetty, and exclude spring-boot-starter-tomcat from the other starters");
            }
        };
    }

    /** Drains on a graceful shutdown, the default of Boot 4 ({@code server.shutdown}). */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "server.shutdown", havingValue = "graceful", matchIfMissing = true)
    PonySDKDrain ponySDKDrain(final PonySDKProperties properties) {
        return new PonySDKDrain(properties.getDrainTimeout());
    }

    private static String prefix(final ApplicationConfiguration configuration) {
        return "/" + configuration.getApplicationContextName();
    }

    /** With the actuator: {@code /actuator/health} says OUT_OF_SERVICE while the pages move. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HealthIndicator.class)
    static class HealthConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "ponysdkHealthIndicator")
        @ConditionalOnEnabledHealthIndicator("ponysdk")
        PonySDKHealthIndicator ponysdkHealthIndicator() {
            return new PonySDKHealthIndicator();
        }
    }
}
