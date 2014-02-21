
package com.ponysdk.core;

import java.util.concurrent.TimeUnit;

public class ApplicationManagerOption {

    public long maxOutOfSyncDuration = -1;
    public long heartBeatPeriod = 5;// seconds

    public void setHeartBeatPreiod(final long heartBeatPeriod, final TimeUnit timeUnit) {
        TimeUnit.SECONDS.convert(heartBeatPeriod, timeUnit);
    }

    public long getHeartBeatPeriod() {
        return heartBeatPeriod;
    }
}
