/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.export.ExportNameUtility;
import lombok.val;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

/**
 * A collection of static methods for file actions accessed through an
 * AbstractImageBrowser, such as delete and rename, and the associated
 * dialogs to ask about overwrite and to report errors.
 */
class FileActions {

    static void deleteFiles(File[] files, Component parent) {
        AlertDialog alert = Platform.getPlatform().getAlertDialog();
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(
            Frame.class, parent
        );
        StringBuilder info = new StringBuilder();
        String message;
        if (files.length > 1) {
            message = LOCALE.get("DeleteQuestionPlural", Integer.toString(files.length));
        }
        else {
            message = LOCALE.get("DeleteQuestionSingular", files[0].getName());
        }
        String okButton = LOCALE.get("DeleteButton");
        String cancelButton = LOCALE.get("CancelButton");
        int option = alert.showAlert(
            frame, message, info.toString(),
            AlertDialog.WARNING_ALERT,
            okButton, cancelButton
        );
        if (option != 0) return;

        val desktop = getAwtDesktop();

        for (File file : files) {
            boolean deleted = desktop.map(d -> d.moveToTrash(file)).orElse(false);
            if (! deleted) {
                deleted = file.delete();
            }
            if (! deleted) {
                String error =
                    LOCALE.get("DeleteFailed", file.getName());
                option = alert.showAlert(
                    frame, error, "", AlertDialog.ERROR_ALERT,
                    LOCALE.get( "ContinueButton" ),
                    LOCALE.get( "CancelButton" )
                );
                if (option > 0) {
                    break;
                }
            }
        }
    }

    private static Optional<Desktop> getAwtDesktop() {
        if (! Desktop.isDesktopSupported())
            return Optional.empty();
        val desktop = Desktop.getDesktop();
        return desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)
                ? Optional.of(desktop)
                : Optional.empty();
    }

    static void renameFile(File file, Component parent) {
        String suffix = ExportNameUtility.getFileExtension(file);

        String oldName = ExportNameUtility.trimFileExtension(file.getName());

        File defaultFile = ExportNameUtility.ensureNotExists(file);
        defaultFile = ExportNameUtility.trimFileExtension(defaultFile);

        String defaultName = defaultFile.getName();

        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(
            Frame.class, parent
        );
        String name = JOptionPane.showInputDialog(
            frame, LOCALE.get("RenamePrompt", oldName), defaultName
        );
        if (name != null) {

            File dir = file.getParentFile();
            File newFile = new File(dir, name);
            newFile = ExportNameUtility.setFileExtension(newFile, suffix);

            if (newFile.isDirectory()) {
                JOptionPane.showMessageDialog(
                    frame, LOCALE.get("RenameFailedDirectory", name)
                );
                return;
            }
            if (newFile.exists()) {
                int option = JOptionPane.showConfirmDialog(
                    frame, LOCALE.get("ClobberPrompt", name)
                );
                if (option != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            boolean fileRenamed = false;
            boolean xmpFileExists = false;
            boolean xmpFileRenamed = false;
            try {
                File xmpFile = getXmpFile(file);
                ImageInfo.closeAll();
                fileRenamed = file.renameTo(newFile);
                xmpFileExists = xmpFile.isFile();
                if (fileRenamed && xmpFileExists) {
                    File newXmpFile = getXmpFile(newFile);
                    ImageInfo.closeAll();
                    xmpFileRenamed = xmpFile.renameTo(newXmpFile);
                }
            }
            catch (Throwable t) {
                System.out.println("File rename failed");
                t.printStackTrace();
            }
            if (! fileRenamed) {
                String error = LOCALE.get(
                    "RenameError", file.getName(), newFile.getName()
                );
                JOptionPane.showMessageDialog(
                    frame,
                    error,
                    LOCALE.get("RenameErrorTitle"),
                    JOptionPane.ERROR_MESSAGE
                );
            }
            else if (xmpFileExists && (! xmpFileRenamed)) {
                if (fileRenamed) {
                    boolean fileUnRenamed = newFile.renameTo(file);
                    if (! fileUnRenamed) {
                        System.out.println("Failed to undo file rename");
                    }
                }
                String error = LOCALE.get("XmpRenameError", file.getName());
                JOptionPane.showMessageDialog(
                    frame,
                    error,
                    LOCALE.get("RenameErrorTitle"),
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private static File getXmpFile(File file) {
        ImageInfo info = ImageInfo.getInstanceFor(file);
        String name = info.getXMPFilename();
        return new File(name);
    }
}
