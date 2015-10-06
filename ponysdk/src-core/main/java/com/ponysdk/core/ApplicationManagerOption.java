
package com.ponysdk.core;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApplicationManagerOption {

    private String applicationID;
    private String applicationName;
    private String applicationDescription;
    private String applicationContextName = "sample";
    private final long maxOutOfSyncDuration = -1;
    private long heartBeatPeriod = 5000;// ms
    private TimeUnit heartBeatPeriodTimeUnit = TimeUnit.MILLISECONDS;

    private int sessionTimeout = 15; // minutes

    private String communicationErrorFunction;

    private List<String> javascript = Collections.emptyList();
    private List<String> style = Collections.emptyList();
    private List<String> meta = Collections.emptyList();
    private List<String> customJavascript = Collections.emptyList();
    private List<String> customStyle = Collections.emptyList();
    private List<String> customMeta = Collections.emptyList();

    public ApplicationManagerOption() {
        applicationID = System.getProperty(SystemProperty.APPLICATION_ID, applicationID);
        applicationName = System.getProperty(SystemProperty.APPLICATION_NAME, applicationName);
        applicationDescription = System.getProperty(SystemProperty.APPLICATION_DESCRIPTION, applicationDescription);

        if (applicationID != null) System.setProperty(SystemProperty.APPLICATION_ID, applicationID);
        if (applicationName != null) System.setProperty(SystemProperty.APPLICATION_NAME, applicationName);
        if (applicationDescription != null) System.setProperty(SystemProperty.APPLICATION_DESCRIPTION, applicationDescription);
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(final String applicationID) {
        this.applicationID = applicationID;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationDescription() {
        return applicationDescription;
    }

    public void setApplicationDescription(final String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    public String getApplicationContextName() {
        return applicationContextName;
    }

    public void setApplicationContextName(final String applicationContextName) {
        this.applicationContextName = applicationContextName;
    }

    public void setHeartBeatPeriod(final long heartBeatPeriod, final TimeUnit heartBeatPeriodTimeUnit) {
        this.heartBeatPeriod = heartBeatPeriod;
        this.heartBeatPeriodTimeUnit = heartBeatPeriodTimeUnit;
    }

    public long getHeartBeatPeriod() {
        return heartBeatPeriod;
    }

    public TimeUnit getHeartBeatPeriodTimeUnit() {
        return heartBeatPeriodTimeUnit;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(final int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public List<String> getJavascript() {
        return javascript;
    }

    public void setJavascript(final List<String> javascript) {
        this.javascript = javascript;
    }

    public List<String> getCustomJavascript() {
        return customJavascript;
    }

    public void setCustomJavascript(final List<String> customJavascript) {
        this.customJavascript = customJavascript;
    }

    public List<String> getStyle() {
        return style;
    }

    public void setStyle(final List<String> style) {
        this.style = style;
    }

    public List<String> getCustomStyle() {
        return customStyle;
    }

    public void setCustomStyle(final List<String> customStyle) {
        this.customStyle = customStyle;
    }

    public List<String> getMeta() {
        return meta;
    }

    public void setMeta(final List<String> meta) {
        this.meta = meta;
    }

    public List<String> getCustomMeta() {
        return customMeta;
    }

    public void setCustomMeta(final List<String> customMeta) {
        this.customMeta = customMeta;
    }

    public String getCommunicationErrorFunction() {
        return communicationErrorFunction;
    }

    public void setCommunicationErrorFunction(final String communicationErrorFunction) {
        this.communicationErrorFunction = communicationErrorFunction;
    }

    @Override
    public String toString() {
        return "ApplicationManagerOption [maxOutOfSyncDuration=" + maxOutOfSyncDuration + ", heartBeatPeriod=" + heartBeatPeriod + " " + heartBeatPeriodTimeUnit + "]";
    }
}
