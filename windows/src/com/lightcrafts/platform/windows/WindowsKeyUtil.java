/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

/**
 * A JNI wrapper for the Windows implementation of Platform.isKeyPressed().
 */
class WindowsKeyUtil {

    static native boolean isKeyPressed( int keyCode );

    static {
        System.loadLibrary( "Windows" );
    }
}
