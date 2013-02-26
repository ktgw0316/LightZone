/* JPEGImageDecoder.java --
   Copyright (C) 2007 Free Software Foundation, Inc.
   Copyright (C) 2007 Matthew Flaschen

   This file is part of GNU Classpath.

   GNU Classpath is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.

   GNU Classpath is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GNU Classpath; see the file COPYING.  If not, write to the
   Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
   02110-1301 USA.

   Linking this library statically or dynamically with other modules is
   making a combined work based on this library.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.

   As a special exception, the copyright holders of this library give you
   permission to link this library with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on this library.  If you modify this library, you may extend
   this exception to your version of the library, but you are not
   obligated to do so.  If you do not wish to do so, delete this
   exception statement from your version. */

package com.sun.image.codec.jpeg;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.io.InputStream;
import java.io.IOException;

public interface JPEGImageDecoder {

    /**
     * Decodes the current JPEG data stream. The result of decoding this
     * InputStream is a BufferedImage the ColorModel associated with this
     * BufferedImage is determined based on the encoded COLOR_ID of the
     * JPEGDecodeParam object. For a tables only stream this will return null.
     *
     * @return BufferedImage containing the image data.
     * @throws ImageFormatException
     *             If irregularities in the JPEG stream or an unknown condition
     *             is encountered.
     * @throws IOException
     */
    public BufferedImage decodeAsBufferedImage() throws IOException,
            ImageFormatException;

    /**
     * Decode the JPEG stream that was passed as part of construction. The JPEG
     * decompression will be performed according to the current settings of the
     * JPEGDecodeParam object. For a tables only stream this will return null.
     *
     * @return Raster containg the image data. Colorspace and other pertinent
     *         information can be obtained from the JPEGDecodeParam object.
     * @throws ImageFormatException
     *             If irregularities in the JPEG stream or an unknown condition
     *             is encountered.
     * @throws IOException
     */
    public Raster decodeAsRaster() throws IOException, ImageFormatException;

    /**
     * Get the input stream that decoding will occur from.
     *
     * @return The stream that the decoder is currently associated with.
     */
    public InputStream getInputStream();

    /**
     * Returns the JPEGDecodeParam object that resulted from the most recent
     * decoding event.
     *
     * @return
     */
    public JPEGDecodeParam getJPEGDecodeParam();

    /**
     * Sets the JPEGDecodeParam object used to determine the features of the
     * decompression performed on the JPEG encoded data. This is usually only
     * needed for decoding abbreviated JPEG data streams.
     *
     * @param jdp
     *            JPEGDecodeParam object
     */
    public void setJPEGDecodeParam(JPEGDecodeParam jdp);
}
