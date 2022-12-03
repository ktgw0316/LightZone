/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.toolkit.TextAreaFactory;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.image.ImageFilenameFilter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

// Show dialogs to get user guidance about how to find the image file for a
// Document, in case the Document's file pointers aren't valid.

class DocumentImageSelector {

    // For file choosers:
    private static Platform Env = Platform.getPlatform();

    // The object of all the choosers:
    private static File file;

    // Pick an image file from among an array of alternatives, or move on
    // to a file chooser.  If the user cancels, return null.
    static File chooseImageFile(
        File docFile,
        final File oldImageFile,
        File[] imageFiles,
        final File chooserFile,
        final Frame parent
    ) {
        if (imageFiles.length == 0) {
            return chooseImageFile(docFile, oldImageFile, chooserFile, parent);
        }
        if (imageFiles.length == 1) {
            return chooseImageFile(
                docFile, oldImageFile, imageFiles[0], chooserFile, parent
            );
        }
        // It's definitely multiple-choice.
        String bigText = LOCALE.get(
            "ImageSelectorMessage1Major",
            docFile.getName(),
            oldImageFile.getAbsolutePath()
        );
        String smallText = LOCALE.get("ImageSelectorMessage1Minor");

        JTextArea bigTextComp = TextAreaFactory.createTextArea(bigText, 30);
        bigTextComp.setFont(bigTextComp.getFont().deriveFont(Font.BOLD));
        JTextArea smallTextComp = TextAreaFactory.createTextArea(smallText, 30);

        Object[] listEntries = new Object[imageFiles.length];
        for (int n=0; n<imageFiles.length; n++) {
            // An anonymous inner class to control JList rendering:
            File file = new File(imageFiles[n], "") {
                public String toString() {
                    return getAbsolutePath();
                }
            };
            listEntries[n] = file;
        }
        final JList list = new JList(listEntries);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(BorderFactory.createLineBorder(Color.black));

        final JButton useThis =
            new JButton(LOCALE.get("ImageSelector1UseOption"));
        final JButton search =
            new JButton(LOCALE.get("ImageSelectorSearchOption"));
        final JButton cancel =
            new JButton(LOCALE.get("ImageSelectorCancelOption"));

        list.addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent event) {
                    Object selection = list.getSelectedValue();
                    useThis.setEnabled(selection != null);
                }
            }
        );
        useThis.setEnabled(false);

        final String findTitle = LOCALE.get(
            "ImageSelectorFindDialogTitle", oldImageFile.getName()
        );
        final JDialog dialog = new JDialog(parent, findTitle, true);
        dialog.setResizable(false);

        useThis.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    file = (File) list.getSelectedValue();
                    dialog.dispose();
                }
            }
        );
        search.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    FileChooser chooser = Env.getFileChooser();
                    file = chooser.openFile(
                        findTitle, chooserFile, parent,
                        ImageFilenameFilter.INSTANCE
                    );
                    if (file != null) {
                        dialog.dispose();
                    }
                }
            }
        );
        cancel.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    file = null;
                    dialog.dispose();
                }
            }
        );
        JPanel contents = new JPanel();
        contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
        contents.add(bigTextComp);
        contents.add(Box.createVerticalStrut(6));
        contents.add(smallTextComp);
        contents.add(Box.createVerticalStrut(6));
        contents.add(list);
        contents.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        bigTextComp.setBackground(contents.getBackground());
        smallTextComp.setBackground(contents.getBackground());

        JOptionPane option =
            new JOptionPane(contents, JOptionPane.QUESTION_MESSAGE);
        option.setOptions(new Object[] {cancel, search, useThis});

        dialog.add(option);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return file;
    }

    // Ask if a given image file is OK, and if it's not, then move on to a
    // file chooser.
    static File chooseImageFile(
        File docFile,
        File oldImageFile,
        File newImageFile,
        File chooserFile,
        Frame parent
    ) {
        // Is this the right one?
        int result = Env.getAlertDialog().showAlert(
            parent,
            LOCALE.get(
                "ImageSelectorMessage2Major",
                docFile.getName(),
                oldImageFile.getName(),
                newImageFile.getAbsolutePath()
            ),
            LOCALE.get("ImageSelectorMessage2Minor"),
            AlertDialog.ERROR_ALERT,
            LOCALE.get("ImageSelector2UseOption", newImageFile.getName()),
            LOCALE.get("ImageSelectorSearchOption"),
            LOCALE.get("ImageSelectorCancelOption")
        );
        if (result == 0) {  // Use this image
            file = newImageFile;
        }
        else if (result == 1) { // Search for the image
            FileChooser chooser = Env.getFileChooser();
            String title = LOCALE.get(
                "ImageSelectorFindDialogTitle", oldImageFile.getName()
            );
            file = chooser.openFile(
                title, chooserFile, parent, ImageFilenameFilter.INSTANCE
            );
        }
        else {
            file = null;    // Cancel
        }
        return file;
    }

    // Pick an image file in a file chooser.
    static File chooseImageFile(
        File docFile,
        File oldImageFile,
        File chooserFile,
        Frame parent) {
        // Would you like to search for it?
        int result = Env.getAlertDialog().showAlert(
            parent,
            LOCALE.get(
                "ImageSelectorMessage3Major",
                docFile.getName(),
                oldImageFile.getName()
            ),
            LOCALE.get("ImageSelectorMessage3Minor"),
            AlertDialog.ERROR_ALERT,
            LOCALE.get("ImageSelectorSearchOption"),
            LOCALE.get("ImageSelectorCancelOption")
        );
        if (result == 0) {  // Yes, please search
            FileChooser chooser = Env.getFileChooser();
            String title = LOCALE.get(
                "ImageSelectorFindDialogTitle", oldImageFile.getName()
            );
            file = chooser.openFile(
                title, chooserFile, parent, ImageFilenameFilter.INSTANCE
            );
        }
        else {
            file = null;    // Cancel
        }
        return file;
    }

    public static void main(String[] args) {
        File docFile = new File("/DocumentFile.lzn");
        File oldImageFile = new File("/OldImage.tif");
        File[] files = new File[] {
            new File("a"), new File("b"), new File("c")
        };
        File newImageFile = DocumentImageSelector.chooseImageFile(
            docFile, oldImageFile, files, null, null
        );
        System.out.println(newImageFile);

        System.exit(0);
    }
}
