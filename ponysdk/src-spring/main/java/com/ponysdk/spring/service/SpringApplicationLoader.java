
package com.ponysdk.spring.service;

import javax.servlet.ServletContextEvent;

import com.ponysdk.core.servlet.ApplicationLoader;

public class SpringApplicationLoader extends ApplicationLoader {

    private final SpringContextLoader contextLoader = new SpringContextLoader();

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        super.contextInitialized(event);

        contextLoader.initWebApplicationContext(event.getServletContext());
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        contextLoader.closeWebApplicationContext(event.getServletContext());

        super.contextDestroyed(event);
    }
}
