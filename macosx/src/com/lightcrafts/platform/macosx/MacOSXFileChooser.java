/*
 * Copyright (c) 2021. Masahiro Kitagawa
 */

package com.lightcrafts.platform.macosx;

import com.lightcrafts.platform.DefaultFileChooser;

import java.awt.*;
import java.io.File;

public class MacOSXFileChooser extends DefaultFileChooser {
    @Override
    public File chooseDirectory(
            String windowTitle, File directory, Window parent, boolean showHidden
    ) {
        final FileDialog fd;
        if (parent instanceof Frame) {
            fd = new FileDialog((Frame) parent, windowTitle);
        } else if (parent instanceof Dialog) {
            fd = new FileDialog((Dialog) parent, windowTitle);
        } else {
            fd = new FileDialog((Frame) null, windowTitle);
        }

        File selectedFile = null;
        try {
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            if (directory != null) {
                fd.setDirectory(directory.getAbsolutePath());
            }
            fd.setVisible(true);
            final String dir = fd.getDirectory() + fd.getFile();
            selectedFile = new File(dir);
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
        }
        return selectedFile;
    }
}
