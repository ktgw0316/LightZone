/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import com.lightcrafts.app.Application;
// import com.lightcrafts.app.CheckForUpdate;
import com.lightcrafts.app.ExceptionDialog;
import com.lightcrafts.app.other.MacApplication;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.splash.SplashImage;
import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.utils.ForkDaemon;
import com.lightcrafts.utils.Version;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static com.lightcrafts.platform.macosx.Locale.LOCALE;

/**
 * Launch LightZone for Mac OS X.
 */
public final class MacOSXLauncher {

    /**
     * Does the following:
     *  <ol>
     *    <li>Check the Java version.
     *    <li>Check for a valid license.
     *    <li>Show the splash screen.
     *    <li>Launches the application.
     *    <li>Disposes of the splash screen.
     *    <li>Check for a LightZone update.
     *  </ol>
     *
     * @param args The command line arguments.
     */
    public static void main( final String[] args ) {
        // java.util.Locale.setDefault( java.util.Locale.ENGLISH );
        // System.setProperty("apple.awt.graphics.UseQuartz", "false");

        System.out.println(
            "This is " + Version.getApplicationName() + ' '
            + Version.getVersionName()
            + " (" + Version.getRevisionNumber() + ')'
        );

        try {
            checkJavaVersion();
            final String licenseText = "Open Source";

            final SplashImage image = new SplashImage(
                SplashImage.getDefaultSplashText(licenseText)
            );
            SplashWindow.splash(image);
            setColor();
            // CheckForUpdate.start();
            {
                Application.setStartupProgress(
                        image.getStartupProgress()
                );
                ForkDaemon.start();
                Application.main(args);
            }
            SplashWindow.disposeSplash();
            // CheckForUpdate.showAlertIfAvailable();
        }
        catch (Throwable t) {
            (new ExceptionDialog()).handle(t);
        }
    }

    /**
     * Tell the native launcher that we're ready to open image files.  Note
     * that the code for this native method is inside the native launcher.
     */
    public static native void readyToOpenFiles();

    ////////// private ////////////////////////////////////////////////////////

    //
    // Values correspond to M.m.p_r, e.g. "1.5.0_13".
    //
    private static final int REQUIRED_MIN_MAJOR    = 1;
    private static final int REQUIRED_MIN_MINOR    = 6;
    private static final int REQUIRED_MIN_POINT    = 0;
    private static final int REQUIRED_MIN_REVISION = 0; // accept any rev.

    /**
     * Check that the currently running JVM meets our minimum required version.
     * If it doesn't, show an error dialog and quit.
     */
    private static void checkJavaVersion() {
        final String javaVersion = System.getProperty( "java.version" );
        System.out.println( "Running Java version " + javaVersion );
        try {
            final String[] parts1 = javaVersion.split( "\\." );

            final int major = Integer.parseInt( parts1[0] );
            if ( major < REQUIRED_MIN_MAJOR )
                showUpgradeJavaDialogAndQuit();
            if ( major > REQUIRED_MIN_MAJOR )
                return;

            final int minor = Integer.parseInt( parts1[1] );
            if ( minor < REQUIRED_MIN_MINOR )
                showUpgradeJavaDialogAndQuit();
            if ( minor > REQUIRED_MIN_MINOR )
                return;

            if ( parts1.length < 3 )
                showUpgradeJavaDialogAndQuit();

            final String[] parts2 = parts1[2].split( "_" );

            final int point = Integer.parseInt( parts2[0] );
            if ( point < REQUIRED_MIN_POINT )
                showUpgradeJavaDialogAndQuit();
            if ( point > REQUIRED_MIN_POINT )
                return;

            if ( parts2.length < 2 )
                showUpgradeJavaDialogAndQuit();

            final int revision = Integer.parseInt( parts2[1] );
            if ( revision < REQUIRED_MIN_REVISION )
                showUpgradeJavaDialogAndQuit();
        }
        catch ( Exception e ) {
            showUpgradeJavaDialogAndQuit();
        }
    }

    /**
     * Open a file passed via an AppleEvent.  This method is called only from
     * native code.
     *
     * @param pathName The full path of the file to open.
     * @param senderSig The 4-character signature of the application that sent
     * the AppleEvent to open the given file.
     */
    @SuppressWarnings( { "UnusedDeclaration" } )
    private static synchronized void openFile( final String pathName,
                                               String senderSig ) {
        final OtherApplication app =
            MacApplication.getAppForSignature( senderSig );
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    Application.openFrom( new File( pathName ), app );
                }
            }
        );
    }

    /**
     * Quit the application.  This method is called only from native code.
     */
    @SuppressWarnings( { "UnusedDeclaration" } )
    private static void quit() {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    Application.quit();
                }
            }
        );
    }

   /**
     * Set the color back to the user's defaults
     */
    private static void setColor() {
        try {
            final Process p = Runtime.getRuntime().exec(
                // "defaults write com.lightcrafts.LightZone AppleAquaColorVariant -int 6"
                "defaults remove com.lightcrafts.LightZone AppleAquaColorVariant"
            );
            p.waitFor();
        }
        catch ( InterruptedException e ) {
            e.printStackTrace();
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Show the "About" box.  This method is called only from native code.
     */
    @SuppressWarnings( { "UnusedDeclaration" } )
    private static void showAbout() {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    Application.showAbout();
                }
            }
        );
    }

    /**
     * Show the Preferences dialog.  This method is called only from native
     * code.
     */
    @SuppressWarnings( { "UnusedDeclaration" } )
    private static void showPreferences() {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    Application.showPreferences();
                }
            }
        );
    }

    /**
     * Show the user a dialog telling him/her to upgrade the installed Java
     * version, then quit.
     */
    private static void showUpgradeJavaDialogAndQuit() {
        final AlertDialog dialog = Platform.getPlatform().getAlertDialog();
        dialog.showAlert(
            null, LOCALE.get( "UpgradeJavaErrorMajor" ),
            LOCALE.get( "UpgradeJavaErrorMinor" ),
            AlertDialog.ERROR_ALERT, LOCALE.get( "QuitButton" )
        );
        System.exit( 0 );
    }

    static {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        System.setProperty( "apple.awt.showGrowBox"     , "true" );
        // System.setProperty( "apple.awt.textantialiasing", "true" );
        // System.setProperty( "apple.awt.antialiasing"    , "false" );
        System.setProperty( "swing.aatext", "true" );
        System.setProperty( "apple.awt.graphics.UseQuartz", "false" );
    }
}
/* vim:set et sw=4 ts=4: */
