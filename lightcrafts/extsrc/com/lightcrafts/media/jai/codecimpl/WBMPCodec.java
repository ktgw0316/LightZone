/*
 * $RCSfile: WBMPCodec.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/14 19:24:54 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.lightcrafts.media.jai.codec.ForwardSeekableStream;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageDecoder;
import com.lightcrafts.media.jai.codec.ImageDecoderImpl;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.ImageEncoder;
import com.lightcrafts.media.jai.codec.ImageEncoderImpl;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;

/**
 * A subclass of <code>ImageCodec</code> that handles the WBMP format.
 */
public final class WBMPCodec extends ImageCodec {

    public WBMPCodec() {}

    public String getFormatName() {
        return "wbmp";
    }

    public Class getEncodeParamClass() {
        return Object.class;
    }

    public Class getDecodeParamClass() {
        return Object.class;
    }

    public boolean canEncodeImage(RenderedImage im,
                                  ImageEncodeParam param) {
        SampleModel sampleModel = im.getSampleModel();

        int dataType = sampleModel.getTransferType();
        if (dataType == DataBuffer.TYPE_FLOAT  ||
            dataType == DataBuffer.TYPE_DOUBLE ||
            sampleModel.getNumBands() != 1     ||
            sampleModel.getSampleSize(0) != 1) {
            return false;
        }

        return true;
    }

    protected ImageEncoder createImageEncoder(OutputStream dst,
                                              ImageEncodeParam param) {
        return new WBMPImageEncoder(dst, null);
    }

    protected ImageDecoder createImageDecoder(InputStream src,
                                              ImageDecodeParam param) {
        // Add buffering for efficiency
        if (!(src instanceof BufferedInputStream)) {
            src = new BufferedInputStream(src);
        }
        return new WBMPImageDecoder(new ForwardSeekableStream(src), null);
    }

    protected ImageDecoder createImageDecoder(SeekableStream src,
                                              ImageDecodeParam param) {
        return new WBMPImageDecoder(src, null);
    }

    public int getNumHeaderBytes() {
         return 3;
    }

    public boolean isFormatRecognized(byte[] header) {
        // WBMP has no magic bytes at the beginning so simply check
        // the first three bytes for known constraints.
        return ((header[0] == (byte)0) &&  // TypeField == 0
                header[1] == 0 && // FixHeaderField == 0xxx00000; not support ext header
                ((header[2] & 0x8f) != 0 || (header[2] & 0x7f) != 0));  // First width byte
                //XXX: header[2] & 0x8f) != 0 for the bug in Sony Ericsson encoder.
    }
}

final class WBMPImageEncoder extends ImageEncoderImpl {

    // Get the number of bits required to represent an int.
    private static int getNumBits(int intValue) {
        int numBits = 32;
        int mask = 0x80000000;
        while(mask != 0 && (intValue & mask) == 0) {
            numBits--;
            mask >>>= 1;
        }
        return numBits;
    }

    // Convert an int value to WBMP multi-byte format.
    private static byte[] intToMultiByte(int intValue) {
        int numBitsLeft = getNumBits(intValue);
        byte[] multiBytes = new byte[(numBitsLeft + 6)/7];

        int maxIndex = multiBytes.length - 1;
        for(int b = 0; b <= maxIndex; b++) {
            multiBytes[b] = (byte)((intValue >>> ((maxIndex - b)*7))&0x7f);
            if(b != maxIndex) {
                multiBytes[b] |= (byte)0x80;
            }
        }

        return multiBytes;
    }

    public WBMPImageEncoder(OutputStream output,
                           ImageEncodeParam param) {
        super(output, param);
    }

