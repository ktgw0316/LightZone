/*
 * $RCSfile: UnpackedImageData.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:23 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;

/**
 * This class is used by <code>PixelAccessor</code> to store unpacked
 * image data and the information needed to access it.  The data are stored in
 * a two-dimensional primitive array.
 *
 * @since JAI 1.1
 *
 */
public final class UnpackedImageData {

    /** The <code>Raster</code> containing the pixel data. */
    public final Raster raster;

    /**
     * The rectangular region within the <code>Raster</code> where the
     * data are to be accessed.
     */
    public final Rectangle rect;

    /** The type of the primitive array used to store the data. */
    public final int type;

    /** The data array supplied to store the unpacked data. */
    public final Object data;

    /**
     * The number of array elements to skip to get to the next
     * pixel on the same scanline.
     */
    public final int pixelStride;

    /** The number of array elements per scanline. */
    public final int lineStride;

    /**
     * The number of array elements from the beginning of the data array
     * to the first pixel of the <code>Rectangle</code> for all bands.
     */
    public final int[] bandOffsets;

    /**
     * Indicates whether the <code>PixelAccessor</code> can and must set the
     * data back into the <code>Raster</code>. If the data does not need 
     * to be copied back to the <code>Raster</code>, this variable should be 
     * set to <code>false</code>. Only destinations can be set.
     */
    public final boolean convertToDest;

    
    /** Constructs a PackedImageRaster.
     *  @param raster The <code>Raster</code> containing the pixel data.
     *  @param rect The rectangular region containing the data.
     *  @param type  The type of the primitive array supplied to store the data.
     *  @param data The data array supplied to store the data.
     *  @param pixelStride The data array increment needed to move from band x
     *                     of pixel i to band x of pixel i+1 on the same 
                           scanline.
     *  @param lineStride The data array increment to move from the pixel x
     *                    of line i to pixel x of line i+1.
     *  @param bandOffsets The number of bytes from the start of the data array
     *                 to the location of the first pixel of the rectangle
     *                 for all bands.
     *  @param convertToDest A <code>boolean</code> indicating whether the data can and
     *             must be set back into the <code>Raster</code>. This applies
     *             only to destinations.
     */
    public UnpackedImageData(Raster    raster,
                             Rectangle rect,
                             int       type,
                             Object    data,
                             int       pixelStride,
                             int       lineStride,
                             int[]     bandOffsets,
                             boolean   convertToDest) {
        this.raster = raster;
        this.rect = rect;
        this.type = type;
        this.data = data;
        this.pixelStride = pixelStride;
        this.lineStride = lineStride;
        this.bandOffsets = bandOffsets;
        this.convertToDest = convertToDest;
    }

    /**
     * Returns the two-dimensional byte data array, or <code>null</code>
     * if the data are not stored in a byte array.
     * The array format is <code>data[band][]</code> where the second
     * index is navigated using the pixel and line strides.
     * @return The two-dimensional byte data array.
     */
    public byte[][] getByteData() {
        return type == DataBuffer.TYPE_BYTE ? (byte[][])data : null;
    }

    /**
     * Returns byte data array for a specific band, or <code>null</code>
     * if the data are not stored in a byte array.
     * @param b The band whose data array is to be retrieved.
     * @return The one-dimensional byte data array for the requested band.
     */
    public byte[] getByteData(int b) {
        byte[][] d = getByteData();
        return d == null ? null : d[b];
    }

    /**
     * Returns the two-dimensional short data array, or <code>null</code>
     * if the data are not stored in a short array.
     * The array format is <code>data[band][]</code> where the second
     * index is navigated using the pixel and line strides.
     * @return The two-dimensional short data array.
     */
    public short[][] getShortData() {
        return (type == DataBuffer.TYPE_USHORT ||
                type == DataBuffer.TYPE_SHORT) ?
               (short[][])data : null;
    }

    /**
     * Returns short data array for a specific band, or <code>null</code>
     * if the data are not stored in a short array.
     * @param b The band whose data array is to be retrieved.
     * @return The one-dimensional short data array for the requested band.
     */
    public short[] getShortData(int b) {
        short[][] d = getShortData();
        return d == null ? null : d[b];
    }

    /**
     * Returns the two-dimensional integer data array, or <code>null</code>
     * if the data are not stored in an integer array.
     * The array format is <code>data[band][]</code> where the second
     * index is navigated using the pixel and line strides.
     * @return The two-dimensional int data array.
     */
    public int[][] getIntData() {
        return type == DataBuffer.TYPE_INT ? (int[][])data : null;
    }

    /**
     * Returns integer data array for a specific band, or <code>null</code>
     * if the data are not stored in an integer array.
     * @param b The band whose data array is to be retrieved.
     * @return The one-dimensional int data array for the requested band.
     */
    public int[] getIntData(int b) {
        int[][] d = getIntData();
        return d == null ? null : d[b];
    }

    /**
     * Returns the two-dimensional float data array, or <code>null</code>
     * if the data are not stored in a float array.
     * The array format is <code>data[band][]</code> where the second
     * index is navigated using the pixel and line strides.
     * @return The two-dimensional float data array.
     */
    public float[][] getFloatData() {
        return type == DataBuffer.TYPE_FLOAT ? (float[][])data : null;
    }

    /**
     * Returns float data array for a specific band, or <code>null</code>
     * if the data are not stored in a float array.
     * @param b The band whose data array is to be retrieved.
     * @return The one-dimensional float data array for the requested band.
     */
    public float[] getFloatData(int b) {
        float[][] d = getFloatData();
        return d == null ? null : d[b];
    }

    /**
     * Returns the two-dimensional double data array, or <code>null</code>
     * if the data are not stored in a double array.
     * The array format is <code>data[band][]</code> where the second
     * index is navigated using the pixel and line strides.
     * @return The two-dimensional double data array.
     */
    public double[][] getDoubleData() {
        return type == DataBuffer.TYPE_DOUBLE ? (double[][])data : null;
    }

    /**
     * Returns double data array for a specific band, or <code>null</code>
     * if the data are not stored in a double array.
     * @param b The band whose data array is to be retrieved.
     * @return The one-dimensional double data array for the requested band.
     */
    public double[] getDoubleData(int b) {
        double[][] d = getDoubleData();
        return d == null ? null : d[b];
    }

    /** Returns the offset for a band.
     *  @param b The band whose offset is to be returned.
     *  @return The offset of the requested band.
     */
    public int getOffset(int b) {
        return bandOffsets[b];
    }

    /** Returns the minimum offset of all bands.
     *  @return The minimum offset of all bands.
     */
    public int getMinOffset() {
        int min = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++) {
            min = Math.min(min, bandOffsets[i]);
        }
        return min;
    }

    /** Returns the maximum offset of all bands.
     *  @return The maximum offset of all the bands.
     */
    public int getMaxOffset() {
        int max = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++) {
            max = Math.max(max, bandOffsets[i]);
        }
        return max;
    }
}
