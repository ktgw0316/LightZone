/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An <code>LCImageDataProvider</code> is used to provide image data to a Light Crafts image library
 * for it to decompress and form into an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface LCImageDataProvider {

    /**
     * Gets image data.
     *
     * @param buf The {@link ByteBuffer} into which to place data.
     * @return Returns the number of bytes placed into the buffer.
     */
    int getImageData(ByteBuffer buf) throws IOException, LCImageLibException;
}
/* vim:set et sw=4 ts=4: */
