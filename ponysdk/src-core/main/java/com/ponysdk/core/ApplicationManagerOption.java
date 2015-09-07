
package com.ponysdk.core;

import java.util.concurrent.TimeUnit;

public class ApplicationManagerOption {

    public long maxOutOfSyncDuration = -1;
    public long heartBeatPeriod = 0;// seconds

    public void setHeartBeatPeriod(final long heartBeatPeriod, final TimeUnit timeUnit) {
        this.heartBeatPeriod = TimeUnit.SECONDS.convert(heartBeatPeriod, timeUnit);
    }

    public long getHeartBeatPeriod() {
        return heartBeatPeriod;
    }

    @Override
    public String toString() {
        return "ApplicationManagerOption [maxOutOfSyncDuration=" + maxOutOfSyncDuration + ", heartBeatPeriod=" + heartBeatPeriod + "]";
    }
}
