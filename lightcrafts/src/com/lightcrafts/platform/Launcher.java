/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.platform;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.CheckForUpdate;
import com.lightcrafts.app.ExceptionDialog;
import com.lightcrafts.splash.SplashImage;
import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.utils.ForkDaemon;
import com.lightcrafts.utils.Version;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            showAppVersion();
            showJavaVersion();
            checkCpu();
            UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

            CheckForUpdate.start();

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

            CheckForUpdate.showAlertIfAvailable();
        }
        catch (Throwable t) {
            (new ExceptionDialog()).handle(t);
        }
    }

    protected void setSystemProperties() {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    protected void showAppVersion() {
        final String rev = Version.getRevisionNumber();
        final String msg = "This is " + Version.getApplicationName() + ' '
                + Version.getVersionName();
        System.out.println(
                rev.isEmpty() ? msg : msg + ' ' + '(' + rev + ')'
        );
    }

    protected String showJavaVersion() {
        final String javaVersion = System.getProperty( "java.version" );
        final String javaArch = System.getProperty( "os.arch" );
        System.out.println( "Running Java version " + javaVersion + ' '
                + '(' + javaArch + ')' );
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

