/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.types.*;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.LightCraftsException;
import com.lightcrafts.utils.TextUtil;

/**
 * A <code>LightroomApplication</code> is-an {@link OtherApplication} for
 * Adobe's Photoshop Lightroom.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LightroomApplication extends OtherApplication {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public ImageFileExportOptions createExportOptions( ImageInfo imageInfo,
                                                       Dimension saveSize )
        throws IOException, LightCraftsException
    {
        final ImageType t = imageInfo.getImageType();
        final ImageFileExportOptions options;
        if ( t instanceof JPEGImageType ) {
            //
            // The only case when Lightroom gives us a JPEG is when (a) the
            // original is a JPEG and (b) the user selects "Edit a Copy".
            //
            // If user selects "Edit a Copy with Lightroom Adjustments", then
            // we always get a TIFF even if the original is a JPEG.
            //
            options = SidecarJPEGImageType.INSTANCE.newExportOptions();
        } else if ( t instanceof RawImageType ) {
            //
            // The only case when Lightroom gives us a raw is when (a) the
            // original is a raw/TIFF and (b) the user selects "Edit a Copy".
            // This is assumed to be a bug in Lightroom.
            //
            // If user selects "Edit a Copy with Lightroom Adjustments", then
            // we always get a TIFF even if the original is raw.
            //
            options = SidecarTIFFImageType.INSTANCE.newExportOptions();
        } else
            options = MultipageTIFFImageType.INSTANCE.newExportOptions();
        initOptions( options, imageInfo, saveSize );
        return options;
    }

    /**
     * Gets the original file for the file currently being edited.  Examples:
     *  <blockquote>
     *    <table cellpadding="0" cellspacing="0">
     *      <tr>
     *        <th align="left">Current</th>
     *        <th>&nbsp;&nbsp;&nbsp;</th>
     *        <th align="left">Original</th>
     *      </tr>
     *      <tr>
     *        <td><code>foo-Edit.tif</code></td>
     *        <td>&nbsp;&nbsp;&nbsp;</td>
     *        <td><code>foo.tif</code></td>
     *      </tr>
     *      <tr>
     *        <td><code>foo-Edit-1.tif</code></td>
     *        <td>&nbsp;&nbsp;&nbsp;</td>
     *        <td><code>foo-Edit.tif</code></td>
     *      </tr>
     *      <tr>
     *        <td><code>foo-Edit-2.tif</code></td>
     *        <td>&nbsp;&nbsp;&nbsp;</td>
     *        <td><code>foo-Edit-1.tif</code></td>
     *      </tr>
     *    </table>
     *  </blockquote>
     *
     * @param currFile The image {@link File} currently being edited.
     * @return Returns said {@link File} or <code>currFile</code> if there is
     * no original file or it could not be determined.
     */
    public static File getOriginalFile( File currFile ) {
        final Matcher m = COPY_PATTERN.matcher(
            TextUtil.normalize( currFile.getAbsolutePath() )
        );
        if ( !m.matches() )
            return currFile;

        final StringBuilder sb = new StringBuilder();
        sb.append( m.group(1) );

        final String number = m.group(3);
        if ( number != null ) {
            try {
                final int n = - Integer.parseInt( number );
                if ( n > 1 ) {
                    final String editWord = m.group(2);
                    sb.append( '-' );
                    sb.append( editWord );
                    sb.append( '-' );
                    sb.append( n - 1 );
                }
            }
            catch ( NumberFormatException e ) {
                // ignore
            }
        }

        sb.append( ".tmp" );
        String origFilename = sb.toString();

        final String ext = FileUtil.getExtensionOf( currFile );
        final boolean isJPEG =
            "jpg".equalsIgnoreCase( ext ) || "jpeg".equalsIgnoreCase( ext );

        origFilename = FileUtil.replaceExtensionOf(
            origFilename, isJPEG ? "jpg" : "tif"
        );
        File origFile = new File( origFilename );
        if ( origFile.exists() )
            return origFile;

        origFilename = FileUtil.replaceExtensionOf(
            origFilename, isJPEG ? "jpeg" : "tiff"
        );
        origFile = new File( origFilename );
        if ( origFile.exists() )
            return origFile;

        return currFile;
    }

    /**
     * {@inheritDoc}
     */
    public void postSave( File imageFile, boolean didSaveDirectly,
                          boolean openPending ) {
        if ( !openPending )
            Platform.getPlatform().bringAppToFront( "Adobe Lightroom" );
    }

    /**
     * {@inheritDoc}
     */
    public boolean shouldSaveDirectly( ImageInfo imageInfo ) {
        final File file = imageInfo.getFile();
        final String filename = TextUtil.normalize( file.getName() );
        if ( !COPY_PATTERN.matcher( filename ).matches() )
            return false;

        try {
            final ImageType t = imageInfo.getImageType();
            if ( t instanceof TIFFImageType )
                return true;
            //
            // If the original is either a JPEG or a raw, then we're forced to
            // save a sidecar JPEG/TIFF file which means we have to be able to
            // find the original file.
            //
            final File origFile = getOriginalFile( file );
            return origFile != null && !origFile.equals( file );
        }
        catch ( IOException e ) {
            // ignore
        }
        catch ( LightCraftsException e ) {
            // ignore
        }
        return false;
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance. */
    static final LightroomApplication INSTANCE = new LightroomApplication();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>LightroomApplication</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private LightroomApplication() {
        super( "Lightroom" );
    }

    /**
     * The {@link Pattern} used to detect whether we're editing a copy of an
     * original image or the original itself.
     */
    private static final Pattern COPY_PATTERN;

    /**
     * The localized words Lightroom appends to a copy of the original file.
     */
    private static final String[] EDIT_WORDS = {
        "Edit",                         // English
        "Modifie\u0301",                // French
        "Bearbeiten",                   // German
    };

    static {
        COPY_PATTERN = Pattern.compile(
            "^(.*)-(" + TextUtil.join( EDIT_WORDS, "|" )
            + ")(-\\d+)?\\.([jJ][pP][eE]?[gG]|[tT][iI][fF][fF]?)$"
        );
    }
}
/* vim:set et sw=4 ts=4: */
