/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.linux;

import com.lightcrafts.platform.DefaultFileChooser;
import com.lightcrafts.utils.file.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;

class LinuxFileChooser extends DefaultFileChooser {

    /**
     * Use JFileChooser for openFile() on Linux.  (FileDialog is used in
     * DefaultFileChooser, resulting in a Motif L&F on Linux.)
     */ 
    public File openFile(
        String windowTitle, File directory, Frame parent, FilenameFilter filter
    ) {
        File file = null;
        JFileChooser chooser = new JFileChooser(directory);
        if (windowTitle != null) {
            chooser.setDialogTitle(windowTitle);
        }
        int result = chooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
        }
        return file;
    }

    /**
     * Use JFileChooser for saveFile() on Linux.  (FileDialog is used in
     * DefaultFileChooser, resulting in a Motif L&F on Linux.)
     */
    public File saveFile(File file, Frame parent) {
        // Note the initial file name extension, to ensure it's enforced
        // after the dialog.
        String extension = FileUtil.getExtensionOf( file );

        JFileChooser chooser = new JFileChooser(file);
        chooser.setSelectedFile(file);
        int result = chooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            String dirPath = file.getParent();
            String fileName = file.getName();
            if ((dirPath != null) && (fileName != null)) {
                // Ensure the file name extension is not modified.
                File newFile = new File(dirPath, fileName);
                String newExtension = FileUtil.getExtensionOf(newFile);
                if (newExtension == null) {
                    return new File(newFile.getAbsolutePath() + "." + extension);
                }
                if (! newExtension.equals(extension)) {
                    String path = FileUtil.replaceExtensionOf(newFile, extension);
                    return new File(path);
                }
                return newFile;
            }
        }
        return null;
    }
}
