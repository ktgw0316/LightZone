/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.linux;

/**
 * A JNI wrapper for the Linux implementation of Platform.isKeyPressed().
 */
class LinuxKeyUtil {

    static native boolean isKeyPressed( int keyCode );

    static {
        System.loadLibrary( "Linux" );
    }
}
