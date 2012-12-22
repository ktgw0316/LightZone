/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.image.metadata.values.ImageMetaValue;

/**
 * A <code>TagHandler</code> allows a tag to be handled in a way beyond what
 * {@link ImageMetadataDirectory#putValue(int,ImageMetaValue)} allows because
 * more information about the tag is passed via parameters.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface TagHandler {

    /**
     * Give the opportunity to handle a tag.
     *
     * @param tagID The tag ID.
     * @param fieldType The metadata field type.
     * @param numValues The number of metadata values.
     * @param byteCount The total number of bytes for all values.
     * @param valueOffset The offset of the first value.
     * @param valueOffsetAdjustment The larger-than-4-byte-value offset
     * adjustment.
     * @param subdirOffset The current subdirectory offset.
     * @param imageInfo The current {@link ImageInfo}.
     * @param buf The current {@link LCByteBuffer}.
     * @param dir The current {@link ImageMetadataDirectory}.
     * @return Returns <code>true</code> only if the tag was handled.
     */
    boolean handleTag( int tagID, int fieldType, int numValues, int byteCount,
                       int valueOffset, int valueOffsetAdjustment,
                       int subdirOffset, ImageInfo imageInfo,
                       LCByteBuffer buf, ImageMetadataDirectory dir )
        throws IOException;

}
/* vim:set et sw=4 ts=4: */
