/*
 * $RCSfile: PiecewiseOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:40 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import com.lightcrafts.mediax.jai.ColormapOpImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the "Piecewise" operation.
 *
 * <p> The "Piecewise" operation maps the pixel values of an image using
 * a piecewise linear function represented by a set of breakpoints for each
 * band. The abscissa of each breakpoint is the source image gray level for
 * the band in question and the ordinate is the destination image gray level
 * to which it is mapped.
 *
 * @see com.lightcrafts.mediax.jai.operator.PiecewiseDescriptor
 * @see PiecewiseCRIF
 *
 *
 * @since EA4
 */
final class PiecewiseOpImage extends ColormapOpImage {
    /** The abscissas of the breakpoints. */
    private float[][] abscissas;

    /** The slope at each abscissa. */
    private float[][] slopes;

    /** The intercept at each abscissa. */
    private float[][] intercepts;

    /** The minimum values of the output per band. */
    private float[] minOrdinates;

    /** The maximum values of the output per band. */
    private float[] maxOrdinates;

    /** Flag indicating byte data. */
    private boolean isByteData = false;

    /** A lookup table for use in the case of byte data. */
    private LookupTableJAI lut;

    /**
     * Find the ordinate value for a given abscissa value.
     *
     * @param x The abscissa array.
     * @param minValue The minimum source gray level in the breakpoint set.
     * @param maxValue The maximum source gray level in the breakpoint set.
     * @param a The array of piecewise slopes.
     * @param b The array of piecewise ordinate intercepts.
     * @param value The source gray level.
     * @return The destination gray level.
     */
    private static float binarySearch(float[] x,
                                      float minValue, float maxValue,
                                      float[] a, float[] b,
                                      float value) {
        int highIndex = x.length - 1;

        if(value <= x[0]) {
            return minValue;
        } else if(value >= x[highIndex]) {
            return maxValue;
        }

        int lowIndex = 0;
        int deltaIndex = highIndex - lowIndex;

        while(deltaIndex > 1) {
            int meanIndex = lowIndex + deltaIndex/2;
            if(value >= x[meanIndex]) {
                lowIndex = meanIndex;
            } else {
                highIndex = meanIndex;
            }
            deltaIndex = highIndex - lowIndex;
        }

        return a[lowIndex]*value + b[lowIndex];
    }

