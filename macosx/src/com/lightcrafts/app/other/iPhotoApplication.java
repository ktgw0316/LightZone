/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.types.*;
import com.lightcrafts.platform.macosx.AppleScript;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.LightCraftsException;

/**
 * An <code>iPhotoApplication</code> is-an {@link OtherApplication} for Apple's
 * iPhoto.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class iPhotoApplication extends MacApplication {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public ImageFileExportOptions createExportOptions( ImageInfo imageInfo,
                                                       Dimension saveSize )
        throws IOException, LightCraftsException
    {
        final ImageFileExportOptions options;
        final ImageType t = imageInfo.getImageType();
        final String ext;
        if ( t instanceof JPEGImageType ) {
            options = SidecarJPEGImageType.INSTANCE.newExportOptions();
            ext = t.getExtensions()[0];
        } else {                        // TIFF or raw
            options = SidecarTIFFImageType.INSTANCE.newExportOptions();
            ext = TIFFImageType.INSTANCE.getExtensions()[0];
        }
        initOptions( options, imageInfo, saveSize );

        //
        // Since iPhoto gives us the original file (which may be raw), we need
        // to replace the filename extension since we don't save as raw.
        //
        // For non-raw files, this normalizes the filename extension, e.g.,
        // "jpeg" becomes "jpg" and "tiff" becomes "tif".
        //
        String saveFileName =
            FileUtil.replaceExtensionOf( imageInfo.getFile(), ext );

        //
        // Insert (if necessary) a suffix to distinguish sidecar JPEG/TIFF
        // files from ordinary JPEG/TIFF files.
        //
        saveFileName = FileUtil.insertSuffix( saveFileName, "_lzn" );

        //
        // We have to save the file to /tmp rather than in the same folder as
        // the original because iPhoto refuses to import any file that's
        // already within its directory structure.
        //
        File saveFile = new File( saveFileName );
        saveFile = new File( FileUtil.getTempDir(), saveFile.getName() );
        saveFile = FileUtil.getNoncollidingFileFor( saveFile );
        options.setExportFile( saveFile );

        //
        // Because iPhoto copies the file on import (by default), we can mark
        // the saved file to be deleted on exit.
        //
        saveFile.deleteOnExit();

        return options;
    }

    /**
     * {@inheritDoc}
     */
    public void postSave( File imageFile, boolean didSaveDirectly,
                          boolean openPending ) {
        if ( didSaveDirectly )
            AppleScript.run( getImportScriptFor( imageFile, !openPending ) );
        else if ( !openPending )
            AppleScript.bringAppToFront( getName() );
    }

    /**
     * {@inheritDoc}
     */
    public boolean shouldSaveDirectly( ImageInfo imageInfo ) {
        return isIntegrationEnabled();
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance. */
    static final iPhotoApplication INSTANCE = new iPhotoApplication();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>iPhotoApplication</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private iPhotoApplication() {
        super( "iPhoto", "iPho" );
    }

    /**
     * Gets an iPhoto AppleScript to import the given image file into its
     * current album.
     *
     * @param imageFile The image {@link File} to tell iPhoto to import.
     * @param activate If <code>true</code>, activate iPhoto.
     */
    private static String getImportScriptFor( File imageFile,
                                              boolean activate ) {
        final StringBuilder sb = new StringBuilder();
        sb.append( "set unixPath to \"" );
        sb.append( imageFile.getAbsolutePath() );
        sb.append( "\"\n" );
        sb.append(
            "set thePhoto to POSIX file unixPath as alias\n" +
            "tell application \"iPhoto\"\n"
        );
        if ( activate )
            sb.append( "    activate\n" );
        sb.append(
            "    import from thePhoto to current album\n" +
            "end tell"
        );
        return sb.toString();
    }

}
/* vim:set et sw=4 ts=4: */
