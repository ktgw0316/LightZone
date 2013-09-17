/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.CheckForUpdate;
import com.lightcrafts.app.ExceptionDialog;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.app.other.WindowsApplication;
import com.lightcrafts.splash.SplashImage;
import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.utils.Version;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Launch LightZone for Windows.
 */
public final class WindowsLauncher {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Does the following:
     *  <ol>
     *    <li>Sets the Swing pluggable look and feel.
     *    <li>Checks for a valid license.
     *    <li>Shows the splash screen.
     *    <li>Launches the application.
     *    <li>Disposes of the splash screen.
     *    <li>Check for a LightZone update.
     *  </ol>
     *
     * @param args The command line arguments.
     */
    public static void main( String[] args ) {
        System.setProperty("awt.useSystemAAFontSettings", "on");

        final boolean lafCond = sun.swing.SwingUtilities2.isLocalDisplay();
        Object aaTextInfo = sun.swing.SwingUtilities2.AATextInfo.getAATextInfo(lafCond);
        UIManager.getDefaults().put(sun.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, aaTextInfo);

        System.out.println(
            "This is " + Version.getApplicationName() + ' '
            + Version.getVersionName()
            + " (" + Version.getRevisionNumber() + ')'
        );

        final String javaVersion = System.getProperty( "java.version" );
        System.out.println( "Running Java version " + javaVersion );

        try {
            /* Application.setLookAndFeel(
                "net.java.plaf.windows.WindowsLookAndFeel"
            ); */

            final String licenseText = "Open Source";

            CheckForUpdate.start();
            {
                final SplashImage image = new SplashImage(
                    SplashImage.getDefaultSplashText( licenseText )
                );
                SplashWindow.splash( image );
                Application.setStartupProgress( image.getStartupProgress() );
                Application.main( args );
                SplashWindow.disposeSplash();
            }
            CheckForUpdate.showAlertIfAvailable();
        }
        catch ( Throwable t ) {
            (new ExceptionDialog()).handle( t );
        }
    }

    /**
     * Tell the native launcher that we're ready to open image files.  Note
     * that the code for this native method is inside the native launcher.
     *
     * @noinspection UNUSED_SYMBOL
     */
    public static native void readyToOpenFiles();

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
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    Application.openFrom( file, app );
                }
            }
        );
    }
}
/* vim:set et sw=4 ts=4: */
