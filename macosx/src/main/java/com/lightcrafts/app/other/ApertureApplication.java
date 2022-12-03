/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

import java.awt.*;
import java.io.IOException;
import java.io.File;

import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.types.MultipageTIFFImageType;
import com.lightcrafts.utils.LightCraftsException;
import com.lightcrafts.platform.macosx.AppleScript;

/**
 * A <code>ApertureApplication</code> is-an {@link MacApplication} for Apple's
 * Aperture.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class ApertureApplication extends MacApplication {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public ImageFileExportOptions createExportOptions( ImageInfo imageInfo,
                                                       Dimension saveSize )
        throws IOException, LightCraftsException
    {
        final MultipageTIFFImageType.ExportOptions options =
            MultipageTIFFImageType.INSTANCE.newExportOptions();
        initOptions( options, imageInfo, saveSize );
        return options;
    }

    /**
     * {@inheritDoc}
     */
    public void postSave( File imageFile, boolean didSaveDirectly,
                          boolean openPending ) {
        if ( !openPending )
            AppleScript.bringAppToFront( getName() );
    }

    /**
     * {@inheritDoc}
     */
    public boolean shouldSaveDirectly( ImageInfo imageInfo ) {
        return true;
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance. */
    static final ApertureApplication INSTANCE = new ApertureApplication();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>ApertureApplication</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private ApertureApplication() {
        super( "Aperture", "fstp" );
    }

}
/* vim:set et sw=4 ts=4: */
