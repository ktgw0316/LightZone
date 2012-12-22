/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import static com.lightcrafts.image.types.AdobeConstants.ADOBE_APPE_SEGMENT_SIZE;
import static com.lightcrafts.image.types.JPEGConstants.JPEG_APPE_MARKER;

/**
 * An <code>AdobeJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter} that
 * is used to get the Adobe (APPE) segment.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see "Adobe Technical Note #5116: Supporting the DCT Filters in PostScript
 * Level 2, Adobe Systems, Inc., November 24, 1992, p. 23."
 */
public class AdobeJPEGSegmentFilter implements JPEGSegmentFilter {

    /**
     * {@inheritDoc}
     */
    public boolean accept( byte segID, ByteBuffer buf ) {
        if ( segID != JPEG_APPE_MARKER || buf.limit() < ADOBE_APPE_SEGMENT_SIZE )
            return false;
        return ByteBufferUtil.getEquals( buf, 0, "Adobe", "ASCII" );
    }

}
/* vim:set et sw=4 ts=4: */
