/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016- Masahiro Kitagawa */

package com.lightcrafts.platform.macosx;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.other.MacApplication;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Launcher;
import com.lightcrafts.platform.Platform;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.List;

import java.awt.Desktop;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.AppReopenedListener;
import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;

import static com.lightcrafts.platform.macosx.Locale.LOCALE;

/**
 * Launch LightZone for Mac OS X.
 */
public final class MacOSXLauncher extends Launcher {

    ////////// public /////////////////////////////////////////////////////////

    public static void main(String[] args) {
        final Launcher launcher = new MacOSXLauncher();
        launcher.init(args);
    }

    ////////// protected //////////////////////////////////////////////////////

    @Override
    protected String showJavaVersion() {
        String javaVersion = super.showJavaVersion();
        checkJavaVersion(javaVersion);
        return javaVersion;
    }

   /**
     * Set the color back to the user's defaults
     */
    @Override
    protected void setColor() {
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
    private static void checkJavaVersion(String javaVersion) {
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
        openFile(new File( pathName ), senderSig);
    }

    /**
     * Open a file passed via an AppleEvent.
     *
     * @param files The list of files to open.
     * @param senderSig The 4-character signature of the application that sent
     * the AppleEvent to open the given file.
     */
    private static synchronized void openFile( final List<File> files,
                                               String senderSig ) {
        for (final File file : files) {
            openFile(file, senderSig);
        }
    }

    /**
     * Open a file passed via an AppleEvent.
     *
     * @param file The file to open.
     * @param senderSig The 4-character signature of the application that sent
     * the AppleEvent to open the given file.
     */
    private static synchronized void openFile( final File file,
                                               String senderSig ) {
        final OtherApplication app =
            MacApplication.getAppForSignature( senderSig );
        EventQueue.invokeLater(
            new Runnable() {
                @Override
                public void run() {
                    Application.openFrom( file, app );
                }
            }
        );
    }

    /**
     * Re-open the application.
     */
    private static void reOpen() {
        EventQueue.invokeLater(
            new Runnable() {
                @Override
                public void run() {
                    Application.reOpen(null);
                }
            }
        );
    }

    /**
     * Quit the application.
     */
    private static void quit() {
        EventQueue.invokeLater(
            new Runnable() {
                @Override
                public void run() {
                    Application.quit();
                }
            }
        );
    }

    /**
     * Show the "About" box.
     */
    private static void showAbout() {
        EventQueue.invokeLater(
            new Runnable() {
                @Override
                public void run() {
                    Application.showAbout();
                }
            }
        );
    }

    /**
     * Show the Preferences dialog.
     */
    private static void showPreferences() {
        EventQueue.invokeLater(
            new Runnable() {
                @Override
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
        System.setProperty( "apple.awt.graphics.UseQuartz", "false" );

        final Desktop app = Desktop.getDesktop(); 
        app.setAboutHandler(new AboutHandler() {
            @Override
            public void handleAbout(AboutEvent e) {
                showAbout();
            }
        });
        app.setOpenFileHandler(new OpenFilesHandler() {
            @Override
            public void openFiles(OpenFilesEvent e) {
                openFile(e.getFiles(), null); // TODO: get senderSig
            }
        });
        app.setPreferencesHandler(new PreferencesHandler() {
            @Override
            public void handlePreferences(PreferencesEvent e) {
                showPreferences();
            }
        });
        app.setQuitHandler(new QuitHandler() {
            @Override
            public void handleQuitRequestWith(QuitEvent e, QuitResponse qr) {
                quit();
                qr.cancelQuit();
            }
        });
        app.addAppEventListener(new AppReopenedListener() {
            @Override
            public void appReopened(AppReopenedEvent e) {
                reOpen();
            }
        });
    }
}
/* vim:set et sw=4 ts=4: */
