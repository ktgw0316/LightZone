/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;
import java.io.File;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

/**
 * An <code>EXIFParserEventHandler</code> handles various events from an
 * {@link EXIFParser} as it parses.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface EXIFParserEventHandler {

    /**
     * The {@link EXIFParser} got bad metadata.
     *
     * @param message A message describing what is wrong with the metadata.
     */
    void gotBadMetadata( String message );

    /**
     * The {@link EXIFParser} got bad metadata by way of an exception.
     *
     * @param cause The thrown exception.
     */
    void gotBadMetadata( Throwable cause );

    /**
     * The {@link EXIFParser} just parsed the beginning of a metadata
     * directory.
     *
     * @return Returns an {@link ImageMetadataDirectory} for the recently
     * parsed directory.
     */
    ImageMetadataDirectory gotDirectory();

    /**
     * The {@link EXIFParser} just parsed an EXIF metadata tag.
     *
     * @param tagID The tag ID.
     * @param fieldType The metadata field type.
     * @param numValues The number of metadata values.
     * @param byteCount The total number of bytes for all values.
     * @param valueOffset The offset of the first value.
     * @param valueOffsetAdjustment The larger-than-4-byte-value offset
     * adjustment.
     * @param subdirOffset The current subdirectory offset.
     * @param imageInfo The image.
     * @param buf The {@link LCByteBuffer} the raw metadata is in.
     * @param dir The relevant {@link ImageMetadataDirectory}.
     */
    void gotTag(int tagID, int fieldType, int numValues, int byteCount,
                int valueOffset, int valueOffsetAdjustment, int subdirOffset,
                ImageInfo imageInfo, LCByteBuffer buf, ImageMetadataDirectory dir )
        throws IOException;

}
/* vim:set et sw=4 ts=4: */
