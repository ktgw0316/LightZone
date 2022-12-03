/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.export.*;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.providers.ResolutionProvider;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.RawImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.image.color.ColorProfileInfo;

import java.awt.*;
import java.awt.color.ICC_Profile;
import java.io.File;

/**
 * This is the home of all the special-case logic that goes into defining
 * default ImageExportOptions for edited images.
 * <p>
 * In general, the settings used to initialize an export dialog depend on:
 * <ul>
 *   <li>The original image (color space, image type)</li>
 *   <li>Edits made by the user (crop bounds)</li>
 *   <li>User prefences (workflow settings, default directory)</li>
 *   <li>Export settings previously used for the current image</li>
 *   <li>The most recent export settings used</li>
 *   <li>The most recent export settings used to export each image type (in
 *   case the user switches image types during the dialog)</li>
 * </ul>
 */
public class ExportLogic {

    /**
     * Figure out new export options when there is no export history
     * whatsoever.
     */
    public static ImageExportOptions getDefaultExportOptions(
        ImageMetadata meta,
        Dimension size
    ) {
        ImageExportOptions options = TIFFImageType.INSTANCE.newExportOptions();
        File file = meta.getFile();
        options.setExportFile(file);
        colorProfileFrom(meta, (ImageFileExportOptions)options);
        resolutionFrom(meta, (ImageFileExportOptions)options);
        maybeUpdateSize(options, size);
        return options;
    }

    /**
     * Figure out new export options when export has been performed at least
     * once but the image being exported has never been saved or exported
     * before.
     */
    public static ImageExportOptions getDefaultExportOptions(
        ImageExportOptions recent,
        ImageMetadata meta,
        Dimension size
    ) {
        String name = meta.getFile().getName();
        return getDefaultExportOptions(recent, meta, size, name);
    }

    /**
     * Figure out new export options when export has been performed at least
     * once, the image being exported has never been exported before, but
     * a base name for the export is known anyway, for instance from saving.
     */
    public static ImageExportOptions getDefaultExportOptions(
        ImageExportOptions recent,
        ImageMetadata meta,
        Dimension size,
        String name
    ) {
        ImageExportOptions options = recent;
        File recentFile = recent.getExportFile();
        File dir = null;
        if (recentFile != null) {
            dir = recentFile.getParentFile();
        }
        if (dir == null) {
            // This sometimes happens: an ImageExportOptions with a null file.
            // I don't know how, maybe very old export options saved in old
            // LZN files or in preferences.
            dir = new File(System.getProperty("user.home"));
        }
        File currentFile = new File(dir, name);
        options.setExportFile(currentFile);
        if (options instanceof ImageFileExportOptions) {
            colorProfileFrom(meta, (ImageFileExportOptions) options);
            resolutionFrom(meta, (ImageFileExportOptions)options);
        }
        maybeUpdateSize(options, size);
        return options;
    }

    /**
      * Figure out new ImageExportOptions when the image being exported
      * has been exported before.
      */
     public static ImageExportOptions getDefaultExportOptions(
         ImageExportOptions current,
         Dimension size
     ) {
        maybeUpdateSize(current, size);
        return current;
    }

    /**
     * If the given export options hold an original size unequal to the given
     * size, then slew its original size values and also its resize values
     * to the given size.
     */
    public static void maybeUpdateSize(
        ImageExportOptions options, Dimension size
    ) {
        if ( options.originalWidth.getValue() != size.width ||
             options.originalHeight.getValue() != size.height ) {
            options.originalWidth.setValue( size.width );
            options.originalHeight.setValue( size.height );
            if ( options instanceof ImageFileExportOptions ) {
                final ImageFileExportOptions fileOptions =
                    (ImageFileExportOptions)options;
                fileOptions.resizeWidth.setValue( size.width );
                fileOptions.resizeHeight.setValue( size.height );
            }
        }
    }

