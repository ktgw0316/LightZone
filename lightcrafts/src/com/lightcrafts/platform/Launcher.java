/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.platform;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ExceptionDialog;
import com.lightcrafts.splash.SplashImage;
import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.utils.ForkDaemon;
import com.lightcrafts.utils.Version;

import javax.swing.*;
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
            Class<?> clazz0 = Class.forName("sun.swing.SwingUtilities2");
            Method isLocalDisplay = clazz0.getMethod("isLocalDisplay");
            final Object lafCond = isLocalDisplay.invoke(null);

            Class<?> clazz = Class.forName("sun.swing.SwingUtilities2$AATextInfo");
            Method method = clazz.getMethod("getAATextInfo", boolean.class);
            Object aaTextInfo = method.invoke(null, lafCond);

            Field field = clazz0.getField("AA_TEXT_PROPERTY_KEY");
            Object aaTextPropertyKey = field.get(null);
            UIManager.getDefaults().put(aaTextPropertyKey, aaTextInfo);
        }
        // Java 9 does not have the class SwingUtilities2.AATextInfo anymore,
        // but text anti-aliasing is enabled by default.
        catch (ClassNotFoundException    ignored) {}
        catch (NoSuchMethodException     ignored) {}
        catch (InvocationTargetException ignored) {}
        catch (IllegalAccessException    ignored) {}
        catch (NoSuchFieldException      ignored) {}
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

