/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import static com.lightcrafts.image.types.AdobeConstants.PHOTOSHOP_3_IDENT;
import static com.lightcrafts.image.types.JPEGConstants.JPEG_APPD_MARKER;

/**
 * An <code>IPTCJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter} that
 * is used to get the IPTC (APPD) segment.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class IPTCJPEGSegmentFilter implements JPEGSegmentFilter {

    /**
     * {@inheritDoc}
     */
    public boolean accept( byte segID, ByteBuffer buf ) {
        if ( segID != JPEG_APPD_MARKER ||
             buf.limit() <= PHOTOSHOP_3_IDENT.length() )
            return false;
        return ByteBufferUtil.getEquals( buf, 0, PHOTOSHOP_3_IDENT, "ASCII" );
    }

}
/* vim:set et sw=4 ts=4: */
