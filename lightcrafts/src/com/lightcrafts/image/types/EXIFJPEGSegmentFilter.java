/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import static com.lightcrafts.image.metadata.EXIFConstants.EXIF_HEADER_SIZE;
import static com.lightcrafts.image.types.JPEGConstants.JPEG_APP1_MARKER;

/**
 * An <code>EXIFJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter} that
 * is used to get the EXIF (APP1) segment.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class EXIFJPEGSegmentFilter implements JPEGSegmentFilter {

    /**
     * {@inheritDoc}
     */
    public boolean accept( byte segID, ByteBuffer buf ) {
        if ( segID != JPEG_APP1_MARKER || buf.limit() < EXIF_HEADER_SIZE )
            return false;
        return ByteBufferUtil.getEquals( buf, 0, "Exif", "ASCII" );
    }

}
/* vim:set et sw=4 ts=4: */
