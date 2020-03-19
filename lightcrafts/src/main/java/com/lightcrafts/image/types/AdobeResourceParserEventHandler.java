/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.File;
import java.io.IOException;

import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

/**
 * A <code>AdobeResourceParserEventHandler</code> handles events from an
 * {@link AdobeResourceParser}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface AdobeResourceParserEventHandler {

    /**
     * The parser got an Adobe resource block.
     *
     * @param blockID The resource ID.
     * @param name The resource name.  (It may be the empty string.)
     * @param dataLength The length of the data in the block.
     * @param file The {@link File} it was parsed from.
     * @param buf The {@link LCByteBuffer} containing the file being parsed.
     * @return Returns <code>true</code> only if parsing should continue.
     */
    boolean gotResource( int blockID, String name, int dataLength, File file,
                         LCByteBuffer buf ) throws IOException;

}
/* vim:set et sw=4 ts=4: */
