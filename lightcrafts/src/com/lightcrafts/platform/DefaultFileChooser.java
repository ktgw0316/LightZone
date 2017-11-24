/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.platform;

import com.lightcrafts.image.ImageFilenameFilter;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.ui.export.ExportDialog;
import com.lightcrafts.utils.file.FileUtil;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import java.awt.*;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static com.lightcrafts.platform.Locale.LOCALE;

/**
 * This FileChooser is public so it can be used by WindowsPlatform and
 * LinuxPlatform.
 */
public class DefaultFileChooser implements com.lightcrafts.platform.FileChooser {

    // Initializer for JavaFX
    @SuppressWarnings("unused")
    static private JFXPanel fxPanel = new JFXPanel();

    @Override
    public File openFile(
        final String windowTitle, final File directory, Frame parent, final ExtensionFilter... filter
    ) {
        final FutureTask<File> query = new FutureTask<File>(new Callable<File>() {
            @Override
            public File call() throws Exception {
                final FileChooser chooser = new FileChooser();
                if (windowTitle != null) {
                    chooser.setTitle(windowTitle);
                }
                chooser.setInitialDirectory(directory);
                if (filter != null) {
                    chooser.getExtensionFilters().addAll(filter);
                }
                return chooser.showOpenDialog(null);
            }
        });
        Platform.runLater(query);

        try {
            return query.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public File chooseDirectory(
        final String windowTitle, final File directory, Window parent, final boolean showHidden
    ) {
        final FutureTask<File> query = new FutureTask<File>(new Callable<File>() {
            @Override
            public File call() throws Exception {
                final DirectoryChooser chooser = new DirectoryChooser();
                if (windowTitle != null) {
                    chooser.setTitle(windowTitle);
                }
                chooser.setInitialDirectory(directory);
                return chooser.showDialog(null);
            }
        });
        Platform.runLater(query);

        try {
            return query.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
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
        int result = com.lightcrafts.platform.Platform.getPlatform().getAlertDialog().showAlert(
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
