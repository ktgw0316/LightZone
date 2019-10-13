/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import java.nio.ByteBuffer;

import static com.lightcrafts.image.types.AdobeConstants.ADOBE_APPC_SEGMENT_SIZE;
import static com.lightcrafts.image.types.JPEGConstants.JPEG_APPC_MARKER;

/**
 * An <code>AdobeEmbedJPEGSegmentFilter</code> is-a {@link JPEGSegmentFilter} that is used to get
 * the Adobe (APPE) segment.
 *
 * @author Masahiro Kitagawa
 * @see "Adobe Technical Note #5116: Supporting the DCT Filters in PostScript Level 2, Adobe
 * Systems, Inc., November 24, 1992, p. 23."
 */
public class AdobeEmbedJPEGSegmentFilter implements JPEGSegmentFilter {

    /**
     * {@inheritDoc}
     */
    public boolean accept(byte segID, ByteBuffer buf) {
        if (segID != JPEG_APPC_MARKER || buf.limit() < ADOBE_APPC_SEGMENT_SIZE) {
            return false;
        }
        return ByteBufferUtil.getEquals(buf, 0, "EMBED\0", "ASCII");
    }

}
/* vim:set et sw=4 ts=4: */

