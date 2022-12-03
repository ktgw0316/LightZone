/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;
import java.nio.BufferUnderflowException;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

/**
 * <code>PhaseOneTIFFRawImageProbe</code> is-a {@link TrueImageTypeProvider}
 * for determining whether a file with a <code>.tif</code> extension is really
 * a Phase One raw file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class PhaseOneTIFFRawImageProbe implements TrueImageTypeProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public ImageType getTrueImageTypeOf( ImageInfo imageInfo )
        throws IOException
    {
        final LCByteBuffer buf = imageInfo.getByteBuffer();
        try {
            buf.position( 12 );
            boolean isRaw = buf.getEquals("Raw", "ASCII");
            if (! isRaw)
            {
                buf.position( 13 );
                isRaw = buf.getEquals("waR", "ASCII");
            }
            if (isRaw) {
                final ImageMetadata metadata = imageInfo.getCurrentMetadata();
                MetadataUtil.removePreviewMetadataFrom( metadata );
                MetadataUtil.removeWidthHeightFrom( metadata );
                return PhaseOneTIFFRawImageType.INSTANCE;
            }
        }
        catch ( BufferUnderflowException e ) {
            // ignore
        }
        catch ( IllegalArgumentException e ) {
            // ignore
        }
        return null;
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance of <code>PhaseOneTIFFRawImageProbe</code>. */
    static final PhaseOneTIFFRawImageProbe INSTANCE =
        new PhaseOneTIFFRawImageProbe();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of
     * <code>PhaseOneTIFFRawImageProbe</code>.
     */
    private PhaseOneTIFFRawImageProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
