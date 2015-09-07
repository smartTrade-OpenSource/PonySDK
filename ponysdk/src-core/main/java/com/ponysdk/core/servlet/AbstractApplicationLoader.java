
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
import com.ponysdk.core.SystemProperty;
import com.ponysdk.core.tools.BannerPrinter;

public abstract class AbstractApplicationLoader implements ServletContextListener, HttpSessionListener {

    protected static final Logger log = LoggerFactory.getLogger(AbstractApplicationLoader.class);

    private String applicationID;
    private String applicationName;
    private String applicationDescription;

    protected final ApplicationManagerOption option;

    private final AbstractApplicationManager applicationManager;

    public AbstractApplicationLoader(final ApplicationManagerOption option) {
        this.option = option;

        applicationID = System.getProperty(SystemProperty.APPLICATION_ID, applicationID);
        applicationName = System.getProperty(SystemProperty.APPLICATION_NAME, applicationName);
        applicationDescription = System.getProperty(SystemProperty.APPLICATION_DESCRIPTION, applicationDescription);

        if (applicationID != null) System.setProperty(SystemProperty.APPLICATION_ID, applicationID);
        if (applicationName != null) System.setProperty(SystemProperty.APPLICATION_NAME, applicationName);
        if (applicationDescription != null) System.setProperty(SystemProperty.APPLICATION_DESCRIPTION, applicationDescription);

        applicationManager = createApplicationManager(option);

        printLicence();

    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        printDestroyedBanner();
    }

    @Override
    public void sessionCreated(final HttpSessionEvent httpSessionEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Session created #" + httpSessionEvent.getSession().getId());
        }

        SessionManager.get().registerSession(new PonySDKSession(httpSessionEvent.getSession()));
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent httpSessionEvent) {
        if (log.isInfoEnabled()) {
            log.info("Session Destroyed #" + httpSessionEvent.getSession().getId());
        }

        SessionManager.get().unregisterSession(httpSessionEvent.getSession().getId());
    }

    private void printDestroyedBanner() {
        String title;

        if (applicationName == null) {
            title = "Pony Application - Context Destroyed";
        } else {
            title = applicationName + " - Context Destroyed";
        }

        final int columnCount = title.length() + 5;

        final BannerPrinter bannerPrinter = new BannerPrinter(columnCount);
        bannerPrinter.appendNewEmptyLine(2);
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendNewLine(2);
        bannerPrinter.appendCenteredLine(title);
        bannerPrinter.appendNewLine(2);
        bannerPrinter.appendLineSeparator();

        log.info(bannerPrinter.toString());
    }

    private void printLicence() {
        String title = "";
        if (applicationName == null && applicationDescription == null) {
            title = "Powered by PonySDK http://www.ponysdk.com";
        } else if (applicationName == null && applicationDescription != null) {
            title = applicationDescription;
        } else if (applicationName == null && applicationDescription != null) {
            title = applicationName;
        } else {
            title = applicationName + " - " + applicationDescription;
        }

        final int columnCount = title.length() + 30;

        final BannerPrinter bannerPrinter = new BannerPrinter(columnCount);
        bannerPrinter.appendNewEmptyLine();
        bannerPrinter.appendLineSeparator();
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine(title);
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine("WEB  APPLICATION");
        bannerPrinter.appendNewLine();
        bannerPrinter.appendCenteredLine("(c) " + Calendar.getInstance().get(Calendar.YEAR) + " PonySDK");
        bannerPrinter.appendNewLine();
        bannerPrinter.appendLineSeparator();

        log.info(bannerPrinter.toString());
    }

    public void setApplicationID(final String applicationID) {
        this.applicationID = applicationID;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setApplicationDescription(final String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        event.getServletContext().setAttribute(AbstractApplicationLoader.class.getCanonicalName(), this);
        event.getServletContext().setAttribute(AbstractApplicationManager.class.getCanonicalName(), applicationManager);
    }

    protected abstract AbstractApplicationManager createApplicationManager(ApplicationManagerOption option);

}
