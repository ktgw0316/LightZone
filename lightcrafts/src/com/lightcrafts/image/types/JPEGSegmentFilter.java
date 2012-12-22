/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.nio.ByteBuffer;

/**
 * A <code>JPEGSegmentFilter</code> is a filter for JPEG segments.
 *
 * @see JPEGImageInfo#getAllSegmentsFor(Byte,JPEGSegmentFilter)
 * @see JPEGImageInfo#getFirstSegmentFor(Byte,JPEGSegmentFilter)
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface JPEGSegmentFilter {

    /**
     * Tests whether the given JPEG segment should be accepted.
     *
     * @param segID The segment marker ID.
     * @param buf The {@link ByteBuffer} containing the JPEG segment to test.
     * Upon return, the buffer's position must be unchanged.
     * @return Returns <code>true</code> only if the JPEG segment should be
     * accepted.
     */
    boolean accept( byte segID, ByteBuffer buf );

}
/* vim:set et sw=4 ts=4: */
