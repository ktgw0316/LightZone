/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform;

import com.lightcrafts.image.ImageFilenameFilter;
import com.lightcrafts.image.export.ImageExportOptions;
import static com.lightcrafts.platform.Locale.LOCALE;
import com.lightcrafts.ui.export.ExportDialog;
import com.lightcrafts.utils.file.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;

/**
 * This FileChooser is public so it can be used by WindowsPlatform and
 * LinuxPlatform.
 */
public class DefaultFileChooser implements FileChooser {

    @Override
    public File openFile(
        String windowTitle, File directory, Frame parent, FilenameFilter filter
    ) {
        File file = null;
        FileDialog fileDialog = new FileDialog(
            parent, windowTitle, FileDialog.LOAD
        );
        if (directory != null)
            fileDialog.setDirectory(directory.getAbsolutePath());
                if (filter != null) {
            fileDialog.setFilenameFilter(filter);
                }
        fileDialog.setVisible(true);
        String chosenDirectory = fileDialog.getDirectory();
        String chosenFile = fileDialog.getFile();
        if (chosenDirectory != null && chosenFile != null) {
            file = new File(chosenDirectory, chosenFile);
        }
        return file;
    }

    @Override
    public File chooseDirectory(
        String windowTitle, File directory, Window parent, boolean showHidden
    ) {
        JFileChooser chooser = new JFileChooser(directory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(windowTitle);
        int option = chooser.showSaveDialog(parent);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            return file;
        }
        return null;
    }

    @Override
    public ImageExportOptions exportFile(
        ImageExportOptions options, Frame parent
    ) {
        options = ExportDialog.showDialog(options, parent);
        if (options != null) {
            File file = options.getExportFile();
            if ((file != null) && file.exists()) {
                final boolean replace = askToReplace(file, parent);
                if (! replace) {
                    return null;
                }
            }
        }
        return options;
    }

    @Override
    public File saveFile(File file, Frame parent) {
        // Note the initial file name extension, to ensure it's enforced
        // after the dialog.
        String extension = FileUtil.getExtensionOf( file );

        FileDialog fileDialog = new FileDialog(
            parent, LOCALE.get("SaveTitle"), FileDialog.SAVE
        );
        if (file != null) {
            File dir = file.getParentFile();
            if (dir != null) {
                String dirPath = dir.getAbsolutePath();
                fileDialog.setDirectory(dirPath);
            }
            fileDialog.setFile(file.getName());
        }
        fileDialog.setFilenameFilter(ImageFilenameFilter.INSTANCE);
        fileDialog.setVisible(true);
        String dirPath = fileDialog.getDirectory();
        String fileName = fileDialog.getFile();
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
        return null;
    }

    private static boolean askToReplace( File file, Frame parent ) {
        int result = Platform.getPlatform().getAlertDialog().showAlert(
            parent,
            LOCALE.get("ReplaceMessageMajor", file.getName()),
            LOCALE.get("ReplaceMessageMinor"),
            AlertDialog.WARNING_ALERT,
            LOCALE.get("ReplaceMessageReplaceOption"),
            LOCALE.get("ReplaceMessageCancelOption")
        );
        return result == 0;
    }
}
/* vim:set et sw=4 ts=4: */