    /**
     * Figure out new ImageExportOptions when a user asks to change the export
     * image type and so old options must be merged into new options.
     */
    public static void mergeExportOptions(
        ImageExportOptions oldOptions,
        ImageExportOptions newOptions
    ) {
        // Copy over the file name, updating its suffix:
        File file = oldOptions.getExportFile();
        ImageType newType = newOptions.getImageType();
        if ((file != null) && (! file.isDirectory())) {
            file = ExportNameUtility.ensureCompatibleExtension(
                file, newType
            );
            newOptions.setExportFile(file);
        }
        // Clone values from old to new:
        if ( oldOptions instanceof ImageFileExportOptions &&
             newOptions instanceof ImageFileExportOptions ) {
            final ImageFileExportOptions oldFileOptions =
                (ImageFileExportOptions)oldOptions;
            final ImageFileExportOptions newFileOptions =
                (ImageFileExportOptions)newOptions;
            copyOption( oldFileOptions.originalWidth, newFileOptions.originalWidth );
            copyOption( oldFileOptions.originalHeight, newFileOptions.originalHeight );
            copyOption( oldFileOptions.resizeWidth, newFileOptions.resizeWidth );
            copyOption( oldFileOptions.resizeHeight, newFileOptions.resizeHeight );
            copyOption( oldFileOptions.colorProfile, newFileOptions.colorProfile );
            copyOption( oldFileOptions.renderingIntent, newFileOptions.renderingIntent );
            copyOption( oldFileOptions.blackPointCompensation, newFileOptions.blackPointCompensation );
        }

        // Switches from sidecar TIFF to multilayer TIFF are possible:
        ImageType oldType = oldOptions.getImageType();
        if ((oldType == TIFFImageType.INSTANCE) &&
            (newType == TIFFImageType.INSTANCE)) {
            TIFFImageType.ExportOptions oldTiff =
                (TIFFImageType.ExportOptions) oldOptions;
            TIFFImageType.ExportOptions newTiff =
                (TIFFImageType.ExportOptions) newOptions;
            copyOption(oldTiff.bitsPerChannel, newTiff.bitsPerChannel);
            copyOption(oldTiff.lzwCompression, newTiff.lzwCompression);
        }
    }

    private static void colorProfileFrom(
        ImageMetadata meta, ImageFileExportOptions options
    ) {
        if (! (meta.getImageType() instanceof RawImageType)) {
            try {
                ImageInfo info = ImageInfo.getInstanceFor(meta.getFile());
                ICC_Profile profile = info.getImageType().getICCProfile(info);
                String profileName = ColorProfileInfo.getNameOf(profile);
                options.colorProfile.setValue(profileName);
            }
            catch (Throwable t) {
                //   BadImageFileException
                //   ColorProfileException
                //   IOException
                //   UnknownImageTypeException
                System.err.println(
                    "Error cloning color profile: " + t.getClass().getName()
                );
            }
        }
    }

    private static void resolutionFrom( ImageMetadata metadata,
                                        ImageFileExportOptions options ) {
        final int resolution = (int)metadata.getResolution();
        final int resolutionUnit = metadata.getResolutionUnit();
        if ( resolution > 0 &&
             resolutionUnit != ResolutionProvider.RESOLUTION_UNIT_NONE ) {
            options.resolution.setValue( resolution );
            options.resolutionUnit.setValue( resolutionUnit );
        }
    }

    /**
     * Clone ImageExportOption values.
     */
    private static void copyOption(
        BooleanExportOption source, BooleanExportOption target
    ) {
        target.setValue(source.getValue());
    }
    /**
     * Clone ImageExportOption values.
     */
    private static void copyOption(
        IntegerExportOption source, IntegerExportOption target
    ) {
        target.setValue(source.getValue());
    }

    /**
     * Clone ImageExportOption values.
     */
    private static void copyOption(
        ColorProfileOption source, ColorProfileOption target
    ) {
        target.setValue(source.getValue());
    }

    /**
     * Clone ImageExportOption values.
     */
    private static void copyOption(
        LZWCompressionOption source, LZWCompressionOption target
    ) {
        target.setValue(source.getValue());
    }
}
