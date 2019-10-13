/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.CheckForUpdate;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.WebBrowser;

import javax.swing.*;
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
        menuItem.setEnabled(CheckForUpdate.isEnabled());
        add(menuItem);

        if (!Platform.isMac()) {
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
