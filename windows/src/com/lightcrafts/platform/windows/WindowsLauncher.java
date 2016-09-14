/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.platform.windows;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.app.other.WindowsApplication;
import com.lightcrafts.platform.Launcher;

import java.awt.EventQueue;
import java.io.File;

/**
 * Launch LightZone for Windows.
 */
public final class WindowsLauncher extends Launcher {

    ////////// public /////////////////////////////////////////////////////////

    public static void main(String[] args) {
        final Launcher launcher = new WindowsLauncher();
        launcher.init(args);
    }

    /**
     * Tell the native launcher that we're ready to open image files.  Note
     * that the code for this native method is inside the native launcher.
     *
     * @noinspection UNUSED_SYMBOL
     */
    public static native void readyToOpenFiles();

    ////////// protected //////////////////////////////////////////////////////

    @Override
    protected void setSystemProperties() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        if (System.getProperty("os.arch").endsWith("64")) {
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        }
    }

    @Override
    protected void startForkDaemon() {
        // Do nothing.
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * This method is called from the native, custom JavaAppLauncher whenever
     * there is a file to open.
     *
     * @param pathToFile The full path to the {@link File} to open.
     * @param parentExe The name of the application executable (including the
     * <code>.exe</code> extension) of the parent process that requested us to
     * open the given {@link File}.
     * @noinspection UNUSED_SYMBOL
     */
    private static void openFile( String pathToFile, String parentExe ) {
        final File file = new File( pathToFile );
        final OtherApplication app =
            WindowsApplication.getAppForExe( parentExe );
        System.out.println( "Parent process: " + parentExe );
        System.out.println( "File path: " + pathToFile );
        EventQueue.invokeLater( () -> Application.openFrom( file, app ) );
    }

}
/* vim:set et sw=4 ts=4: */
