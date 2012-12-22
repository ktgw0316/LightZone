/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.platform.DefaultFileChooser;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.macosx.sheets.SaveExportSheet;
import com.lightcrafts.utils.file.FileUtil;

import java.awt.*;
import java.io.File;

/**
 * A <code>MacOSXFileChooser</code> is-a {@link FileChooser} customized for the
 * Mac&nbsp;OS&nbsp;X look-and-feel.
 */
public class MacOSXFileChooser extends DefaultFileChooser {

    ////////// public /////////////////////////////////////////////////////////

    public synchronized static MacOSXFileChooser getFileChooser() {
        if ( m_fileChooser == null )
            m_fileChooser = new MacOSXFileChooser();
        return m_fileChooser;
    }

    /**
     * {@inheritDoc}
     */
    public ImageExportOptions exportFile( ImageExportOptions options,
                                          Frame parent ) {
        final SaveExportSheet sheet = new SaveExportSheet();
        sheet.showExportAndWait(
            parent,
            //
            // Chop off filename extension so we don't get proposed filenames
            // like "myimage.CRW.jpg".
            //
            new File( FileUtil.trimExtensionOf( options.getExportFile() ) ),
            new ImageExportOptions[]{ options }
        );
        return sheet.getExportOptions();
    }

    /**
     * {@inheritDoc}
     */
    public ImageExportOptions saveFile( ImageExportOptions options,
                                        Frame parent ) {
        final SaveExportSheet sheet = new SaveExportSheet();

        //
        // Chop off filename extension so we don't get proposed filenames like
        // "myimage.CRW.jpg".
        //
        String fileName = FileUtil.trimExtensionOf( options.getExportFile() );

        //
        // Add a suffix so it makes it much harder for a user to overwrite an
        // original JPEG or TIFF file.
        //
        if ( !fileName.endsWith( "_lzn" ) )
            fileName += "_lzn";

        sheet.showSaveAndWait(
            parent, new File( fileName ),
            new ImageExportOptions[]{ options }
        );
        return sheet.getExportOptions();
    }

    // The DefaultFileChooser uses a JFileChooser.  Here we use the
    // heavyweight FileDialog.
    public File chooseDirectory(
        String windowTitle, File directory, Window parent, boolean showHidden
    ) {
        FileDialog dialog;
        if (parent instanceof Frame) {
            dialog = new FileDialog((Frame) parent);
        }
        else if (parent instanceof Dialog) {
            dialog = new FileDialog((Dialog) parent);
        }
        else {
            throw new IllegalArgumentException(
                "chooseDirectory() requires an owning Frame or Dialog"
            );
        }
        if (directory != null) {
            dialog.setDirectory(directory.getAbsolutePath());
        }
        if (windowTitle != null) {
            dialog.setTitle(windowTitle);
        }
        String prop = System.getProperty("apple.awt.fileDialogForDirectories");
        System.setProperty("apple.awt.fileDialogForDirectories", "true");
        dialog.setVisible(true);
        System.setProperty(
            "apple.awt.fileDialogForDirectories", prop != null ? prop : "false"
        );
        String path = dialog.getDirectory();
        String name = dialog.getFile();
        if ((path != null) && (name != null)) {
            return new File(path, name);
        }
        return null;
    }

    ////////// private ////////////////////////////////////////////////////////

    private static MacOSXFileChooser m_fileChooser;

}
/* vim:set et sw=4 ts=4: */
