/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package bench;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.impl.java.server.JavaApplicationManager;
import com.ponysdk.impl.main.PonySDKServer;

/**
 * Starts the bench application on the given port and prints, every second, the cumulative counters of this JVM:
 * {@code stat <epochMs> <setter calls> <process CPU ns> <allocated bytes> <sessions>}. Creating the file
 * {@code gc.req} in the working directory runs two full GCs and prints {@code live <heap used> <rss KB before the GCs>}.
 */
public final class BenchServer {

    public static void main(final String[] args) throws Exception {
        final int port = Integer.parseInt(args[0]);
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setApplicationID("bench");
        configuration.setApplicationName("bench");
        configuration.setApplicationDescription("bench");
        configuration.setApplicationContextName("bench");
        configuration.setSessionTimeout(60);
        configuration.setEntryPointClass(BenchEntryPoint.class);

        final JavaApplicationManager applicationManager = new JavaApplicationManager();
        applicationManager.setConfiguration(configuration);

        final PonySDKServer server = new PonySDKServer();
        server.setApplicationManager(applicationManager);
        server.setPort(port);
        server.setHost("127.0.0.1");
        server.setUseSSL(false);
        server.start();

        final com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory
            .getOperatingSystemMXBean();
        final com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        final Path gcReq = Path.of("gc.req");
        System.out.println("ready " + port);
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.out.println("stat " + System.currentTimeMillis() + " " + BenchEntryPoint.calls.sum() + " "
                    + os.getProcessCpuTime() + " " + threads.getTotalThreadAllocatedBytes() + " "
                    + BenchEntryPoint.sessions.sum());
            if (Files.exists(gcReq)) {
                try {
                    Files.delete(gcReq);
                    final Process ps = new ProcessBuilder("ps", "-o", "rss=", "-p", Long.toString(ProcessHandle.current().pid()))
                        .start();
                    final String rss = new String(ps.getInputStream().readAllBytes()).trim();
                    System.gc();
                    System.gc();
                    System.out.println("live " + memory.getHeapMemoryUsage().getUsed() + " " + rss);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}
