
package com.ponysdk.core;

import java.util.concurrent.TimeUnit;

public class ApplicationManagerOption {

    private String applicationID;
    private String applicationName;
    private String applicationDescription;
    private final long maxOutOfSyncDuration = -1;
    private long heartBeatPeriod = 5000;// ms
    private TimeUnit heartBeatPeriodTimeUnit = TimeUnit.MILLISECONDS;

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

    @Override
    public String toString() {
        return "ApplicationManagerOption [maxOutOfSyncDuration=" + maxOutOfSyncDuration + ", heartBeatPeriod=" + heartBeatPeriod + " " + heartBeatPeriodTimeUnit + "]";
    }
}
