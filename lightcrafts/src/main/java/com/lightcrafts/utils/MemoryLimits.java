/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import com.lightcrafts.platform.Platform;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * Some memory limit routines that must be shared among the launchers, the
 * tile cache mechanism, and the preference controls.
 * <p>
 * All memory quantities are in megabytes.
 */
public class MemoryLimits {

    /**
     * The default memory allocated to LightZone, neglecting absolute upper
     * and lower bounds, is three tenths of the physical memory.  See
     * getDefault().
     */
    public final static double DefaultMemoryFraction = .3;

    public static int getMinimum() {
        return 512;
    }

    private final static int physicalMax = Platform.getPlatform().getPhysicalMemoryInMB();

    public static int getMaximum() {
        int maximum = physicalMax / 2;
        maximum = Math.max(maximum, getMinimum());

        // there is ~2GB limit for Java heap size on 32-bit JVM
        if (System.getProperty("sun.arch.data.model").equals("32")) {
            maximum = Math.min(maximum, 2048);
        }
        return maximum;
    }

    public static int getDefault() {
        int limit = (int) Math.round(DefaultMemoryFraction * physicalMax);
        limit = Math.max(limit, getMinimum());
        limit = Math.min(limit, getMaximum());
        return limit;
    }
}
