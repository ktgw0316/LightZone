/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.CheckForUpdate;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.UpgradeDialog;
import com.lightcrafts.license.LicenseChecker;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.prefs.ApplicationMode;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.WebBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

class HelpMenu extends UpdatableDisposableMenu {

    HelpMenu(final ComboFrame frame) {
        super(frame, "Help");

        JMenuItem menuItem;

        menuItem = MenuFactory.createMenuItem("LightZoneHelp");
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Platform.getPlatform().showHelpTopic(null);
                }
            }
        );
        add(menuItem);

        menuItem = MenuFactory.createMenuItem("VideoLearningCenter");
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final URL url = Version.getVideoLearningCenterURL();
                    WebBrowser.browse(url.toString());
                }
            }
        );
        add(menuItem);

        add(new JSeparator());

        menuItem = MenuFactory.createMenuItem("ProductPage");
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final URL url = Version.getApplicationURL();
                    if (url != null) {
                        WebBrowser.browse(url.toString());
                    }
                }
            }
        );
        add(menuItem);

        menuItem = MenuFactory.createMenuItem("CheckForUpdate");
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CheckForUpdate.checkNowAndWait();
                }
            }
        );
        add(menuItem);

        if (LicenseChecker.getLicenseKey() == null) {
            final JMenuItem buyNowItem =
                MenuFactory.createMenuItem("BuyNow");
            buyNowItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (LicenseChecker.license()) {
                            // License key was entered, remove this menu item.
                            EventQueue.invokeLater(
                                new Runnable() {
                                    public void run() {
                                        remove(buyNowItem);
                                    }
                                }
                            );
                        }
                    }
                }
            );
            add(buyNowItem);
        }
        if (LicenseChecker.isBasic()) {
            menuItem = MenuFactory.createMenuItem("Relicense");
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        UpgradeDialog dialog = new UpgradeDialog(frame);
                        dialog.setVisible(true);
                        boolean bought = dialog.userBought();
                        if (bought) {
                            boolean relicensed = LicenseChecker.relicense();
                            if (relicensed) {
                                ApplicationMode.resetPreference();
                                Application.appModeChanged();
                            }
                        }
                    }
                }
            );
            add(menuItem);
        }

/*
        if (LicenseChecker.getLicenseKey() != null) {
            menuItem = MenuFactory.createMenuItem("DeactivateLicense");
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (LicenseChecker.unlicense()) {
                            System.exit(0);
                        }
                    }
                }
            );
            add(menuItem);
        }
*/

        if (Platform.getType() != Platform.MacOSX) {

            // On the Mac, the "About" item lies under the app menu.

            menuItem = MenuFactory.createMenuItem("About");
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Application.showAbout();
                    }
                }
            );
            add(new JSeparator());
            add(menuItem);
        }
    }
}
