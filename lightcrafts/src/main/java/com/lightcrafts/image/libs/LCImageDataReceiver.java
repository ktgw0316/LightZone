/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An <code>LCImageDataReceiver</code> is used to provide image data to a Light Crafts image library
 * for it to compress.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface LCImageDataReceiver {

    /**
     * Puts image data.
     *
     * @param buf The {@link ByteBuffer} containing the data that is to be written.
     * @return Returns the number of bytes written.
     */
    int putImageData(ByteBuffer buf) throws IOException, LCImageLibException;
}
/* vim:set et sw=4 ts=4: */
