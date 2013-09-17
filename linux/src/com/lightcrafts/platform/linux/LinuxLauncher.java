/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.linux;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ExceptionDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.splash.SplashImage;
import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.utils.ForkDaemon;
import com.lightcrafts.utils.Version;

import javax.swing.*;

public final class LinuxLauncher {

    public static void main(String[] args) {
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");

            final boolean lafCond = sun.swing.SwingUtilities2.isLocalDisplay();
            Object aaTextInfo = sun.swing.SwingUtilities2.AATextInfo.getAATextInfo(lafCond);
            UIManager.getDefaults().put(sun.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, aaTextInfo);

            System.out.println(
                "This is " +
                Version.getApplicationName() + ' ' +
                Version.getVersionName() + ' ' +
                '(' + Version.getRevisionNumber() + ')'
            );
            if (! TestSSE2.hasSSE2()) {
                TestSSE2.showDialog();
                System.exit(0);
            }
            // Here is how you make a version that expires on a fixed date:
//            boolean expired = ExpirationLogic.showExpirationDialog();
//            if (expired) {
//                System.exit(0);
//            }
            UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

            String revision = Version.getRevisionNumber();
            String name = Version.getVersionName();
            String versionText = "Version " + name + " (" + revision + ")";

            String splashText = versionText;

            // Here is how you make a licensed version:
            splashText = "Open Source";
            SplashImage splash = new SplashImage(splashText);
            SplashWindow.splash(splash);
            Application.setStartupProgress(splash.getStartupProgress());
            ForkDaemon.start();
            Application.main(args);
            SplashWindow.disposeSplash();
        }
        catch (Throwable t) {
            (new ExceptionDialog()).handle(t);
        }
    }
}
