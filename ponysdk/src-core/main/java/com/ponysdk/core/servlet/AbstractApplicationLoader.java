
package com.ponysdk.core.servlet;

import java.util.Calendar;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.AbstractApplicationManager;
import com.ponysdk.core.ApplicationManagerOption;
import com.ponysdk.core.tools.BannerPrinter;

public abstract class AbstractApplicationLoader implements ServletContextListener, HttpSessionListener {

    protected static final Logger log = LoggerFactory.getLogger(AbstractApplicationLoader.class);

    protected final ApplicationManagerOption option;

    private final AbstractApplicationManager applicationManager;

    public AbstractApplicationLoader(final ApplicationManagerOption option) {
        this.option = option;

        applicationManager = createApplicationManager(option);

        printLicence();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        printDestroyedBanner();
    }

    @Override
    public void sessionCreated(final HttpSessionEvent httpSessionEvent) {
        if (log.isInfoEnabled()) {
            log.info("HTTP Session created: {}", httpSessionEvent.getSession().getId());
        }

        SessionManager.get().registerSession(new PonySDKSession(httpSessionEvent.getSession()));
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent httpSessionEvent) {
        if (log.isInfoEnabled()) {
            log.info("HTTP Session Destroyed: {}", httpSessionEvent.getSession().getId());
        }

        SessionManager.get().unregisterSession(httpSessionEvent.getSession().getId());
    }

    private void printLicence() {
        final BannerPrinter bannerPrinter = new BannerPrinter(60);
        bannerPrinter.appendNewEmptyLine(2);
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendCenteredLine("PonySDK http://www.ponysdk.com");
        bannerPrinter.appendCenteredLine("WEB  APPLICATION");
        bannerPrinter.appendCenteredLine(option.getApplicationID());
        bannerPrinter.appendCenteredLine(option.getApplicationName());
        bannerPrinter.appendCenteredLine("(c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        bannerPrinter.appendLineSeparator();

        bannerPrinter.print();
    }

    private void printDestroyedBanner() {
        final BannerPrinter bannerPrinter = new BannerPrinter(60);
        bannerPrinter.appendNewEmptyLine(2);
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendCenteredLine("Context Destroyed");
        bannerPrinter.appendCenteredLine(option.getApplicationID());
        bannerPrinter.appendCenteredLine(option.getApplicationName());
        bannerPrinter.appendCenteredLine("(c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        bannerPrinter.appendLineSeparator();

        bannerPrinter.print();
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        event.getServletContext().setAttribute(AbstractApplicationLoader.class.getCanonicalName(), this);
        event.getServletContext().setAttribute(AbstractApplicationManager.class.getCanonicalName(), applicationManager);
    }

    protected abstract AbstractApplicationManager createApplicationManager(ApplicationManagerOption option);

}
