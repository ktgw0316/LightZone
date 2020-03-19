/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.File;
import java.io.IOException;

import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

/**
 * A <code>JPEGParserEventHandler</code> handles events from a
 * {@link JPEGParser}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface JPEGParserEventHandler {

    /**
     * The parser got a JPEG segment.
     *
     * @param segID The segment marker ID.
     * @param segLength The length of the segment (not including the 2 bytes
     * for the length itself).
     * @param jpegFile
     * @param buf The {@link LCByteBuffer} containing the JPEG being parsed.
     * @return Returns <code>true</code> only if parsing should continue.
     */
    boolean gotSegment( byte segID, int segLength, File jpegFile,
                        LCByteBuffer buf ) throws IOException;

}
/* vim:set et sw=4 ts=4: */
