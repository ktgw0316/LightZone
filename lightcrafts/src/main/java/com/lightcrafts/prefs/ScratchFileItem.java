/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;

import static com.lightcrafts.prefs.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

class ScratchFileItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/jai/utils";
    private final static String Key = "ScratchDirectory";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JTextField text;
    private JButton chooserButton;

    ScratchFileItem(JTextArea help) {
        super(help);

        text = new JTextField();
        text.setEditable(false);

        chooserButton = new JButton(LOCALE.get("ScratchFileItemButton"));
        chooserButton.setToolTipText(LOCALE.get("ScratchFileItemToolTip"));

        chooserButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    FileChooser chooser =
                        Platform.getPlatform().getFileChooser();
                    String path = text.getText();
                    File dir = new File(path);
                    Window window = (Window) SwingUtilities.getAncestorOfClass(
                        Window.class, chooserButton
                    );
                    dir = chooser.chooseDirectory(
                        LOCALE.get("ScratchFileDialogTitle"), dir, window, true
                    );
                    if (dir != null) {
                        if (! dir.isDirectory()) {
                            dir = dir.getParentFile();
                        }
                        if (dir != null) {
                            path = dir.getAbsolutePath();
                            text.setText(path);
                        }
                    }
                }
            }
        );
        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("ScratchFileItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("ScratchFileItemHelp");
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
        // Don't use the regular Preferences default argument, to minimize
        // calls to getDefaultTempPath().
        String path = Prefs.get(Key, null);
        if (path == null) {
            path = getDefaultTempPath();
        }
        if (path != null) {
            text.setText(path);
        }
        else {
            text.setEnabled(false);
        }
    }

    // Create and delete a temp file, to discover the temp directory.
    private static String getDefaultTempPath() {
        try {
            File tempFile = File.createTempFile("lzScratchProbe", "");
            File tempDir = tempFile.getParentFile();
            tempFile.delete();
            if (tempDir != null) {
                return tempDir.getAbsolutePath();
            }
            return null;
        }
        catch (IOException e) {
            return null;
        }
    }
}
