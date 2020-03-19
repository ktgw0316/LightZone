/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.makernotes.CanonDirectory;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.UnknownImageTypeException;

import static com.lightcrafts.image.metadata.makernotes.CanonConstants.CANON_CS_QUALITY_RAW;
import static com.lightcrafts.image.metadata.makernotes.CanonConstants.CANON_MODEL_ID_EOS_1D;
import static com.lightcrafts.image.metadata.makernotes.CanonConstants.CANON_MODEL_ID_EOS_1DS;
import static com.lightcrafts.image.metadata.makernotes.CanonTags.CANON_CS_QUALITY;
import static com.lightcrafts.image.metadata.makernotes.CanonTags.CANON_MODEL_ID;

/**
 * <code>CanonTIFFRawImageProbe</code> is-a {@link TrueImageTypeProvider} for
 * determining whether a file with a <code>.tif</code> extension is really a
 * Canon raw file.  At least the raw files from the Canon EOS-1D and EOS-1Ds
 * have a <code>.tif</code> extension.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
final class CanonTIFFRawImageProbe implements TrueImageTypeProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public ImageType getTrueImageTypeOf( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        try {
            final ImageMetadata metadata = imageInfo.getMetadata();
            final ImageMetaValue modelId =
                metadata.getValue( CanonDirectory.class, CANON_MODEL_ID );
            if ( modelId == null )
                return null;
            final int id = modelId.getIntValue();
            if ( id != CANON_MODEL_ID_EOS_1D && id != CANON_MODEL_ID_EOS_1DS )
                return null;
            final ImageMetaValue qualityValue =
                metadata.getValue( CanonDirectory.class, CANON_CS_QUALITY );
            if ( qualityValue == null )
                return null;
            if ( qualityValue.getIntValue() == CANON_CS_QUALITY_RAW ) {
                MetadataUtil.removePreviewMetadataFrom( metadata );
                MetadataUtil.removeWidthHeightFrom( metadata );
                return CanonTIFFRawImageType.INSTANCE;
            }
        }
        catch ( UnknownImageTypeException e ) {
            // can never happen at this stage
        }
        return null;
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance of <code>CanonTIFFRawImageProbe</code>. */
    static final CanonTIFFRawImageProbe INSTANCE = new CanonTIFFRawImageProbe();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>CanonTIFFRawImageProbe</code>.
     */
    private CanonTIFFRawImageProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
