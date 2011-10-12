/*
 * Copyright (c) 2011 PonySDK
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
package com.ponysdk.core.spring;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class ContextLoader {
    private static final Logger log = LoggerFactory.getLogger(ContextLoader.class);

    private XmlWebApplicationContext context;

    public void initWebApplicationContext(ServletContext servletContext) {
        final String configFiles = "classpath:com/ponysdk/core/conf/server_application.inc.xml," + servletContext.getInitParameter(org.springframework.web.context.ContextLoader.CONFIG_LOCATION_PARAM);
        context = new XmlWebApplicationContext();
        context.setServletContext(servletContext);
        context.setConfigLocations(StringUtils.tokenizeToStringArray(configFiles, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
        context.refresh();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
    }

    public void closeWebApplicationContext(ServletContext servletContext) {
        servletContext.log("Closing Spring context");
        try {
            if (this.context instanceof ConfigurableWebApplicationContext) {
                ((ConfigurableWebApplicationContext) this.context).close();
            }
        } catch (final Exception e) {
            log.error("Failure during Spring context closure", e);
        }
    }

    public XmlWebApplicationContext getContext() {
        return context;
    }
}
