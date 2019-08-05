/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.operator.TransposeType;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;

/**
 * A <code>DCRImageType</code> is-a {@link RawImageType} for DCR (Kodak raw)
 * images.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 */
public final class DCRImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>DCRImageType</code>. */
    public static final DCRImageType INSTANCE = new DCRImageType();

    /**
     * {@inheritDoc}
     */
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "DCR";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final RenderedImage thumb = super.getThumbnailImage( imageInfo );
        final ImageMetadata metaData = imageInfo.getMetadata();
        final ImageOrientation orientation = metaData.getOrientation();
        final TransposeType transpose = orientation.getCorrection();
        if ( transpose == null )
            return thumb;
        final ParameterBlock pb = new ParameterBlock();
        pb.addSource( thumb );
        pb.add( transpose );
        return JAI.create( "Transpose", pb, null );
    }

    /**
     * {@inheritDoc}
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final ImageMetadataReader reader = new TIFFMetadataReader( imageInfo );
        MetadataUtil.removePreviewMetadataFrom( reader.readMetadata() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>DCRImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private DCRImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for DCR files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "dcr"
    };
}
/* vim:set et sw=4 ts=4: */
