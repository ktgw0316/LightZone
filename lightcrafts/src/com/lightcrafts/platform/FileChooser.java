/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform;

import com.lightcrafts.image.export.ImageExportOptions;
import javafx.stage.FileChooser.ExtensionFilter;

import java.awt.*;
import java.io.File;

public interface FileChooser {

    /**
     * Conduct an open-file dialog.
     * @param windowTitle The title to use for the open-file dialog, or null to
     * get a default title.
     * @param directory A default File for the dialog, or null to get a
     * default directory.
     * @param parent A Frame owner for dialog boxes, or null.
     * @param filter Something to restrict files that will be selectable.
     * @return A chosen File to open, or null to indicate the user cancelled.
     */
    File openFile(
        String windowTitle, File directory, Frame parent, ExtensionFilter... filter
    );

    /**
     * Conduct a file dialog to select a directory.
     * @param windowTitle The title to use for the dialog, or null to
     * get a default title.
     * @param directory A default File for the dialog, or null to get a
     * default directory.
     * @param parent A Frame owner for dialog boxes, or null.
     * @param showHidden If true, show hidden folders in the chooser.
     * @return A chosen selected directory, or null to indicate the user
     * cancelled.
     */
    File chooseDirectory(
        String windowTitle, File directory, Window parent, boolean showHidden
    );

    /**
     * Conduct a save-file dialog, including warning messages for clobbering
     * existing Files.
     * @param file A default File for the dialog, or null to get some default
     * default.
     * @param parent A Frame owner for dialog boxes, or to get a dialog
     * centered on screen.
     * @return A chosen File to save in, or null to indicate the user
     * cancelled.
     */
    File saveFile(File file, Frame parent);

    /**
     * Conduct an export-file dialog, including warning messages for clobbering
     * existing Files.
     * @param options Some default ImageExportOptions to use to initialize the
     * dialog's controls.
     * @param parent A Frame owner for dialog boxes, or null.
     * @return Some ImageExportOptions selected by the user, or null to
     * indicate the user cancelled.
     */
    ImageExportOptions exportFile( ImageExportOptions options, Frame parent );
}
/* vim:set et sw=4 ts=4: */
