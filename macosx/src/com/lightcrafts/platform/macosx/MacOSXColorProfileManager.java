/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.util.ArrayList;
import java.util.Collection;

import com.lightcrafts.image.color.ColorProfileInfo;

/**
 * A <code>MacOSXColorProfileManager</code> is a class that is used to get
 * various color profiles.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MacOSXColorProfileManager {

    /**
     * Gets the path to the system display profile.
     *
     * @return Returns said path.
     */
    public static native String getSystemDisplayProfilePath();

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
