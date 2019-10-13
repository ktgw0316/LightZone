/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

import java.awt.*;
import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.utils.LightCraftsException;

import static com.lightcrafts.image.metadata.providers.ResolutionProvider.*;

/**
 * An <code>OtherApplication</code> is an enum for the various other
 * applications that can request LightZone to open an image file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class OtherApplication {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Creates an {@link ImageFileExportOptions} that is to be used to save the
     * given image.
     *
     * @param imageInfo The image LightZone was given to edit.
     * @param saveSize The saved size of the image.
     * @return Returns said {@link ImageExportOptions}.
     */
    public ImageFileExportOptions createExportOptions( ImageInfo imageInfo,
                                                       Dimension saveSize )
        throws IOException, LightCraftsException
    {
        return null;
    }

    /**
     * Gets the user-presentable name of the application.
     *
     * @return Returns said name.
     */
    public final String getName() {
        return m_name;
    }

    /**
     * Checks whether integration is enabled.
     *
     * @return Returns <code>true</code> only if it is.
     */
    public static boolean isIntegrationEnabled() {
        return m_prefs.getBoolean( INTEGRATION_ENABLED_KEY, true );
    }

    /**
     * Performs some action just after the image file was saved.
     *
     * @param imageFile The {@link File} the image was saved to.
     * @param didSaveDirectly Must be <code>true</code> only when the recent
     * save was directly back to the other application.
     * @param openPending Must be <code>true</code> only when the recent save
     * was prompted because the user wants to open a different image.
     */
    public void postSave( File imageFile, boolean didSaveDirectly,
                          boolean openPending ) throws IOException {
        // do nothing
    }

    /**
     * Checks whether an image should be saved directly back to the other
     * application (that is, without presenting a Save dialog).
     *
     * @param imageInfo The image LightZone was given to edit.
     * @return Returns <code>true</code> only if the image should be saved
     * directly.
     */
    public boolean shouldSaveDirectly( ImageInfo imageInfo ) {
        return false;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct an <code>OtherApplication</code>.
     *
     * @param name The user-presentable name of the application.
     */
    protected OtherApplication( String name ) {
        m_name = name;
    }

    /**
     * Initialize the given {@link ImageFileExportOptions} from the metadata of
     * the given image.
     *
     * @param options The {@link ImageFileExportOptions} to initialize.
     * @param imageInfo The original image.
     * @param saveSize The size of the saved image, i.e., after cropping.
     */
    protected static void initOptions( ImageFileExportOptions options,
                                       ImageInfo imageInfo,
                                       Dimension saveSize )
        throws IOException, LightCraftsException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageType t = imageInfo.getImageType();

        options.setExportFile( imageInfo.getFile() );

        try {
            final ICC_Profile profile = t.getICCProfile( imageInfo );
            if ( profile != null ) {
                final String profileName =
                    ColorProfileInfo.getNameOf( profile );
                options.colorProfile.setValue( profileName );
            }
        }
        catch ( LightCraftsException e ) {
            // use ImageExportOptions' default color profile
        }
        catch ( IOException e ) {
            // use ImageExportOptions' default color profile
        }

        options.originalWidth.setValue( metadata.getImageWidth() );
        options.originalHeight.setValue( metadata.getImageHeight() );
        options.resizeWidth.setValue( saveSize.width );
        options.resizeHeight.setValue( saveSize.height );

        final double resolution = metadata.getResolution();
        final int resolutionUnit = metadata.getResolutionUnit();

        if ( resolution > 0 && resolutionUnit != RESOLUTION_UNIT_NONE ) {
            options.resolution.setValue( (int)resolution );
            options.resolutionUnit.setValue( resolutionUnit );
        }

        if ( options instanceof TIFFImageType.ExportOptions ) {
            final TIFFImageType.ExportOptions tiffOptions =
                (TIFFImageType.ExportOptions)options;

            final int bitsPerChannel = metadata.getBitsPerChannel();
            if ( bitsPerChannel > 0 )
                tiffOptions.bitsPerChannel.setValue( bitsPerChannel );
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    /** The user-presentable name of the application. */
    private final String m_name;

    private static final String PREFS_PACKAGE = "/com/lightcrafts/app/other";

    private static final String INTEGRATION_ENABLED_KEY = "IntegrationEnabled";

    private static final Preferences m_prefs =
        Preferences.userRoot().node( PREFS_PACKAGE );

}
/* vim:set et sw=4 ts=4: */
