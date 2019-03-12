
package com.ponysdk.core;

import com.hazelcast.config.Config;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;

//============================================================================
//
// Copyright (c) 2000-2005 Smart Trade Technologies. All Rights Reserved.
//
// This software is the proprietary information of Smart Trade Technologies
// Use is subject to license terms.
//
//============================================================================

public class SlaveDistributedTestTask {

    public static void main(String[] args) throws Exception {
        final Config config = new Config();
        config.setInstanceName("gpd");
        final JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(true);
        join.getMulticastConfig().setMulticastGroup("224.0.0.12");

        final ExecutorConfig executorConfig = config.getExecutorConfig("exec");
        executorConfig.setPoolSize(1).setQueueCapacity(10).setStatisticsEnabled(true).setQuorumName("quorumname");

        final HazelcastInstance instance = Hazelcast.getOrCreateHazelcastInstance(config);
        final IExecutorService executor = instance.getExecutorService("exec");

        System.in.read();
    }
}