    public void encode(RenderedImage im) throws IOException {
        // Get the SampleModel.
        SampleModel sm = im.getSampleModel();

        // Check the data type, band count, and sample size.
        int dataType = sm.getTransferType();
        if (dataType == DataBuffer.TYPE_FLOAT ||
            dataType == DataBuffer.TYPE_DOUBLE) {
            throw new IllegalArgumentException(JaiI18N.getString("WBMPImageEncoder0"));
        } else if (sm.getNumBands() != 1) {
            throw new IllegalArgumentException(JaiI18N.getString("WBMPImageEncoder1"));
        } else if (sm.getSampleSize(0) != 1) {
            throw new IllegalArgumentException(JaiI18N.getString("WBMPImageEncoder2"));
        }

        // Save image dimensions.
        int width = im.getWidth(); 
        int height = im.getHeight(); 

        // Write WBMP header.
        output.write(0); // TypeField
        output.write(0); // FixHeaderField
        output.write(intToMultiByte(width)); // width
        output.write(intToMultiByte(height)); // height

        Raster tile = null;

        // If the data are not formatted nominally then reformat.
        if(sm.getDataType() != DataBuffer.TYPE_BYTE ||
           !(sm instanceof MultiPixelPackedSampleModel) ||
           ((MultiPixelPackedSampleModel)sm).getDataBitOffset() != 0) {
            MultiPixelPackedSampleModel mppsm =
                new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                width, height, 1,
                                                (width + 7)/8, 0);
            WritableRaster raster =
                Raster.createWritableRaster(mppsm,
                                            new Point(im.getMinX(),
                                                      im.getMinY()));
            raster.setRect(im.getData());
            tile = raster;
        } else if(im.getNumXTiles() == 1 &&
                  im.getNumYTiles() == 1) {
            tile = im.getTile(im.getMinTileX(), im.getMinTileY());
        } else {
            tile = im.getData();
        }

        // Check whether the image is white-is-zero.
        boolean isWhiteZero = false;
        if(im.getColorModel() instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel)im.getColorModel();
            isWhiteZero =
                (icm.getRed(0) + icm.getGreen(0) + icm.getBlue(0)) >
                (icm.getRed(1) + icm.getGreen(1) + icm.getBlue(1));
        }

        // Get the line stride, bytes per row, and data array.
        int lineStride =
            ((MultiPixelPackedSampleModel)sm).getScanlineStride();
        int bytesPerRow = (width + 7)/8;
        byte[] bdata = ((DataBufferByte)tile.getDataBuffer()).getData();

        // Write the data.
        if(!isWhiteZero && lineStride == bytesPerRow) {
            // Write the entire image.
            output.write(bdata, 0, height*bytesPerRow);
        } else {
            // Write the image row-by-row.
            int offset = 0;
            if(!isWhiteZero) {
                // Black-is-zero
                for(int row = 0; row < height; row++) {
                    output.write(bdata, offset, bytesPerRow);
                    offset += lineStride;
                }
            } else {
                // White-is-zero: need to invert data.
                byte[] inverted = new byte[bytesPerRow];
                for(int row = 0; row < height; row++) {
                    for(int col = 0; col < bytesPerRow; col++) {
                        inverted[col] = (byte)(~(bdata[col+offset]));
                    }
                    output.write(inverted, 0, bytesPerRow);
                    offset += lineStride;
                }
            }
        }
    }
}

final class WBMPImageDecoder extends ImageDecoderImpl {

    public WBMPImageDecoder(SeekableStream input,
                           ImageDecodeParam param) {
        super(input, param);
    }

    public RenderedImage decodeAsRenderedImage(int page) throws IOException {
        if (page != 0) {
            throw new IOException(JaiI18N.getString(JaiI18N.getString("WBMPImageDecoder0")));
        }

        input.read(); // TypeField
        input.read(); // FixHeaderField

        // Image width
        int value = input.read();
        int width = value & 0x7f;
        while((value & 0x80) == 0x80) {
            width <<= 7;
            value = input.read();
            width |= (value & 0x7f);
        }

        // Image height
        value = input.read();
        int height = value & 0x7f;
        while((value & 0x80) == 0x80) {
            height <<= 7;
            value = input.read();
            height |= (value & 0x7f);
        }

        // Create byte-packed bilevel image width an IndexColorModel
        BufferedImage bi = new BufferedImage(width,
                                             height,
                                             BufferedImage.TYPE_BYTE_BINARY);

        // Get the image tile.
        WritableRaster tile = bi.getWritableTile(0, 0);

        // Get the SampleModel.
        MultiPixelPackedSampleModel sm =
            (MultiPixelPackedSampleModel)bi.getSampleModel();

        // Read the data.
        input.readFully(((DataBufferByte)tile.getDataBuffer()).getData(),
                   0, height*sm.getScanlineStride());

        return bi;
    }
}
