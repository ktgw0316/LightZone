/* JPEGImageEncoder.java -- JPEG encoder implementation
Copyright (C) 2011 Red Hat

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package sun.awt.image.codec;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGDecodeParam;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.imageio.plugins.jpeg.JPEG;

/**
 * This class provides the implementation for encoding JPEG images.
 *
 */
public class JPEGImageEncoderImpl implements JPEGImageEncoder {
    private static final String JPGMime = "image/jpeg";

    private ImageWriter JPGWriter;
    private JPEGEncodeParam param;
    private OutputStream out;

    public JPEGImageEncoderImpl(OutputStream os) {
        this(os, null);
    }

    public JPEGImageEncoderImpl(OutputStream out, JPEGEncodeParam newParam) {
        this.out = out;
        setJPEGEncodeParam(newParam);

        Iterator<ImageWriter> JPGWriterIter = ImageIO
                .getImageWritersByMIMEType(JPGMime);
        if (JPGWriterIter.hasNext()) {
            JPGWriter = JPGWriterIter.next();
        }

        JPGWriter.setOutput(new MemoryCacheImageOutputStream(out));
    }

    public JPEGEncodeParam getDefaultJPEGEncodeParam(BufferedImage bi)
            throws ImageFormatException {
        return JPEGCodec.getDefaultJPEGEncodeParam(bi);
    }

    public JPEGEncodeParam getDefaultJPEGEncodeParam(int numBands, int colorID)
            throws ImageFormatException {
        return JPEGCodec.getDefaultJPEGEncodeParam(numBands, colorID);
    }

    public JPEGEncodeParam getDefaultJPEGEncodeParam(JPEGDecodeParam d)
            throws ImageFormatException {
        return JPEGCodec.getDefaultJPEGEncodeParam(d);
    }

    public JPEGEncodeParam getDefaultJPEGEncodeParam(Raster ras, int colorID)
            throws ImageFormatException {
        return JPEGCodec.getDefaultJPEGEncodeParam(ras, colorID);
    }

    public JPEGEncodeParam getJPEGEncodeParam() throws ImageFormatException {
        if (param == null)
            return null;
        return (JPEGEncodeParam) param.clone();
    }

    public void setJPEGEncodeParam(JPEGEncodeParam p) {
        param = p;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    private void encode(IIOImage img) throws IOException, ImageFormatException {
        if (JPGWriter == null)
            throw new ImageFormatException(
                    "JPEG writer code not implemented in ImageIO");

        JPEGImageWriteParam jiwp = new JPEGImageWriteParam(null);
        ;
        jiwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        if (param != null && param instanceof JPEGParam) {
            JPEGParam jp = (JPEGParam) param;
            jiwp.setCompressionQuality(jp.getQuality());
        } else {
            jiwp.setCompressionQuality(JPEG.DEFAULT_QUALITY);
        }

        JPGWriter.write(null, img, jiwp);
    }

    public void encode(BufferedImage bi, JPEGEncodeParam writeParam)
            throws IOException, ImageFormatException {
        setJPEGEncodeParam(writeParam);
        encode(new IIOImage(bi, new ArrayList<BufferedImage>(), null));
    }

    public void encode(Raster rs, JPEGEncodeParam writeParam)
            throws IOException, ImageFormatException {
        setJPEGEncodeParam(writeParam);
        encode(new IIOImage(rs, new ArrayList<BufferedImage>(), null));
    }

    public void encode(BufferedImage bi) throws IOException,
            ImageFormatException {
        encode(bi, null);
    }

    public void encode(Raster rs) throws IOException, ImageFormatException {
        encode(rs, null);
    }

    @Override
    public int getDefaultColorId(ColorModel cm) {
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
