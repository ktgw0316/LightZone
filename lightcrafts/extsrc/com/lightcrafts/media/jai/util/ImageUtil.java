/*
 * $RCSfile: ImageUtil.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/07/21 20:53:28 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.DeferredData;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.UnpackedImageData;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class ImageUtil {

    /** The minimum value of a float. */
    private static final float FLOAT_MIN = -Float.MAX_VALUE;

    /** The counter for images that use the method generateID to create
     *  a UID.
     */
    private static long counter;

    /** A constant used to extract a byte from a short or an int. */
    public static final int BYTE_MASK  = 0xFF;

    /** A constant used to extract an unsigned short from an int. */
    public static final int USHORT_MASK = 0xFFFF;

    /** Clamps a number to the range supported by byte data type. */
    public static final byte clampByte(int in) {
	return (in > 0xFF ? (byte)0xFF : (in >= 0 ? (byte)in : (byte)0));
    }

    /** Clamps a number to the range supported by unsigned short data type. */
    public static final short clampUShort(int in) {
	return (in > 0xFFFF ? (short)0xFFFF : (in >= 0 ? (short)in : (short)0));
    }

    /** Clamps a number to the range supported by short data type. */
    public static final short clampShort(int in) {
        return (in > Short.MAX_VALUE ? Short.MAX_VALUE :
                (in >= Short.MIN_VALUE ? (short)in : Short.MIN_VALUE));
    }

    /** Clamps a number to the range supported by integer data type. */
    public static final int clampInt(long in) {
	return (in > Integer.MAX_VALUE ? Integer.MAX_VALUE :
                (in >= Integer.MIN_VALUE ? (int)in : Integer.MIN_VALUE));
    }

    /** Clamps a number to the range supported by float data type. */
    public static final float clampFloat(double in) {
	return (in > Float.MAX_VALUE ? Float.MAX_VALUE :
                (in >= FLOAT_MIN ? (float)in : FLOAT_MIN));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * byte data type. The input number is float.
     */
    public static final byte clampRoundByte(float in) {
        return (in > 0xFF ? (byte)0xFF :
		(in >= 0 ? (byte)(in + 0.5F) : (byte)0));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * byte data type. The input number is double.
     */
    public static final byte clampRoundByte(double in) {
        return (in > 0xFF ? (byte)0xFF : (in >= 0 ? (byte)(in + 0.5) : (byte)0));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * unsigned short data type. The input number is float.
     */
    public static final short clampRoundUShort(float in) {
        return (in > 0xFFFF ? (short)0xFFFF :
		(in >= 0 ? (short)(in + 0.5F) : (short)0));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * unsigned short data type. The input number is double.
     */
    public static final short clampRoundUShort(double in) {
        return (in > 0xFFFF ? (short)0xFFFF :
                (in >= 0 ? (short)(in + 0.5) : (short)0));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * short data type. The input number is float.
     */
    public static final short clampRoundShort(float in) {
        return (in > Short.MAX_VALUE ? Short.MAX_VALUE :
                (in >= Short.MIN_VALUE ?
		 (short)Math.floor(in + 0.5F) : Short.MIN_VALUE));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * short data type. The input number is double.
     */
    public static final short clampRoundShort(double in) {
	return (in > Short.MAX_VALUE ? Short.MAX_VALUE :
                (in >= Short.MIN_VALUE ?
		 (short)Math.floor(in + 0.5) : Short.MIN_VALUE));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * integer data type. The input number is float.
     */
    public static final int clampRoundInt(float in) {
        return (in > Integer.MAX_VALUE ? Integer.MAX_VALUE :
                (in >= Integer.MIN_VALUE ?
		 (int)Math.floor(in + 0.5F) : Integer.MIN_VALUE));
    }

    /**
     * Clamps and rounds a number to the range supported by
     * integer data type. The input number is double.
     */
    public static final int clampRoundInt(double in) {
	return (in > Integer.MAX_VALUE ? Integer.MAX_VALUE :
                (in >= Integer.MIN_VALUE ?
		 (int)Math.floor(in + 0.5) : Integer.MIN_VALUE));
    }

    /** Clamps a positive number to the range supported by byte data type. */
    public static final byte clampBytePositive(int in) {
        return (in > 0xFF ? (byte)0xFF : (byte)in);
    }

    /** Clamps a negative number to the range supported by byte data type. */
    public static final byte clampByteNegative(int in) {
        return (in < 0 ? (byte)0 : (byte)in);
    }

    /**
     * Clamps a positive number to the range supported by
     * unsigned short data type.
     */
    public static final short clampUShortPositive(int in) {
        return (in > 0xFFFF ? (short)0xFFFF : (short)in);
    }

    /*
     * Clamps a negative number to the range supported by
     * unsigned short data type.
     */
    public static final short clampUShortNegative(int in) {
        return (in < 0 ? (short)0 : (short)in);
    }

    public static final void copyRaster(RasterAccessor src,
                                        RasterAccessor dst) {
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth() * dstPixelStride;
        int height = dst.getHeight() * dstLineStride;
        int bands = dst.getNumBands();

        switch (dst.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byte[][] bSrcData = src.getByteDataArrays();
            byte[][] bDstData = dst.getByteDataArrays();

            for (int b = 0; b < bands; b++) {
                byte[] s = bSrcData[b];
                byte[] d = bDstData[b];

                int heightEnd = dstBandOffsets[b] + height;

                for (int dstLineOffset = dstBandOffsets[b],
                     srcLineOffset = srcBandOffsets[b];
                     dstLineOffset < heightEnd;
                     dstLineOffset += dstLineStride,
                     srcLineOffset += srcLineStride) {

                    int widthEnd = dstLineOffset + width;

                    for (int dstPixelOffset = dstLineOffset,
                         srcPixelOffset = srcLineOffset;
                         dstPixelOffset < widthEnd;
                         dstPixelOffset += dstPixelStride,
                         srcPixelOffset += srcPixelStride) {

                        d[dstPixelOffset] = s[srcPixelOffset];
                    }
                }
            }
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            short[][] sSrcData = src.getShortDataArrays();
            short[][] sDstData = dst.getShortDataArrays();

            for (int b = 0; b < bands; b++) {
                short[] s = sSrcData[b];
                short[] d = sDstData[b];

                int heightEnd = dstBandOffsets[b] + height;

                for (int dstLineOffset = dstBandOffsets[b],
                     srcLineOffset = srcBandOffsets[b];
                     dstLineOffset < heightEnd;
                     dstLineOffset += dstLineStride,
                     srcLineOffset += srcLineStride) {

                    int widthEnd = dstLineOffset + width;

                    for (int dstPixelOffset = dstLineOffset,
                         srcPixelOffset = srcLineOffset;
                         dstPixelOffset < widthEnd;
                         dstPixelOffset += dstPixelStride,
                         srcPixelOffset += srcPixelStride) {

                        d[dstPixelOffset] = s[srcPixelOffset];
                    }
                }
            }
            break;

        case DataBuffer.TYPE_INT:
            int[][] iSrcData = src.getIntDataArrays();
            int[][] iDstData = dst.getIntDataArrays();

            for (int b = 0; b < bands; b++) {
                int[] s = iSrcData[b];
                int[] d = iDstData[b];

                int heightEnd = dstBandOffsets[b] + height;

                for (int dstLineOffset = dstBandOffsets[b],
                     srcLineOffset = srcBandOffsets[b];
                     dstLineOffset < heightEnd;
                     dstLineOffset += dstLineStride,
                     srcLineOffset += srcLineStride) {

                    int widthEnd = dstLineOffset + width;

                    for (int dstPixelOffset = dstLineOffset,
                         srcPixelOffset = srcLineOffset;
                         dstPixelOffset < widthEnd;
                         dstPixelOffset += dstPixelStride,
                         srcPixelOffset += srcPixelStride) {

                        d[dstPixelOffset] = s[srcPixelOffset];
                    }
                }
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[][] fSrcData = src.getFloatDataArrays();
            float[][] fDstData = dst.getFloatDataArrays();

            for (int b = 0; b < bands; b++) {
                float[] s = fSrcData[b];
                float[] d = fDstData[b];

                int heightEnd = dstBandOffsets[b] + height;

                for (int dstLineOffset = dstBandOffsets[b],
                     srcLineOffset = srcBandOffsets[b];
                     dstLineOffset < heightEnd;
                     dstLineOffset += dstLineStride,
                     srcLineOffset += srcLineStride) {

                    int widthEnd = dstLineOffset + width;

                    for (int dstPixelOffset = dstLineOffset,
                         srcPixelOffset = srcLineOffset;
                         dstPixelOffset < widthEnd;
                         dstPixelOffset += dstPixelStride,
                         srcPixelOffset += srcPixelStride) {

                        d[dstPixelOffset] = s[srcPixelOffset];
                    }
                }
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[][] dSrcData = src.getDoubleDataArrays();
            double[][] dDstData = dst.getDoubleDataArrays();

            for (int b = 0; b < bands; b++) {
                double[] s = dSrcData[b];
                double[] d = dDstData[b];

                int heightEnd = dstBandOffsets[b] + height;

                for (int dstLineOffset = dstBandOffsets[b],
                     srcLineOffset = srcBandOffsets[b];
                     dstLineOffset < heightEnd;
                     dstLineOffset += dstLineStride,
                     srcLineOffset += srcLineStride) {

                    int widthEnd = dstLineOffset + width;

                    for (int dstPixelOffset = dstLineOffset,
                         srcPixelOffset = srcLineOffset;
                         dstPixelOffset < widthEnd;
                         dstPixelOffset += dstPixelStride,
                         srcPixelOffset += srcPixelStride) {

                        d[dstPixelOffset] = s[srcPixelOffset];
                    }
                }
            }
            break;
        }

        if (dst.isDataCopy()) {
            dst.clampDataArrays();
            dst.copyDataToRaster();
        }
    }

    /**
     * Determines whether two SampleModels are "equal", i.e.,
     * assignment-compatible.  This signifies that the two SampleModels
     * are either the very same object or are two different objects
     * with identical characteristics.
     */
    public boolean areEqualSampleModels(SampleModel sm1, SampleModel sm2) {
        if(sm1 == sm2) {
            // Identical objects.
            return true;
        } else if(sm1.getClass() == sm2.getClass() &&
                  sm1.getDataType() == sm2.getDataType() &&
                  sm1.getTransferType() == sm2.getTransferType() &&
                  sm1.getWidth() == sm2.getWidth() &&
                  sm1.getHeight() == sm2.getHeight()) {
            // At this point all common attributes are equivalent. Next test
            // those specific to the known direct subclasses of SampleModel.
            // Subclasses which are not known will always return false.
            if(sm1 instanceof ComponentSampleModel) {
                ComponentSampleModel csm1 = (ComponentSampleModel)sm1;
                ComponentSampleModel csm2 = (ComponentSampleModel)sm2;
                return csm1.getPixelStride() == csm2.getPixelStride() &&
                    csm1.getScanlineStride() == csm2.getScanlineStride() &&
                    Arrays.equals(csm1.getBankIndices(),
                                  csm2.getBankIndices()) &&
                    Arrays.equals(csm1.getBandOffsets(),
                                  csm2.getBandOffsets());
            } else if(sm1 instanceof MultiPixelPackedSampleModel) {
                MultiPixelPackedSampleModel mpp1 =
                    (MultiPixelPackedSampleModel)sm1;
                MultiPixelPackedSampleModel mpp2 =
                    (MultiPixelPackedSampleModel)sm2;
                return mpp1.getPixelBitStride() == mpp2.getPixelBitStride() &&
                    mpp1.getScanlineStride() == mpp2.getScanlineStride() &&
                    mpp1.getDataBitOffset() == mpp2.getDataBitOffset();
            } else if(sm1 instanceof SinglePixelPackedSampleModel) {
                SinglePixelPackedSampleModel spp1 =
                    (SinglePixelPackedSampleModel)sm1;
                SinglePixelPackedSampleModel spp2 =
                    (SinglePixelPackedSampleModel)sm2;
                return spp1.getScanlineStride() == spp2.getScanlineStride() &&
                    Arrays.equals(spp1.getBitMasks(), spp2.getBitMasks());
            }
        }

        return false;
    }

    /// ---- BEGIN Binary data handling methods ----

    /**
     * Check whether a <code>SampleModel</code> represents a binary
     * data set, i.e., a single band of data with one bit per pixel
     * packed into a <code>MultiPixelPackedSampleModel</code>.
     */
    public static boolean isBinary(SampleModel sm) {
        return sm instanceof MultiPixelPackedSampleModel &&
            ((MultiPixelPackedSampleModel)sm).getPixelBitStride() == 1 &&
            sm.getNumBands() == 1;
    }

    /**
     * For the case of binary data (<code>isBinary()</code> returns
     * <code>true</code>), return the binary data as a packed byte array.
     * The data will be packed as eight bits per byte with no bit offset,
     * i.e., the first bit in each image line will be the left-most of the
     * first byte of the line.  The line stride in bytes will be
     * <code>(int)((getWidth()+7)/8)</code>.  The length of the returned
     * array will be the line stride multiplied by <code>getHeight()</code>
     *
     * @return the binary data as a packed array of bytes with zero offset
     * of <code>null</code> if the data are not binary.
     * @throws IllegalArgumentException if <code>isBinary()</code> returns
     * <code>false</code> with the <code>SampleModel</code> of the
     * supplied <code>Raster</code> as argument.
     */
    public static byte[] getPackedBinaryData(Raster raster,
                                             Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(JaiI18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int numBytesPerRow = (rectWidth + 7)/8;
        if(dataBuffer instanceof DataBufferByte &&
           eltOffset == 0 && bitOffset == 0 &&
           numBytesPerRow == lineStride &&
           ((DataBufferByte)dataBuffer).getData().length ==
           numBytesPerRow*rectHeight) {
            return ((DataBufferByte)dataBuffer).getData();
        }

        byte[] binaryDataArray = new byte[numBytesPerRow*rectHeight];

        int b = 0;

        if(bitOffset == 0) {
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();
                int stride = numBytesPerRow;
                int offset = 0;
                for(int y = 0; y < rectHeight; y++) {
                    System.arraycopy(data, eltOffset,
                                     binaryDataArray, offset,
                                     stride);
                    offset += stride;
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 8) {
                        short datum = data[i++];
                        binaryDataArray[b++] = (byte)((datum >>> 8) & 0xFF);
                        binaryDataArray[b++] = (byte)(datum & 0xFF);
                        xRemaining -= 16;
                    }
                    if(xRemaining > 0) {
                        binaryDataArray[b++] = (byte)((data[i] >>> 8) & 0XFF);
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 24) {
                        int datum = data[i++];
                        binaryDataArray[b++] = (byte)((datum >>> 24) & 0xFF);
                        binaryDataArray[b++] = (byte)((datum >>> 16) & 0xFF);
                        binaryDataArray[b++] = (byte)((datum >>> 8) & 0xFF);
                        binaryDataArray[b++] = (byte)(datum & 0xFF);
                        xRemaining -= 32;
                    }
                    int shift = 24;
                    while(xRemaining > 0) {
                        binaryDataArray[b++] =
                            (byte)((data[i] >>> shift) & 0xFF);
                        shift -= 8;
                        xRemaining -= 8;
                    }
                    eltOffset += lineStride;
                }
            }
        } else { // bitOffset != 0
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();

                if((bitOffset & 7) == 0) {
                    int stride = numBytesPerRow;
                    int offset = 0;
                    for(int y = 0; y < rectHeight; y++) {
                        System.arraycopy(data, eltOffset,
                                         binaryDataArray, offset,
                                         stride);
                        offset += stride;
                        eltOffset += lineStride;
                    }
                } else { // bitOffset % 8 != 0
                    int leftShift = bitOffset & 7;
                    int rightShift = 8 - leftShift;
                    for(int y = 0; y < rectHeight; y++) {
                        int i = eltOffset;
                        int xRemaining = rectWidth;
                        while(xRemaining > 0) {
                            if(xRemaining > rightShift) {
                                binaryDataArray[b++] =
                                    (byte)(((data[i++]&0xFF) << leftShift) |
                                           ((data[i]&0xFF) >>> rightShift));
                            } else {
                                binaryDataArray[b++] =
                                    (byte)((data[i]&0xFF) << leftShift);
                            }
                            xRemaining -= 8;
                        }
                        eltOffset += lineStride;
                    }
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
                    for(int x = 0; x < rectWidth; x += 8, bOffset += 8) {
                        int i = eltOffset + bOffset/16;
                        int mod = bOffset % 16;
                        int left = data[i] & 0xFFFF;
                        if(mod <= 8) {
                            binaryDataArray[b++] = (byte)(left >>> (8 - mod));
                        } else {
                            int delta = mod - 8;
                            int right = data[i+1] & 0xFFFF;
                            binaryDataArray[b++] =
                                (byte)((left << delta) |
                                       (right >>> (16 - delta)));
                        }
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
                    for(int x = 0; x < rectWidth; x += 8, bOffset += 8) {
                        int i = eltOffset + bOffset/32;
                        int mod = bOffset % 32;
                        int left = data[i];
                        if(mod <= 24) {
                            binaryDataArray[b++] =
                                (byte)(left >>> (24 - mod));
                        } else {
                            int delta = mod - 24;
                            int right = data[i+1];
                            binaryDataArray[b++] =
                                (byte)((left << delta) |
                                       (right >>> (32 - delta)));
                        }
                    }
                    eltOffset += lineStride;
                }
            }
        }

        return binaryDataArray;
    }

    /**
     * Returns the binary data unpacked into an array of bytes.
     * The line stride will be the width of the <code>Raster</code>.
     *
     * @throws IllegalArgumentException if <code>isBinary()</code> returns
     * <code>false</code> with the <code>SampleModel</code> of the
     * supplied <code>Raster</code> as argument.
     */
    public static byte[] getUnpackedBinaryData(Raster raster,
                                               Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(JaiI18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        byte[] bdata = new byte[rectWidth*rectHeight];
        int maxY = rectY + rectHeight;
        int maxX = rectX + rectWidth;
        int k = 0;

        if(dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte)dataBuffer).getData();
            for(int y = rectY; y < maxY; y++) {
                int bOffset = eltOffset*8 + bitOffset;
                for(int x = rectX; x < maxX; x++) {
                    byte b = data[bOffset/8];
                    bdata[k++] =
                        (byte)((b >>> (7 - bOffset & 7)) & 0x0000001);
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferShort ||
                  dataBuffer instanceof DataBufferUShort) {
            short[] data = dataBuffer instanceof DataBufferShort ?
                ((DataBufferShort)dataBuffer).getData() :
                ((DataBufferUShort)dataBuffer).getData();
            for(int y = rectY; y < maxY; y++) {
                int bOffset = eltOffset*16 + bitOffset;
                for(int x = rectX; x < maxX; x++) {
                    short s = data[bOffset/16];
                    bdata[k++] =
                        (byte)((s >>> (15 - bOffset % 16)) &
                               0x0000001);
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferInt) {
            int[] data = ((DataBufferInt)dataBuffer).getData();
            for(int y = rectY; y < maxY; y++) {
                int bOffset = eltOffset*32 + bitOffset;
                for(int x = rectX; x < maxX; x++) {
                    int i = data[bOffset/32];
                    bdata[k++] =
                        (byte)((i >>> (31 - bOffset % 32)) &
                               0x0000001);
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        }

        return bdata;
    }

    /**
     * Sets the supplied <code>Raster</code>'s data from an array
     * of packed binary data of the form returned by
     * <code>getPackedBinaryData()</code>.
     *
     * @throws IllegalArgumentException if <code>isBinary()</code> returns
     * <code>false</code> with the <code>SampleModel</code> of the
     * supplied <code>Raster</code> as argument.
     */
    public static void setPackedBinaryData(byte[] binaryDataArray,
                                           WritableRaster raster,
                                           Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(JaiI18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int b = 0;

        if(bitOffset == 0) {
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();
                if(data == binaryDataArray) {
                    // Optimal case: simply return.
                    return;
                }
                int stride = (rectWidth + 7)/8;
                int offset = 0;
                for(int y = 0; y < rectHeight; y++) {
                    System.arraycopy(binaryDataArray, offset,
                                     data, eltOffset,
                                     stride);
                    offset += stride;
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 8) {
                        data[i++] =
                            (short)(((binaryDataArray[b++] & 0xFF) << 8) |
                                    (binaryDataArray[b++] & 0xFF));
                        xRemaining -= 16;
                    }
                    if(xRemaining > 0) {
                        data[i++] =
                            (short)((binaryDataArray[b++] & 0xFF) << 8);
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 24) {
                        data[i++] =
                            (int)(((binaryDataArray[b++] & 0xFF) << 24) |
                                  ((binaryDataArray[b++] & 0xFF) << 16) |
                                  ((binaryDataArray[b++] & 0xFF) << 8) |
                                  (binaryDataArray[b++] & 0xFF));
                        xRemaining -= 32;
                    }
                    int shift = 24;
                    while(xRemaining > 0) {
                        data[i] |=
                            (int)((binaryDataArray[b++] & 0xFF) << shift);
                        shift -= 8;
                        xRemaining -= 8;
                    }
                    eltOffset += lineStride;
                }
            }
        } else { // bitOffset != 0
            int stride = (rectWidth + 7)/8;
            int offset = 0;
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();

                if((bitOffset & 7) == 0) {
                    for(int y = 0; y < rectHeight; y++) {
                        System.arraycopy(binaryDataArray, offset,
                                         data, eltOffset,
                                         stride);
                        offset += stride;
                        eltOffset += lineStride;
                    }
                } else { // bitOffset % 8 != 0
                    int rightShift = bitOffset & 7;
                    int leftShift = 8 - rightShift;
                    int leftShift8 = 8 + leftShift;
		    int mask = (byte)(255<<leftShift);
		    int mask1 = (byte)~mask;

                    for(int y = 0; y < rectHeight; y++) {
                        int i = eltOffset;
                        int xRemaining = rectWidth;
                        while(xRemaining > 0) {
                            byte datum = binaryDataArray[b++];

                            if (xRemaining > leftShift8) {
				// when all the bits in this BYTE will be set
				// into the data buffer.
                                data[i] = (byte)((data[i] & mask ) |
                                    ((datum&0xFF) >>> rightShift));
                                data[++i] = (byte)((datum & 0xFF) << leftShift);
                            } else if (xRemaining > leftShift) {
				// All the "leftShift" high bits will be set
				// into the data buffer.  But not all the
				// "rightShift" low bits will be set.
				data[i] = (byte)((data[i] & mask ) |
				    ((datum&0xFF) >>> rightShift));
				i++;
				data[i] =
				    (byte)((data[i] & mask1) | ((datum & 0xFF) << leftShift));
			    }
			    else {
				// Less than "leftShift" high bits will be set.
				int remainMask = (1 << leftShift - xRemaining) - 1;
                                data[i] =
                                    (byte)((data[i] & (mask | remainMask)) |
				    (datum&0xFF) >>> rightShift & ~remainMask);
                            }
                            xRemaining -= 8;
                        }
                        eltOffset += lineStride;
                    }
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

		int rightShift = bitOffset & 7;
		int leftShift = 8 - rightShift;
                int leftShift16 = 16 + leftShift;
		int mask = (short)(~(255 << leftShift));
		int mask1 = (short)(65535 << leftShift);
		int mask2 = (short)~mask1;

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
		    int xRemaining = rectWidth;
                    for(int x = 0; x < rectWidth;
			x += 8, bOffset += 8, xRemaining -= 8) {
                        int i = eltOffset + (bOffset >> 4);
                        int mod = bOffset & 15;
                        int datum = binaryDataArray[b++] & 0xFF;
                        if(mod <= 8) {
			    // This BYTE is set into one SHORT
			    if (xRemaining < 8) {
				// Mask the bits to be set.
				datum &= 255 << 8 - xRemaining;
			    }
                            data[i] = (short)((data[i] & mask) | (datum << leftShift));
                        } else if (xRemaining > leftShift16) {
			    // This BYTE will be set into two SHORTs
                            data[i] = (short)((data[i] & mask1) | ((datum >>> rightShift)&0xFFFF));
                            data[++i] =
                                (short)((datum << leftShift)&0xFFFF);
                        } else if (xRemaining > leftShift) {
			    // This BYTE will be set into two SHORTs;
			    // But not all the low bits will be set into SHORT
			    data[i] = (short)((data[i] & mask1) | ((datum >>> rightShift)&0xFFFF));
			    i++;
			    data[i] =
			        (short)((data[i] & mask2) | ((datum << leftShift)&0xFFFF));
			} else {
			    // Only some of the high bits will be set into
			    // SHORTs
			    int remainMask = (1 << leftShift - xRemaining) - 1;
			    data[i] = (short)((data[i] & (mask1 | remainMask)) |
				      ((datum >>> rightShift)&0xFFFF & ~remainMask));
			}
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();
                int rightShift = bitOffset & 7;
		int leftShift = 8 - rightShift;
		int leftShift32 = 32 + leftShift;
		int mask = 0xFFFFFFFF << leftShift;
		int mask1 = ~mask;

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
		    int xRemaining = rectWidth;
                    for(int x = 0; x < rectWidth;
			x += 8, bOffset += 8, xRemaining -= 8) {
                        int i = eltOffset + (bOffset >> 5);
                        int mod = bOffset & 31;
                        int datum = binaryDataArray[b++] & 0xFF;
                        if(mod <= 24) {
			    // This BYTE is set into one INT
			    int shift = 24 - mod;
			    if (xRemaining < 8) {
				// Mask the bits to be set.
				datum &= 255 << 8 - xRemaining;
			    }
                            data[i] = (data[i] & (~(255 << shift))) | (datum << shift);
                        } else if (xRemaining > leftShift32) {
			    // All the bits of this BYTE will be set into two INTs
                            data[i] = (data[i] & mask) | (datum >>> rightShift);
                            data[++i] = datum << leftShift;
                        } else if (xRemaining > leftShift) {
			    // This BYTE will be set into two INTs;
			    // But not all the low bits will be set into INT
                            data[i] = (data[i] & mask) | (datum >>> rightShift);
			    i++;
                            data[i] = (data[i] & mask1) | (datum << leftShift);
                        } else {
			    // Only some of the high bits will be set into INT
			    int remainMask = (1 << leftShift - xRemaining) - 1;
			    data[i] = (data[i] & (mask | remainMask)) |
				      (datum >>> rightShift & ~remainMask);
			}
                    }
                    eltOffset += lineStride;
                }
            }
        }
    }

    /**
     * Copies data into the packed array of the <code>Raster</code>
     * from an array of unpacked data of the form returned by
     * <code>getUnpackedBinaryData()</code>.
     *
     * <p> If the data are binary, then the target bit will be set if
     * and only if the corresponding byte is non-zero.
     *
     * @throws IllegalArgumentException if <code>isBinary()</code> returns
     * <code>false</code> with the <code>SampleModel</code> of the
     * supplied <code>Raster</code> as argument.
     */
    public static void setUnpackedBinaryData(byte[] bdata,
                                             WritableRaster raster,
                                             Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(JaiI18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int k = 0;

        if(dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*8 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/8] |=
                            (byte)(0x00000001 << (7 - bOffset & 7));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferShort ||
                  dataBuffer instanceof DataBufferUShort) {
            short[] data = dataBuffer instanceof DataBufferShort ?
                ((DataBufferShort)dataBuffer).getData() :
                ((DataBufferUShort)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*16 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/16] |=
                            (short)(0x00000001 <<
                                    (15 - bOffset % 16));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferInt) {
            int[] data = ((DataBufferInt)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*32 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/32] |=
                            (int)(0x00000001 <<
                                  (31 - bOffset % 32));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        }
    }

    /** Fill the specified rectangle of <code>raster</code> with the provided
     *  background values.  Suppose the raster is initialized to 0.  Thus,
     *  for binary data, if the provided background values are 0, do nothing.
     */
    public static void fillBackground(WritableRaster raster,
				      Rectangle rect,
				      double[] backgroundValues) {
	rect = rect.intersection(raster.getBounds());
	int numBands = raster.getSampleModel().getNumBands();
        SampleModel sm = raster.getSampleModel();
        PixelAccessor accessor = new PixelAccessor(sm, null);

        if (isBinary(sm)) {
            //fill binary data
            byte value = (byte)(((int)backgroundValues[0]) & 1);
            if (value == 0)
                return;
            int rectX = rect.x;
            int rectY = rect.y;
            int rectWidth = rect.width;
            int rectHeight = rect.height;

            int dx = rectX - raster.getSampleModelTranslateX();
            int dy = rectY - raster.getSampleModelTranslateY();

            DataBuffer dataBuffer = raster.getDataBuffer();
            MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
            int lineStride = mpp.getScanlineStride();
            int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
            int bitOffset = mpp.getBitOffset(dx);

            switch(sm.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                {
                    byte[] data = ((DataBufferByte)dataBuffer).getData();
                    int bits = bitOffset & 7;
                    int otherBits = (bits == 0) ? 0: 8 - bits;

                    byte mask = (byte)(255 >> bits);
                    int lineLength = (rectWidth - otherBits) / 8;
                    int bits1 = (rectWidth - otherBits) & 7;
                    byte mask1 = (byte)(255 << (8 - bits1));
                    // If operating within a single byte, merge masks into one
                    // and don't apply second mask after while loop
                    if (lineLength == 0) {
                        mask &= mask1;
                        bits1 = 0;
                    }

                    for (int y = 0; y < rectHeight; y++) {
                        int start = eltOffset;
                        int end = start + lineLength;
                        if (bits != 0)
                            data[start++] |= mask;
                        while (start < end)
                            data[start++] = (byte)255;
                        if (bits1 != 0)
                            data[start] |= mask1;
                        eltOffset += lineStride;
                    }
                    break;
                }
            case DataBuffer.TYPE_USHORT:
                {
                    short[] data = ((DataBufferUShort)dataBuffer).getData();
                    int bits = bitOffset & 15;
                    int otherBits = (bits == 0) ? 0: 16 - bits;

                    short mask = (short)(65535 >> bits);
                    int lineLength = (rectWidth - otherBits) / 16;
                    int bits1 = (rectWidth - otherBits) & 15;
                    short mask1 = (short)(65535 << (16 - bits1));
                    // If operating within a single byte, merge masks into one
                    // and don't apply second mask after while loop
                    if (lineLength == 0) {
                        mask &= mask1;
                        bits1 = 0;
                    }

                    for (int y = 0; y < rectHeight; y++) {
                        int start = eltOffset;
                        int end = start + lineLength;
                        if (bits != 0)
                            data[start++] |= mask;
                        while (start < end)
                            data[start++] = (short)0xFFFF;
                        if (bits1 != 0)
                            data[start++] |= mask1;
                        eltOffset += lineStride;
                    }
                    break;
                }
            case DataBuffer.TYPE_INT:
                {
                    int[] data = ((DataBufferInt)dataBuffer).getData();
                    int bits = bitOffset & 31;
                    int otherBits = (bits == 0) ? 0: 32 - bits;

                    int mask = 0xFFFFFFFF >> bits;
                    int lineLength = (rectWidth - otherBits) / 32;
                    int bits1 = (rectWidth - otherBits) & 31;
                    int mask1 = 0xFFFFFFFF << (32 - bits1);
                    // If operating within a single byte, merge masks into one
                    // and don't apply second mask after while loop
                    if (lineLength == 0) {
                        mask &= mask1;
                        bits1 = 0;
                    }

                    for (int y = 0; y < rectHeight; y++) {
                        int start = eltOffset;
                        int end = start + lineLength;
                        if (bits != 0)
                            data[start++] |= mask;
                        while (start < end)
                            data[start++] = 0xFFFFFFFF;
                        if (bits1 != 0)
                            data[start++] |= mask1;
                        eltOffset += lineStride;
                    }
                    break;
                }

            }
        } else {
            int srcSampleType = accessor.sampleType == PixelAccessor.TYPE_BIT ?
                DataBuffer.TYPE_BYTE : accessor.sampleType;
            UnpackedImageData uid = accessor.getPixels(raster, rect,
                                                    srcSampleType, false);
            rect = uid.rect;
            int lineStride = uid.lineStride;
            int pixelStride = uid.pixelStride;

            switch(uid.type) {
            case DataBuffer.TYPE_BYTE:
                byte[][] bdata = uid.getByteData();
                for (int b = 0; b < accessor.numBands; b++) {
                    byte value = (byte)backgroundValues[b];
                    byte[] bd = bdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            bd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[][] sdata = uid.getShortData();
                for (int b = 0; b < accessor.numBands; b++) {
                    short value = (short)backgroundValues[b];
                    short[] sd = sdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            sd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_INT:
                int[][] idata = uid.getIntData();
                for (int b = 0; b < accessor.numBands; b++) {
                    int value = (int)backgroundValues[b];
                    int[] id = idata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            id[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                float[][] fdata = uid.getFloatData();
                for (int b = 0; b < accessor.numBands; b++) {
                    float value = (float)backgroundValues[b];
                    float[] fd = fdata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            fd[po] = value;
                        }
                    }
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                double[][] ddata = uid.getDoubleData();
                for (int b = 0; b < accessor.numBands; b++) {
                    double value = backgroundValues[b];
                    double[] dd = ddata[b];
                    int lastLine = uid.bandOffsets[b] + rect.height * lineStride;

                    for (int lo = uid.bandOffsets[b]; lo < lastLine; lo += lineStride) {
                        int lastPixel = lo + rect.width * pixelStride;
                        for (int po = lo; po < lastPixel; po += pixelStride) {
                            dd[po] = value;
                        }
                    }
                }
                break;
            }
        }
    }

    /** When the destination rectangle is not the same as the image bounds,
     *  should fill the border.
     */
    public static void fillBordersWithBackgroundValues(Rectangle outerRect,
						       Rectangle innerRect,
						       WritableRaster raster,
						       double[] backgroundValues) {
        int outerMaxX = outerRect.x + outerRect.width;
        int outerMaxY = outerRect.y + outerRect.height;

        int innerMaxX = innerRect.x + innerRect.width;
        int innerMaxY = innerRect.y + innerRect.height;

        if (outerRect.x < innerRect.x) {
            Rectangle rect = new Rectangle(outerRect.x, innerRect.y,
                                           innerRect.x - outerRect.x,
                                           outerMaxY - innerRect.y);
            fillBackground(raster, rect, backgroundValues);
        }

        if (outerRect.y < innerRect.y) {
            Rectangle rect = new Rectangle(outerRect.x, outerRect.y,
                                           innerMaxX - outerRect.x,
                                           innerRect.y - outerRect.y);
            fillBackground(raster, rect, backgroundValues);
        }

        if (outerMaxX > innerMaxX) {
            Rectangle rect = new Rectangle(innerMaxX, outerRect.y,
                                           outerMaxX - innerMaxX,
                                           innerMaxY - outerRect.y);
            fillBackground(raster, rect, backgroundValues);
        }

        if (outerMaxY > innerMaxY) {
            Rectangle rect = new Rectangle(innerRect.x, innerMaxY,
                                           outerMaxX - innerRect.x,
                                           outerMaxY - innerMaxY);
            fillBackground(raster, rect, backgroundValues);
        }
    }

    /// ---- END Binary data handling methods ----

    /**
      * Given a kernel and the gain (sharpness) factor of an
      * UnsharpMask operation, compute a modified kernel that
      * would be equivalent to the specified unsharp operation.
      *
      * for UnsharpMask function we have the following formula:
      *
      * dst(i,j) = src(i,j) + gain *
      *			(src(i,j) - SUM  SUM  K(l,m) * src(i+l,j+m))
      *				     l    m
      *
      * Which can be written as :
      *
      * dst(i,j) = SUM  SUM  Q(l,m) * src(i+l,j+m),
      *             l    m
      *
      * where Q(0,0) = 1 + gain * (1 - K(0,0)), and
      *	      Q(l,m) = - gain * K(l,m)  otherwise
      *
      * @param kernel the unsharp mask kernel
      * @param gain the unsharp mask gain (sharpness) factor.
      *
      * @return an equivalent convolution KernelJAI
      */
    public static KernelJAI getUnsharpMaskEquivalentKernel(
			    KernelJAI kernel, float gain) {

	int width   = kernel.getWidth();
	int height  = kernel.getHeight();
	int xOrigin = kernel.getXOrigin();
	int yOrigin = kernel.getYOrigin();

	float oldData[] = kernel.getKernelData();
	float newData[] = new float[oldData.length];

	int k;

	for (k = 0; k < width*height; k++)
	    newData[k] = -gain * oldData[k];

	k = yOrigin*width + xOrigin;
	newData[k] = 1.0f + gain * (1.0f - oldData[k]);

	return new KernelJAI(width, height, xOrigin, yOrigin, newData);
    }

    /**
     * Retrieve the indices of a set of tiles in row-major order with
     * the given tile index bounds in x and y.
     */
    public static final Point[] getTileIndices(int txmin, int txmax,
                                               int tymin, int tymax) {
        if(txmin > txmax || tymin > tymax) {
            return null;
        }

        Point[] tileIndices =
            new Point[(txmax - txmin + 1)*(tymax - tymin + 1)];
        int k = 0;
        for (int tj = tymin; tj <= tymax; tj++) {
            for (int ti = txmin; ti <= txmax; ti++) {
                tileIndices[k++] = new Point(ti, tj);
            }
        }

        return tileIndices;
    }

    /// Method for handling DeferrdData objects in ParameterBlocks.

    /**
     * If any <code>DeferredData</code> components are detected,
     * the argument is cloned and the <code>DeferredData</code>
     * object is replaced with what its <code>getData()</code> returns.
     */
    public static Vector evaluateParameters(Vector parameters) {
        if(parameters == null) {
            throw new IllegalArgumentException();
        }

        Vector paramEval = parameters;

        int size = parameters.size();
        for(int i = 0; i < size; i++) {
            Object element = parameters.get(i);
            if(element instanceof DeferredData) {
                if(paramEval == parameters) {
                    paramEval = (Vector)parameters.clone();
                }
                paramEval.set(i, ((DeferredData)element).getData());
            }
        }

        return paramEval;
    }

    /**
     * If any <code>DeferredData</code> parameters are detected,
     * a new <code>ParameterBlock</code> is constructed and the
     * <code>DeferredData</code> object is replaced with what its
     * <code>getData()</code> returns.
     */
    public static ParameterBlock evaluateParameters(ParameterBlock pb) {
        if(pb == null) {
            throw new IllegalArgumentException();
        }

        Vector parameters = pb.getParameters();
        Vector paramEval = evaluateParameters(parameters);
        return paramEval == parameters ?
            pb : new ParameterBlock(pb.getSources(), paramEval);
    }

    /**
     * Derive a compatible <code>ColorModel</code> for the supplied
     * <code>SampleModel</code> using the method specified via the
     * <code>OpImage</code> configuration <code>Map</code>.
     *
     * @return a compatible <code>ColorModel</code> or <code>null</code>.
     */
    public static ColorModel getCompatibleColorModel(SampleModel sm,
                                                     Map config) {
        ColorModel cm = null;

        if(config == null ||
           !Boolean.FALSE.equals(
               config.get(JAI.KEY_DEFAULT_COLOR_MODEL_ENABLED))) {

            // Set the default ColorModel

            if(config != null &&
               config.containsKey(JAI.KEY_DEFAULT_COLOR_MODEL_METHOD)) {
                // Attempt to retrieve the default CM Method.
                Method cmMethod =
                    (Method)config.get(JAI.KEY_DEFAULT_COLOR_MODEL_METHOD);

                // Check method compatibility.
                Class[] paramTypes = cmMethod.getParameterTypes();
                if((cmMethod.getModifiers() & Modifier.STATIC) !=
                   Modifier.STATIC) {
                    // Method must be static.
                    throw new RuntimeException(JaiI18N.getString("ImageUtil1"));
                } else if(cmMethod.getReturnType() != ColorModel.class) {
                    // Method must return a ColorModel.
                    throw new RuntimeException(JaiI18N.getString("ImageUtil2"));
                } else if(paramTypes.length != 1 ||
                          !paramTypes[0].equals(SampleModel.class)) {
                    // Unique Method parameter must be a SampleModel.
                    throw new RuntimeException(JaiI18N.getString("ImageUtil3"));
                }

                // Set the default ColorModel.
                try {
                    // Attempt to use the supplied Method.
                    Object[] args = new Object[] {sm};
                    cm = (ColorModel)cmMethod.invoke(null, args);
                } catch(Exception e) {
                    String message =
                        JaiI18N.getString("ImageUtil4") + cmMethod.getName();
                    sendExceptionToListener(message ,
                                            new ImagingException(message, e));
/*
                    // XXX Is this a reasonable Exception to throw?
                    throw new RuntimeException(cmMethod.getName()+" "+
                                               e.getMessage());
*/
                }
            } else { // No default method hint set.
                // Use PlanarImage method.
                cm = PlanarImage.createColorModel(sm);
            }
        }

        return cm;
    }

    /**
     * Converts the supplied <code>Exception</code>'s stack trace
     * to a <code>String</code>.
     */
    public static String getStackTraceString(Exception e) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteStream);
        e.printStackTrace(printStream);
        printStream.flush();
        String stackTraceString = byteStream.toString();
        printStream.close();
        return stackTraceString;
    }

    public static ImagingListener getImagingListener(RenderingHints hints) {
        ImagingListener listener = null;
        if (hints != null)
            listener = (ImagingListener)hints.get(JAI.KEY_IMAGING_LISTENER);

        if (listener == null)
            listener = JAI.getDefaultInstance().getImagingListener();
        return listener;
    }

    public static ImagingListener getImagingListener(RenderContext context) {
        return getImagingListener(context.getRenderingHints());
    }

    /**
     * Generates a UID for the provided <code>Object</code>.
     *  The counter for the objects that request an ID, the hashcode of the
     *  class of the provided object, the hashcode of the provided object,
     *  the current time in milli seconds, and a random number are
     *  concatenated together in a <code>BigInteger</code>.  This
     *  <code>BigInteger</code> is returned as the unique ID.
     */
    public static synchronized Object generateID(Object owner) {
        Class c = owner.getClass();
        counter++;

        byte[] uid = new byte[32];
        int k = 0;
        for (int i = 7, j = 0; i >=0; i--, j += 8)
            uid[k++] = (byte)(counter >> j);
        int hash = c.hashCode();
        for (int i = 3, j = 0; i >= 0; i--, j += 8)
            uid[k++] = (byte)(hash >> j);
        hash = owner.hashCode();
        for (int i = 3, j = 0; i >= 0; i--, j += 8)
            uid[k++] = (byte)(hash >> j);
        long time = System.currentTimeMillis();
        for (int i = 7, j = 0; i >=0; i--, j += 8)
            uid[k++] = (byte)(time >> j);
        long rand =
            Double.doubleToLongBits(new Double(Math.random()).doubleValue());
        for (int i = 7, j = 0; i >=0; i--, j += 8)
            uid[k++] = (byte)(rand>> j);
        return new BigInteger(uid);
    }

    static void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
            getImagingListener((RenderingHints)null);
        listener.errorOccurred(message, e, ImageUtil.class, false);
    }
}
