/* JPEGImageEncoder.java --
   Copyright (C) 2007 Free Software Foundation, Inc.

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

import java.io.OutputStream;
import java.io.IOException;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

public interface JPEGImageEncoder {
    /**
     * This is a factory method for creating JPEGEncodeParam objects. The
     * returned object will do a credible job of encoding the given
     * BufferedImage.
     *
     * @param bi
     * @return
     * @throws ImageFormatException
     */
    public JPEGEncodeParam getDefaultJPEGEncodeParam(BufferedImage bi)
            throws ImageFormatException;

    /**
     * This is a factory method for creating JPEGEncodeParam objects. It is the
     * users responsibility to match the colorID with the given number of bands,
     * which should match the data being encoded. Failure to do so may lead to
     * poor compression and/or poor image quality. If you don't understand much
     * about JPEG it is strongly recommended that you stick to the BufferedImage
     * interface.
     *
     * @param numBands
     *            the number of bands that will be encoded (max of four).
     * @param colorID
     *            the COLOR_ID for the encoded data. This is used to set
     *            reasonable defaults in the parameter object. This must match
     *            the number of bands given.
     * @return
     * @throws ImageFormatException
     */
    public JPEGEncodeParam getDefaultJPEGEncodeParam(int numBands, int colorID)
            throws ImageFormatException;

    /**
     * This is a factory method for creating a JPEGEncodeParam from a
     * JPEGDecodeParam. This will return a new JPEGEncodeParam object that is
     * initialized from the JPEGDecodeParam object. All major pieces of
     * information will be initialized from the DecodeParam (Markers, Tables,
     * mappings).
     *
     * @param d
     *            The JPEGDecodeParam object to copy.
     * @return
     * @throws ImageFormatException
     */
    public JPEGEncodeParam getDefaultJPEGEncodeParam(JPEGDecodeParam d)
            throws ImageFormatException;

    /**
     * This is a factory method for creating JPEGEncodeParam objects. It is the
     * users responsiblity to match the colorID with the data contained in the
     * Raster. Failure to do so may lead to either poor compression or poor
     * image quality. If you don't understand much about JPEG it is strongly
     * reccomended that you stick to the BufferedImage interfaces.
     *
     * @param ras
     * @param colorID
     * @return
     * @throws ImageFormatException
     */
    public JPEGEncodeParam getDefaultJPEGEncodeParam(Raster ras, int colorID)
            throws ImageFormatException;

    public JPEGEncodeParam getJPEGEncodeParam() throws ImageFormatException;

    /**
     * Set the JPEGEncodeParam object that is to be used for future encoding
     * operations. 'p' is copied so changes will not be tracked, unless you call
     * this method again.
     *
     * @param p
     *            The JPEGEncodeParam object to use for future encodings.
     */
    public void setJPEGEncodeParam(JPEGEncodeParam p);

    /**
     * Return the stream the Encoder is current associated with.
     *
     * @return
     */
    public OutputStream getOutputStream();

    /**
     * Encode a BufferedImage as a JPEG data stream. Note, some color
     * conversions may takes place. The jep's encoded COLOR_ID should match the
     * value returned by getDefaultColorID when given the BufferedImage's
     * ColorModel. This call also sets the current JPEGEncodeParam object. The
     * given JPEGEncodeParam object will be used for this and future encodings.
     * If p is null then a new JPEGEncodeParam object will be created by calling
     * getDefaultJPEGEncodeParam with bi.
     *
     * @param bi
     *            The BufferedImage to encode.
     * @param p
     *            The JPEGEncodeParam object used to control the encoding.
     * @throws IOException
     * @throws ImageFormatException
     */
    public void encode(BufferedImage bi, JPEGEncodeParam p) throws IOException,
            ImageFormatException;

    /**
     * Encode a Raster as a JPEG data stream. Note that no color conversion
     * takes place. It is required that you match the Raster to the encoded
     * COLOR_ID contained in the current JPEGEncodeParam object. If no
     * JPEGEncodeParam object has been provided yet a new JPEGEncodeParam object
     * will be created by calling getDefaultJPEGEncodeParam with ras and
     * COLOR_ID_UNKNOWN.
     *
     * @param ras
     *            The Raster to encode.
     * @throws IOException
     * @throws ImageFormatException
     */
    public void encode(Raster ras) throws IOException, ImageFormatException;

    /**
     * Encode a BufferedImage as a JPEG data stream. Note, some color
     * conversions may takes place. The current JPEGEncodeParam's encoded
     * COLOR_ID should match the value returned by getDefaultColorID when given
     * the BufferedImage's ColorModel. If no JPEGEncodeParam object has been
     * provided yet a default one will be created by calling
     * getDefaultJPEGEncodeParam with bi.
     *
     * @param bi
     *            The BufferedImage to encode.
     * @throws IOException
     * @throws ImageFormatException
     */
    public void encode(BufferedImage bi) throws IOException,
            ImageFormatException;

    /**
     * Encode a Raster as a JPEG data stream. Note that no color conversion
     * takes place. It is required that you match the Raster to the encoded
     * COLOR_ID contained in the JPEGEncodeParam object. If p is null a new
     * JPEGEncodeParam object will be created by calling
     * getDefaultJPEGEncodeParam with ras and COLOR_ID_UNKNOWN.
     *
     * @param ras
     *            The Raster to encode.
     * @param p
     *            The JPEGEncodeParam object used to control the encoding.
     * @throws IOException
     * @throws ImageFormatException
     */
    public void encode(Raster ras, JPEGEncodeParam p) throws IOException,
            ImageFormatException;

    /**
     * Returns the 'default' encoded COLOR_ID for a given ColorModel. This
     * method is not needed in the simple case of encoding Buffered Images (the
     * library will figure things out for you). It can be useful for encoding
     * Rasters. To determine what needs to be done to the image prior to
     * encoding.
     *
     * @param cm
     *            The ColorModel to map to an jpeg encoded COLOR_ID.
     * @return
     */
    public int getDefaultColorId(ColorModel cm);
}