    /**
     * Constructor.
     *
     * @param source       The source image.
     * @param layout       The destination image layout.
     * @param breakpoints  The piecewise mapping stored by reference. The
     * arrays breakpoints[b][0] and breakpoints[b][1] represent the abscissas
     * and ordinates of the breakpoints, respectively, for band <i>b</i>. The
     * number of sets of breakpoints must be one or equal to the number of
     * image bands.
     */
    public PiecewiseOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout,
                            float[][][] breakpoints) {
        super(source, layout, config, true);

        // Ensure that the number of sets of breakpoints is either unity
        // or equal to the number of bands.
        int numBands = sampleModel.getNumBands();

        // Initalize the instance variables.
        initFields(numBands, breakpoints);

        // Set the byte data flag.
        isByteData = sampleModel.getTransferType() == DataBuffer.TYPE_BYTE;

        // Perform byte-specific initialization.
        if(isByteData) {
            // Initialize the lookup table.
            createLUT();

            // Clear the other instance variables for the garbage collector.
            unsetFields();
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();
    }

    /**
     * Transform the colormap according to the rescaling parameters.
     */
    protected void transformColormap(byte[][] colormap) {

	byte byteTable[][] = lut.getByteData();

        for(int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
	    byte[] luTable = byteTable[b >= byteTable.length ? 0 : b];
            int mapSize = map.length;

            for(int i = 0; i < mapSize; i++) {
                map[i] = luTable[(map[i] & 0xFF)];
            }
        }
    }

    /**
     * Initialize various instance variables from the array of breakpoints.
     * The principal derived values are the slope and ordinate-intercept of
     * each piecewise segment.
     *
     * @param numBands The number of bands in the image.
     * @param The breakpoints as breakpoints[numBands][0..1][numPoints].
     */
    private void initFields(int numBands, float[][][] breakpoints) {
        abscissas = new float[numBands][];
        slopes = new float[numBands][];
        intercepts = new float[numBands][];
        minOrdinates = new float[numBands];
        maxOrdinates = new float[numBands];

        for(int band = 0; band < numBands; band++) {
            abscissas[band] = breakpoints.length == 1 ?
                breakpoints[0][0] : breakpoints[band][0];
            int maxIndex = abscissas[band].length - 1;

            minOrdinates[band] = breakpoints.length == 1 ?
                breakpoints[0][1][0] : breakpoints[band][1][0];
            maxOrdinates[band] = breakpoints.length == 1 ?
                breakpoints[0][1][maxIndex] : breakpoints[band][1][maxIndex];

            slopes[band] = new float[maxIndex];
            intercepts[band] = new float[maxIndex];

            float[] x =  abscissas[band];
            float[] y =  breakpoints.length == 1 ?
                breakpoints[0][1] : breakpoints[band][1];
            float[] a = slopes[band];
            float[] b = intercepts[band];
            for(int i1 = 0; i1 < maxIndex; i1++) {
                int i2 = i1 + 1;
                a[i1] = (y[i2]-y[i1])/(x[i2] - x[i1]);
                b[i1] = y[i1] - x[i1]*a[i1];
            }
        }
    }

    
    /**
     * Clear all instance fields which are ununsed references so the GC
     * may clear them.
     */
    private void unsetFields() {
        abscissas = null;
        slopes = null;
        intercepts = null;
        minOrdinates = null;
        maxOrdinates = null;
    }

    /**
     * Create a lookup table to be used in the case of byte data.
     */
    private void createLUT() {
        // Allocate memory for the data array references.
        int numBands = abscissas.length;
        byte[][] data = new byte[numBands][];

        // Generate the data for each band.
        for(int band = 0; band < numBands; band++) {
            // Allocate memory for this band.
            data[band] = new byte[256];

            // Cache the references to avoid extra indexing.
            byte[] table = data[band];
            float[] x = abscissas[band];
            float[] a = slopes[band];
            float[] b = intercepts[band];
            float yL = minOrdinates[band];
            float yH = maxOrdinates[band];

            // Initialize the lookup table data.
            for(int value = 0; value < 256; value++) {
                table[value] =
                    ImageUtil.clampRoundByte(binarySearch(x, yL, yH, a, b, value));
            }
        }

        // Construct the lookup table.
        lut = new LookupTableJAI(data);
    }

    /**
     * Piecewises to the pixel values within a specified rectangle.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        if(isByteData) {
            computeRectByte(sources, dest, destRect);
        } else {
            RasterAccessor dst =
                new RasterAccessor(dest, destRect, formatTags[1],
                                   getColorModel());
            RasterAccessor src =
                new RasterAccessor(sources[0], destRect, formatTags[0],
                                   getSource(0).getColorModel());

            switch (dst.getDataType()) {
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(src, dst);
                break;
            case DataBuffer.TYPE_SHORT:
                computeRectShort(src, dst);
                break;
            case DataBuffer.TYPE_INT:
                computeRectInt(src, dst);
                break;
            case DataBuffer.TYPE_FLOAT:
                computeRectFloat(src, dst);
                break;
            case DataBuffer.TYPE_DOUBLE:
                computeRectDouble(src, dst);
                break;
            }

            dst.copyDataToRaster();
        }
    }

    private void computeRectByte(Raster[] sources,
                                 WritableRaster dest,
                                 Rectangle destRect) {
        lut.lookup(sources[0], dest, destRect);
    }

    private void computeRectUShort(RasterAccessor src,
                                   RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        for (int b = 0; b < dstBands; b++) {
            short[] d = dstData[b];
            short[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            // Cache the references to avoid extra indexing.
            float[] x = abscissas[b];
            float[] gain = slopes[b];
            float[] bias = intercepts[b];
            float yL = minOrdinates[b];
            float yH = maxOrdinates[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] =
                        ImageUtil.clampRoundUShort(binarySearch(x, yL, yH, gain, bias,
                                                      s[srcPixelOffset] &
                                                      0xFFFF));

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor src,
                                  RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        for (int b = 0; b < dstBands; b++) {
            short[] d = dstData[b];
            short[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            // Cache the references to avoid extra indexing.
            float[] x = abscissas[b];
            float[] gain = slopes[b];
            float[] bias = intercepts[b];
            float yL = minOrdinates[b];
            float yH = maxOrdinates[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] =
                        ImageUtil.clampRoundShort(binarySearch(x, yL, yH, gain, bias,
                                                     s[srcPixelOffset]));

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src,
                                RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        for (int b = 0; b < dstBands; b++) {
            int[] d = dstData[b];
            int[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            // Cache the references to avoid extra indexing.
            float[] x = abscissas[b];
            float[] gain = slopes[b];
            float[] bias = intercepts[b];
            float yL = minOrdinates[b];
            float yH = maxOrdinates[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] =
                        ImageUtil.clampRoundInt(binarySearch(x, yL, yH, gain, bias,
                                                   s[srcPixelOffset]));

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor src,
                                  RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        float[][] srcData = src.getFloatDataArrays();

        for (int b = 0; b < dstBands; b++) {
            float[] d = dstData[b];
            float[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            // Cache the references to avoid extra indexing.
            float[] x = abscissas[b];
            float[] gain = slopes[b];
            float[] bias = intercepts[b];
            float yL = minOrdinates[b];
            float yH = maxOrdinates[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] =
                        binarySearch(x, yL, yH, gain, bias,
                                     s[srcPixelOffset]);

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor src,
                                   RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        double[][] dstData = dst.getDoubleDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        double[][] srcData = src.getDoubleDataArrays();

        for (int b = 0; b < dstBands; b++) {
            double[] d = dstData[b];
            double[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            // Cache the references to avoid extra indexing.
            float[] x = abscissas[b];
            float[] gain = slopes[b];
            float[] bias = intercepts[b];
            float yL = minOrdinates[b];
            float yH = maxOrdinates[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] =
                        binarySearch(x, yL, yH, gain, bias,
                                     (float)s[srcPixelOffset]);

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }
}
