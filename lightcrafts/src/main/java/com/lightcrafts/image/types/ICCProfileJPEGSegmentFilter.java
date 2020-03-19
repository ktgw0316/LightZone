/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import static com.lightcrafts.image.types.JPEGConstants.ICC_PROFILE_HEADER_SIZE;
import static com.lightcrafts.image.types.JPEGConstants.JPEG_APP2_MARKER;

/**
 * A <code>ICCProfileJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter}
 * that is used to get only the JPEG segments that contain ICC profile data.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ICCProfileJPEGSegmentFilter implements JPEGSegmentFilter {

    /**
     * {@inheritDoc}
     */
    public boolean accept( byte segID, ByteBuffer buf ) {
        if ( segID != JPEG_APP2_MARKER ||
             buf.limit() <= ICC_PROFILE_HEADER_SIZE )
            return false;
        return ByteBufferUtil.getEquals( buf, 0, "ICC_PROFILE", "ASCII" );
    }

}
/* vim:set et sw=4 ts=4: */
