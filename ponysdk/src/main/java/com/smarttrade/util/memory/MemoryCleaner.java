/*============================================================================
 *
 * Copyright (c) 2000-2018 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.smarttrade.util.memory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.management.MBeanServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * The objective of this class is to trigger a Full GC. One of the idea behind
 * this behavior is to promote static data (like cache for providers) to the Old
 * space, thus enhancing minor GCs after a JVM start.<br/>
 * This class is a mostly copy/paste from
 * {@link com.smarttrade.util.HeapDumper}.
 *
 * @see <a href=
 *      "https://developer.smart-trade.net/jira/browse/DVLPT-7768">DVLPT-7768</a>
 * @author ebillaud
 */
public final class MemoryCleaner {

    private static final Logger log = LoggerFactory.getLogger(MemoryCleaner.class);

    private MemoryCleaner() {
        // no instance allowed
    }

    /**
     * Try to perform a Full GC.
     *
     * @param cleanWithHeapDump whether the Full GC shall be initiated with a
     *            heap dump (if not, or if heap dump fails, calls
     *            <code>System.gc()</code>).
     * @param deleteHeapDump whether the generated heap dump (if any) shall
     *            be deleted.
     */
    public static void cleanMemory(final boolean cleanWithHeapDump, final boolean deleteHeapDump) {
        boolean heapDumped = false;
        if (cleanWithHeapDump) {
            try {
                final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                final HotSpotDiagnosticMXBean mbean = ManagementFactory.newPlatformMXBeanProxy(server,
                    "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
                final String fileName = "memory_cleaner_" + System.currentTimeMillis() + "_live.hprof";
                try {
                    mbean.dumpHeap(fileName, true);
                    log.info("The MemoryCleaner heap dump '{}' has been successfully generated", fileName);
                    heapDumped = true;

                    if (deleteHeapDump) {
                        try {
                            Files.delete(Paths.get(fileName));
                            log.info("The hprof '{}' was successfully deleted", fileName);
                        } catch (final IOException e) {
                            log.error("Could not delete the generated hprof " + fileName, e);
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    log.error("Cannot dump heap (MemoryCleaner) through HotspotDiagnostic MBean", e);
                }
            } catch (final IOException | RuntimeException e) {
                log.error("Cannot find HotspotDiagnostic MBean for the MemoryCleaner heap dump", e);
            }
        }

        // If heap dump failed, at least try to call an explicit GC
        if (!heapDumped) System.gc();
    }

}
