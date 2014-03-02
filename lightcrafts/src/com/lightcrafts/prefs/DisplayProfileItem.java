/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;

import static com.lightcrafts.prefs.Locale.LOCALE;

import javax.swing.*;
import java.awt.color.ICC_Profile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

class DisplayProfileItem extends PreferencesItem implements ActionListener {

    private final static String Package = "/com/lightcrafts/platform/linux";
    private final static String Key = "DisplayProfile";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JTextField text;
    private JButton chooserButton;

    DisplayProfileItem(JTextArea help) {
        super(help);

        text = new JTextField(20);
        text.setEditable(false);

        chooserButton = new JButton("Choose");
        chooserButton.setToolTipText(
            "Pick a new color profile for this display"
        );
        chooserButton.addActionListener(this);

        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("DisplayProfileItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("DisplayProfileItemHelp");
    }

    public boolean requiresRestart() {
        return true;
    }

    public JComponent getComponent() {
        Box box = Box.createHorizontalBox();
        box.add(text);
        box.add(Box.createHorizontalStrut(6));
        box.add(chooserButton);
        return box;
    }

    public void commit() {
        String path = text.getText();
        Prefs.put(Key, path);
    }

    public void restore() {
        String path = Prefs.get(Key, null);
        if (path != null) {
            text.setText(path);
            text.setEnabled(true);
        }
        else {
            text.setText("(sRGB default)");
            text.setEnabled(false);
        }
    }

    // Conduct the dialog to accept a new color profile.
    public void actionPerformed(ActionEvent event) {
        FileChooser chooser = Platform.getPlatform().getFileChooser();
        AlertDialog alert = Platform.getPlatform().getAlertDialog();
        String path;
        if (text.isEnabled()) {
            path = text.getText();
        }
        else {
            path = System.getProperty("user.home");
        }
        File file = new File(path);
        file = chooser.openFile(
            "Display Color Profile", file, null, null
        );
        if (file != null) {
            if (! file.isFile()) {
                alert.showAlert(
                    null,
                    '\"' + file.getName() + "\" is not a file.",
                    "A color profile is a file, " +
                    "usually ending with \".icc\" or \".icm\".",
                    AlertDialog.WARNING_ALERT,
                    "OK"
                );
                return;
            }
            try {
                ICC_Profile.getInstance(file.getAbsolutePath());
            }
            catch (IOException e) {
                alert.showAlert(
                    null,
                    '\"' + file.getName() + "\" could not be read.",
                    e.getClass().getName() + ": " + e.getMessage(),
                    AlertDialog.WARNING_ALERT,
                    "OK"
                );
                return;
            }
            catch (IllegalArgumentException e) {
                alert.showAlert(
                    null,
                    '\"' + file.getName() + "\" is not a valid color profile.",
                    e.getClass().getName() + ": " + e.getMessage(),
                    AlertDialog.WARNING_ALERT,
                    "OK"
                );
                return;
            }
            path = file.getAbsolutePath();
            text.setText(path);
            text.setEnabled(true);
            alert.showAlert(
                null,
                "Display color profile \"" + file.getName() + "\" accepted.",
                "You must now restart LightZone for your new display " +
                "profile to take effect.",
                AlertDialog.WARNING_ALERT,
                "OK"
            );
        }
    }
}
