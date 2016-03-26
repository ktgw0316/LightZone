/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.platform;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ExceptionDialog;
import com.lightcrafts.splash.SplashImage;
import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.utils.ForkDaemon;
import com.lightcrafts.utils.Version;

import javax.swing.*;
import java.io.IOException;

/**
 * Launch LightZone.
 */
public class Launcher {

    /**
     * @param args The command line arguments.
     */
    public void init(String[] args) {
        try {
            setSystemProperties();
            enableTextAntiAliasing();
            showAppVersion();
            showJavaVersion();
            checkCpu();
            UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

            final String licenseText = "Open Source";
            final SplashImage splash = new SplashImage(
                SplashImage.getDefaultSplashText(licenseText)
            );
            SplashWindow.splash(splash);
            setColor();
            Application.setStartupProgress(splash.getStartupProgress());
            startForkDaemon();
            Application.main(args);
            SplashWindow.disposeSplash();
        }
        catch (Throwable t) {
            (new ExceptionDialog()).handle(t);
        }
    }

    protected void setSystemProperties() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    protected void enableTextAntiAliasing() {
        try {
            final boolean lafCond = sun.swing.SwingUtilities2.isLocalDisplay();
            Object aaTextInfo = sun.swing.SwingUtilities2.AATextInfo.getAATextInfo(lafCond);
            UIManager.getDefaults().put(sun.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, aaTextInfo);
        }
        catch (NoClassDefFoundError e) {
            // Java 9 does not have the class SwingUtilities2.AATextInfo anymore,
            // but text anti-aliasing is enabled by default.
        }
    }

    protected void showAppVersion() {
        System.out.println(
                "This is " + Version.getApplicationName() + ' '
                        + Version.getVersionName() + ' '
                        + '(' + Version.getRevisionNumber() + ')'
        );
    }

    protected String showJavaVersion() {
        final String javaVersion = System.getProperty( "java.version" );
        System.out.println( "Running Java version " + javaVersion );
        return javaVersion;
    }

    protected void checkCpu() {
        // Do nothing by default.
    }

    protected void setColor() {
        // Do nothing by default.
    }

    protected void startForkDaemon() throws IOException {
        ForkDaemon.start();
    }

}

