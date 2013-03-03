/* JPEGCodec.java --
 * Copyright (C) 2007 Free Software Foundation, Inc.
 * Copyright (C) 2007 Matthew Flaschen
 *
 * This file is part of GNU Classpath.
 *
 * GNU Classpath is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * GNU Classpath is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GNU Classpath; see the file COPYING. If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package com.sun.image.codec.jpeg;

import java.io.InputStream;
import java.io.OutputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

import sun.awt.image.codec.JPEGImageDecoderImpl;
import sun.awt.image.codec.JPEGImageEncoderImpl;
import sun.awt.image.codec.JPEGParam;

/**
 * This class is a factory for implementations of the JPEG Image
 * Decoder/Encoder.
 */
public class JPEGCodec {

    private JPEGCodec() {}

    /**
     * This creates an instance of a JPEGImageDecoder that can be used to decode
     * JPEG Data streams.
     *
     * @param src
     * @return
     */
    public static JPEGImageDecoder createJPEGDecoder(InputStream src) {
        return new JPEGImageDecoderImpl(src);
    }

    /**
     * This creates an instance of a JPEGImageDecoder that can be used to decode
     * JPEG Data streams.
     *
     * @param src
     * @param jdp
     * @return
     */
    public static JPEGImageDecoder createJPEGDecoder(InputStream src,
            JPEGDecodeParam jdp) {
        return new JPEGImageDecoderImpl(src, jdp);
    }

    /**
     * This creates an instance of a JPEGImageEncoder that can be used to encode
     * image data as JPEG Data streams.
     *
     * @param os
     * @return
     */
    public static JPEGImageEncoder createJPEGEncoder(OutputStream os) {
        return new JPEGImageEncoderImpl(os);
    }

    /**
     * This creates an instance of a JPEGImageEncoder that can be used to encode
     * image data as JPEG Data streams.
     *
     * @param dest
     * @param jep
     * @return
     */
    public static JPEGImageEncoder createJPEGEncoder(OutputStream dest,
            JPEGEncodeParam jep) {
        return new JPEGImageEncoderImpl(dest, jep);
    }

    /**
     * This is a factory method for creating JPEGEncodeParam objects.
     *
     * @param bi
     * @return
     */
    public static JPEGEncodeParam getDefaultJPEGEncodeParam(BufferedImage bi) {
        return getDefaultJPEGEncodeParam(bi.getRaster(),
                getDefaultColorID(bi.getColorModel()));
    }

    /**
     * This is a factory method for creating JPEGEncodeParam objects.
     *
     * @param numBands
     * @param colorID
     * @return
     */
    public static JPEGEncodeParam getDefaultJPEGEncodeParam(int numBands,
            int colorID) {
        return new JPEGParam(colorID, numBands);
    }

    /**
     * This is a factory method for creating a JPEGEncodeParam from a
     * JPEGDecodeParam.
     *
     * @param jdp
     * @return
     */
    public static JPEGEncodeParam getDefaultJPEGEncodeParam(JPEGDecodeParam jdp) {
        return new JPEGParam(jdp);
    }

    /**
     * This is a factory method for creating JPEGEncodeParam objects.
     *
     * @param ras
     * @param colorID
     * @return
     */
    public static JPEGEncodeParam getDefaultJPEGEncodeParam(Raster ras,
            int colorID) {
        return getDefaultJPEGEncodeParam(ras.getNumBands(), colorID);
    }

    private static int getDefaultColorID(ColorModel cm) {
        ColorSpace cs = cm.getColorSpace();
        int type = cs.getType();
        int id = -1;
        switch (type) {
            case ColorSpace.TYPE_GRAY:
                id = JPEGEncodeParam.COLOR_ID_GRAY;
                break;

            case ColorSpace.TYPE_RGB:
                id = cm.hasAlpha() ? JPEGEncodeParam.COLOR_ID_RGBA
                        : JPEGEncodeParam.COLOR_ID_RGB;

            case ColorSpace.TYPE_YCbCr:
                try {
                    if (cs == ColorSpace.getInstance(ColorSpace.CS_PYCC)) {
                        id = cm.hasAlpha() ? JPEGEncodeParam.COLOR_ID_PYCCA
                                : JPEGEncodeParam.COLOR_ID_PYCC;
                    }
                } catch (IllegalArgumentException e) {
                    /* We know it isn't PYCC type, nothing to handle */
                }
                if (id == -1) {
                    id = cm.hasAlpha() ? JPEGEncodeParam.COLOR_ID_YCbCrA
                            : JPEGEncodeParam.COLOR_ID_YCbCr;
                }
                break;

            case ColorSpace.TYPE_CMYK:
                id = JPEGEncodeParam.COLOR_ID_CMYK;
                break;

            default:
                id = JPEGEncodeParam.COLOR_ID_UNKNOWN;
        }

        return id;
    }
}
