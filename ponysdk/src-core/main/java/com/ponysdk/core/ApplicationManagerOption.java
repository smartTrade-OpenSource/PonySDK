
package com.ponysdk.core;

import java.util.concurrent.TimeUnit;

public class ApplicationManagerOption {

    public long maxOutOfSyncDuration = -1;
    public long heartBeatPeriod = 0;// seconds
    public int instructionsSizeLimit = 10000;

    public int getInstructionsSizeLimit() {
        return instructionsSizeLimit;
    }

    public void setHeartBeatPeriod(final long heartBeatPeriod, final TimeUnit timeUnit) {
        this.heartBeatPeriod = TimeUnit.SECONDS.convert(heartBeatPeriod, timeUnit);
    }

    public void setInstructionsSizeLimit(final int instructionsSizeLimit) {
        this.instructionsSizeLimit = instructionsSizeLimit;
    }

    public long getHeartBeatPeriod() {
        return heartBeatPeriod;
    }
}
