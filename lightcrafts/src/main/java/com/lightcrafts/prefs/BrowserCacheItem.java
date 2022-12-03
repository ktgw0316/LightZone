/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.utils.filecache.FileCacheFactory;
import com.lightcrafts.utils.filecache.FileCache;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.AlertDialog;

import static com.lightcrafts.prefs.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.util.prefs.Preferences;
import java.io.IOException;

class BrowserCacheItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/utils/filecache";
    private final static String Key = "CacheScope";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JRadioButton localButton;
    private JRadioButton globalButton;
    private ButtonGroup group;

    private JButton clearButton;

    BrowserCacheItem(JTextArea help) {
        super(help);
        localButton = new JRadioButton(
            LOCALE.get("BrowserCacheItemLocalOption")
        );
        globalButton = new JRadioButton(
            LOCALE.get("BrowserCacheItemGlobalOption")
        );
        localButton.setFocusable(false);
        globalButton.setFocusable(false);
        group = new ButtonGroup();
        group.add(localButton);
        group.add(globalButton);

        clearButton = new ClearButton();

        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("BrowserCacheItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("BrowserCacheItemHelp");
    }

    public boolean requiresRestart() {
        return false;
    }

    public JComponent getComponent() {
        Box box = Box.createHorizontalBox();
        box.add(localButton);
        box.add(globalButton);
        box.add(Box.createHorizontalGlue());
        box.add(clearButton);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    public void commit() {
        Prefs.putBoolean(Key, localButton.isSelected());
    }

    public void restore() {
        boolean local = Prefs.getBoolean(Key, false);
        localButton.setSelected(local);
        globalButton.setSelected(! local);
    }

    class ClearButton extends JButton implements ActionListener {

        ClearButton() {
            super(LOCALE.get("BrowserCacheItemClearButton"));
            setToolTipText(LOCALE.get("BrowserCacheItemClearToolTip"));
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event) {
            Frame parent = (Frame) SwingUtilities.getAncestorOfClass(
                Frame.class, this
            );
            AlertDialog alert = Platform.getPlatform().getAlertDialog();
            int option = alert.showAlert(
                parent,
                LOCALE.get("BrowserCacheItemClearWarningMajor"),
                LOCALE.get("BrowserCacheItemClearWarningMinor"),
                AlertDialog.WARNING_ALERT,
                LOCALE.get( "BrowserCacheItemClearOption" ),
                LOCALE.get( "BrowserCacheItemCancelOption" )
            );
            if (option != 0) {
                return;
            }
            try {
                FileCache cache = FileCacheFactory.getGlobalCache();
                if (cache != null) {
                    cache.clear();
                }
                else {
                    throw new IOException("Global cache does not exist");
                }
            }
            catch (IOException e) {
                alert.showAlert(
                    parent,
                    LOCALE.get("BrowserCacheItemError"),
                    e.getMessage(),
                    AlertDialog.ERROR_ALERT,
                    new String[] { LOCALE.get("BrowserCacheItemErrorButton") }
                );
                System.out.println("Error clearing the global browser cache:");
                e.printStackTrace();
            }
        }
    }
}
