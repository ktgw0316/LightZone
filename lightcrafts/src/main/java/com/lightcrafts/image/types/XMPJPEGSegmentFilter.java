/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import static com.lightcrafts.image.metadata.XMPConstants.XMP_XAP_NS;
import static com.lightcrafts.image.types.JPEGConstants.*;

/**
 * An <code>XMPJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter} that
 * is used to get the XMP (APP1) segment.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class XMPJPEGSegmentFilter implements JPEGSegmentFilter {

    /**
     * {@inheritDoc}
     */
    public boolean accept( byte segID, ByteBuffer buf ) {
        if ( segID != JPEG_APP1_MARKER || buf.limit() < JPEG_XMP_HEADER_SIZE )
            return false;
        return ByteBufferUtil.getEquals( buf, 0, XMP_XAP_NS, "ASCII" );
    }

}
/* vim:set et sw=4 ts=4: */
