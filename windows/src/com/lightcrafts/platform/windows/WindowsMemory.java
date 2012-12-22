/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

/**
 * Gets information about the memory in this computer.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsMemory {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the amount of physical memory installed in the computer.
     *
     * @return Returns the amount of memory in megabytes.
     */
    public static native int getPhysicalMemoryInMB();

    ////////// private ////////////////////////////////////////////////////////

    static {
        System.loadLibrary( "Windows" );
    }
}
/* vim:set et sw=4 ts=4: */
