/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import com.lightcrafts.platform.Platform;

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
        return 256;
    }

    public static int getMaximum() {
        int physicalMax = Platform.getPlatform().getPhysicalMemoryInMB();
        int platformMax = (int)(Runtime.getRuntime().maxMemory() / 1048576);
        return Math.min(physicalMax, platformMax);
    }

    public static int getDefault() {
        int physicalMax = Platform.getPlatform().getPhysicalMemoryInMB();
        int limit = (int) Math.round(DefaultMemoryFraction * physicalMax);
        limit = Math.max(limit, getMinimum());
        limit = Math.min(limit, getMaximum());
        return limit;
    }
}
