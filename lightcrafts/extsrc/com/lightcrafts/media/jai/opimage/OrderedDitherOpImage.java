/*
 * $RCSfile: OrderedDitherOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:38 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ColorCube;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the ordered dither operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.OrderedDitherDescriptor</code>.
 *
 * <p>This <code>OpImage</code> performs dithering of its source image into
 * a single band image using a specified color cube and dither mask.
 *
 * @see com.lightcrafts.mediax.jai.KernelJAI
 * @see com.lightcrafts.mediax.jai.ColorCube
 *
 * @since EA3
 *
 */
final class OrderedDitherOpImage extends PointOpImage {
    /**
     * Flag indicating that the generic implementation is used.
     */
    private static final int TYPE_OD_GENERAL = 0;

    /**
     * Flag indicating that the optimized three-band implementation is used
     * (byte data only).
     */
    private static final int TYPE_OD_BYTE_LUT_3BAND = 1;

    /**
     * Flag indicating that the optimized N-band implementation is used
     * (byte data only).
     */
    private static final int TYPE_OD_BYTE_LUT_NBAND = 2;

    /**
     * Maximim dither LUT size: 16x16 4-band byte dither mask.
     */
    private static final int DITHER_LUT_LENGTH_MAX = 16*16*4*256;

    /**
     * The maximum number of elements in the <code>DitherLUT</code> cache.
     */
    private static final int DITHER_LUT_CACHE_LENGTH_MAX = 4;

    /**
     * A cache of <code>SoftReference</code>s to <code>DitherLUT</code>
     * inner class instances.
     */
    private static Vector ditherLUTCache =
        new Vector(0, DITHER_LUT_CACHE_LENGTH_MAX);

    /**
     * Flag indicating the implementation to be used.
     */
    private int odType = TYPE_OD_GENERAL;

    /**
     * The number of bands in the source image.
     */
    protected int numBands;

    /**
     * The array of color cube dimensions-less-one.
     */
    protected int[] dims;

    /**
     * The array of color cube multipliers.
     */
    protected int[] mults;

    /**
     * The adjusted offset of the color cube.
     */
    protected int adjustedOffset;

    /**
     * The width of the dither mask.
     */
    protected int maskWidth;

    /**
     * The height of the dither mask.
     */
    protected int maskHeight;

    /**
     * The dither mask matrix scaled by 255.
     */
    protected byte[][] maskDataByte;

    /**
     * The dither mask matrix scaled to USHORT range.
     */
    protected int[][] maskDataInt;

    /**
     * The dither mask matrix scaled to "unsigned int" range.
     */
    protected long[][] maskDataLong;

    /**
     * The dither mask matrix.
     */
    protected float[][] maskDataFloat;

    /**
     * An inner class instance representing a dither lookup table. Used
     * for byte data only when the table size is within a specified limit.
     */
    protected DitherLUT odLUT = null;

    /**
     * Force the destination image to be single-banded.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            ColorCube colorMap) {
        ImageLayout il;
        if (layout == null) {
            il = new ImageLayout(source);
        } else {
            il = (ImageLayout)layout.clone();
        }

        // Get the SampleModel.
        SampleModel sm = il.getSampleModel(source);

        // Ensure an appropriate SampleModel.
        if(colorMap.getNumBands() == 1 &&
           colorMap.getNumEntries() == 2 &&
           !ImageUtil.isBinary(il.getSampleModel(source))) {
            sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                 il.getTileWidth(source),
                                                 il.getTileHeight(source),
                                                 1);
            il.setSampleModel(sm);
        }

        // Make sure that this OpImage is single-banded.
        if (sm.getNumBands() != 1) {
            // TODO: Force to SHORT or USHORT if FLOAT or DOUBLE?
            sm = RasterFactory.createComponentSampleModel(sm,
                                                          sm.getTransferType(),
                                                          sm.getWidth(),
                                                          sm.getHeight(),
                                                          1);
            il.setSampleModel(sm);

            // Clear the ColorModel mask if needed.
            ColorModel cm = il.getColorModel(null);
            if(cm != null &&
               !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
                // Clear the mask bit if incompatible.
                il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            }
        }

        // Set an IndexColorModel on the image if:
        // a. none is provided in the layout;
        // b. source, destination, and colormap have byte data type;
        // c. the colormap has 3 bands; and
        // d. the source ColorModel is either null or is non-null
        //    and has a ColorSpace equal to CS_sRGB.
        if((layout == null || !il.isValid(ImageLayout.COLOR_MODEL_MASK)) &&
           source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE &&
           il.getSampleModel(null).getDataType() == DataBuffer.TYPE_BYTE &&
           colorMap.getDataType() == DataBuffer.TYPE_BYTE &&
           colorMap.getNumBands() == 3) {
            ColorModel cm = source.getColorModel();
            if(cm == null ||
               (cm != null && cm.getColorSpace().isCS_sRGB())) {
                int size = colorMap.getNumEntries();
                byte[][] cmap = new byte[3][256];
                for(int i = 0; i < 3; i++) {
                    byte[] band = cmap[i];
                    byte[] data = colorMap.getByteData(i);
                    int offset = colorMap.getOffset(i);
                    int end = offset + size;
                    for(int j = 0; j < offset; j++) {
                        band[j] = (byte)0;
                    }
                    for(int j = offset; j < end; j++) {
                        band[j] = data[j - offset];
                    }
                    for(int j = end; j < 256; j++) {
                        band[j] = (byte)0xFF;
                    }
                }

                il.setColorModel(new IndexColorModel(8, 256,
                                                     cmap[0], cmap[1],
                                                     cmap[2]));
            }
        }

        return il;
    }

    /**
     * Constructs an OrderedDitherOpImage object. May be used to convert a
     * single- or multi-band image into a single-band image with a color map.
     *
     * <p>The image dimensions are derived from the source image. The tile
     * grid layout, SampleModel, and ColorModel may optionally be specified
     * by an ImageLayout object.
     *
     * @param source A RenderedImage.
     * @param layout An ImageLayout optionally containing the tile grid layout,
     * SampleModel, and ColorModel, or null.
     * @param colorMap The color map to use which must have a number of bands
     * equal to the number of bands in the source image. The offset of this
     * <code>ColorCube</code> must be the same for all bands.
     * @param ditherMask An an array of <code>KernelJAI</code> objects the
     * dimension of which must equal the number of bands in the source image.
     * The <i>n</i>th element of the array contains a <code>KernelJAI</code>
     * object which represents the dither mask matrix for the corresponding
     * band. All <code>KernelJAI</code> objects in the array must have the
     * same dimensions and contain floating point values between 0.0F and 1.0F.
     */
    public OrderedDitherOpImage(RenderedImage source,
                                Map config,
                                ImageLayout layout,
                                ColorCube colorMap,
                                KernelJAI[] ditherMask) {
        // Construct as a PointOpImage.
	super(source, layoutHelper(layout, source, colorMap),
              config, true);

        // Initialize the instance variables derived from the color map.
        numBands = colorMap.getNumBands();
        mults = (int[])colorMap.getMultipliers().clone();
        dims = (int[])colorMap.getDimsLessOne().clone();
        adjustedOffset = colorMap.getAdjustedOffset();

        // Initialize the instance variables derived from the dither mask.
        maskWidth = ditherMask[0].getWidth();
        maskHeight = ditherMask[0].getHeight();

        // Initialize the data required to effect the operation.
        // XXX Postpone until first invocation of computeRect()?
        initializeDitherData(sampleModel.getTransferType(), ditherMask);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * An inner class represting a lookup table to be used in the optimized
     * implementations of ordered dithering of byte data.
     */
    private class DitherLUT {
        // Clones of color cube and dither mask data used to create the
        // dithering lookup table.
        private int[] dimsCache;
        private int[] multsCache;
        private byte[][] maskDataCache;

        // Stride values of the dither lookup table.
        public int ditherLUTBandStride;
        public int ditherLUTRowStride;
        public int ditherLUTColStride;

        // The dither lookup table.
        public byte[] ditherLUT;

        /**
         * Create an inner class object representing an ordered dither
         * lookup table for byte data.
         *
         * @param dims The color cube dimensions less one.
         * @param mults The color cube multipliers.
         * @param maskData The dither mask data scaled to byte range.
         */
        DitherLUT(int[] dims, int[] mults, byte[][] maskData) {
            // Clone the constructor parameters.
            dimsCache = (int[])dims.clone();
            multsCache = (int[])mults.clone();
            maskDataCache = new byte[maskData.length][];
            for(int i = 0; i < maskData.length; i++) {
                maskDataCache[i] = (byte[])maskData[i].clone();
            }

            // Set dither lookup table stride values.
            ditherLUTColStride = 256;
            ditherLUTRowStride = maskWidth*ditherLUTColStride;
            ditherLUTBandStride = maskHeight*ditherLUTRowStride;
            
            //
            // Construct the big dither table. If indexed as a
            // multi-dimensional array this would be equivalent to:
            //
            //   ditherLUT[band][ditherRow][ditherColumn][grayLevel]
            //
            // where ditherRow, Col are modulo the dither mask size.
            //
            // To minimize the table construction cost, precalculate
            // the bin value for a given band and gray level. Then use
            // the dithermask threshold value to determine whether to bump
            // the value up one level. Thus most of the work is done in the
            // outer loops, with a simple comparison left for the inner loop.
            //
            ditherLUT = new byte[numBands*ditherLUTBandStride];

            int pDithBand = 0;
            int maskSize2D = maskWidth*maskHeight;
            for(int band = 0; band < numBands; band++) {
                int step  = dims[band];
                int delta = mults[band];
                byte[] maskDataBand = maskData[band];
                int sum = 0;
                for(int gray = 0; gray < 256; gray++) {
                    int tmp = sum;
                    int frac = (int)(tmp & 0xff);
                    int bin = tmp >> 8;
                    int lowVal = bin * delta;
                    int highVal = lowVal + delta;
                    int pDith = pDithBand + gray;
                    for(int dcount = 0; dcount < maskSize2D; dcount++) {
                        int threshold = maskDataBand[dcount] & 0xff;
                        if(frac > threshold) {
                            ditherLUT[pDith] = (byte)(highVal & 0xff);
                        } else {
                            ditherLUT[pDith] = (byte)(lowVal & 0xff);
                        }
                        pDith += 256;
                    } // end dithermask entry
                    sum += step;
                } // end gray level
                pDithBand += ditherLUTBandStride;
            } // end band
        }

        /**
         * Determine whether the internal table of this <code>DitherLUT</code>
         * is the same as that which would be generated using the supplied
         * parameters.
         *
         * @param dims The color cube dimensions less one.
         * @param mults The color cube multipliers.
         * @param maskData The dither mask data scaled to byte range.
         *
         * @return Value indicating equivalence of dither LUTs.
         */
        public boolean equals(int[] dims, int[] mults, byte[][] maskData) {
            // Check dimensions.
            if(dims.length != dimsCache.length) {
                return false;
            }

            for(int i = 0; i < dims.length; i++) {
                if(dims[i] != dimsCache[i]) return false;
            }

            // Check multipliers.
            if(mults.length != multsCache.length) {
                return false;
            }

            for(int i = 0; i < mults.length; i++) {
                if(mults[i] != multsCache[i]) return false;
            }

            // Check dither mask.
            if(maskData.length != maskDataByte.length) {
                return false;
            }

            for(int i = 0; i < maskData.length; i++) {
                if(maskData[i].length != maskDataCache[i].length) return false;
                byte[] refData = maskDataCache[i];
                byte[] data = maskData[i];
                for(int j = 0; j < maskData[i].length; j++) {
                    if(data[j] != refData[j]) return false;
                }
            }

            return true;
        }
    } // End inner class DitherLUT.

    /**
     * Initialize data type-dependent fields including the dither mask data
     * arrays and, for optimized byte cases, the dither lookup table object.
     *
     * @param dataType The data type as defined in <code>DataBuffer</code>.
     * @param ditherMask The dither mask represented as an array of
     * <code>KernelJAI</code> objects.
     */
    private void initializeDitherData(int dataType, KernelJAI[] ditherMask) {
        switch(dataType) {
        case DataBuffer.TYPE_BYTE:
            {
                maskDataByte = new byte[ditherMask.length][];
                for(int i = 0; i < maskDataByte.length; i++) {
                    float[] maskData = ditherMask[i].getKernelData();
                    maskDataByte[i] = new byte[maskData.length];
                    for(int j = 0; j < maskData.length; j++) {
                        maskDataByte[i][j] =
                            (byte)((int)(maskData[j]*255.0F)&0xff);
                    }
                }

                initializeDitherLUT();
            }
        break;

        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            {
                int scaleFactor = (int)Short.MAX_VALUE - (int)Short.MIN_VALUE;
                maskDataInt = new int[ditherMask.length][];
                for(int i = 0; i < maskDataInt.length; i++) {
                    float[] maskData = ditherMask[i].getKernelData();
                    maskDataInt[i] = new int[maskData.length];
                    for(int j = 0; j < maskData.length; j++) {
                        maskDataInt[i][j] = (int)(maskData[j]*scaleFactor);
                    }
                }
            }
        break;

        case DataBuffer.TYPE_INT:
            {
                long scaleFactor =
                    (long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE;
                maskDataLong = new long[ditherMask.length][];
                for(int i = 0; i < maskDataLong.length; i++) {
                    float[] maskData = ditherMask[i].getKernelData();
                    maskDataLong[i] = new long[maskData.length];
                    for(int j = 0; j < maskData.length; j++) {
                        maskDataLong[i][j] = (long)(maskData[j]*scaleFactor);
                    }
                }
            }
        break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            {
                maskDataFloat = new float[ditherMask.length][];
                for(int i = 0; i < maskDataFloat.length; i++) {
                    maskDataFloat[i] = ditherMask[i].getKernelData();
                }
            }
        break;

        default:
            throw new RuntimeException(JaiI18N.getString("OrderedDitherOpImage0"));
        }
    }

    /**
     * For byte data only, initialize the dither lookup table if it is small
     * enough and set the type of ordered dither implementation to use.
     */
    private synchronized void initializeDitherLUT() {
        // Check whether a DitherLUT may be used.
        if(numBands*maskHeight*maskWidth*256 > DITHER_LUT_LENGTH_MAX) {
            odType = TYPE_OD_GENERAL; // NB: This is superfluous.
            return;
        }

        // If execution has proceeded to this point then this is one of the
        // optimized cases so set the type flag accordingly.
        odType =
            numBands == 3 ? TYPE_OD_BYTE_LUT_3BAND : TYPE_OD_BYTE_LUT_NBAND;

        // Check whether an equivalent DitherLUT object already exists.
        int index = 0;
        while(index < ditherLUTCache.size()) {
            SoftReference lutRef = (SoftReference)ditherLUTCache.get(index);
            DitherLUT lut = (DitherLUT)lutRef.get();
            if(lut == null) {
                // The reference has been cleared: remove the Vector element
                // but do not increment the loop index.
                ditherLUTCache.remove(index);
            } else {
                if(lut.equals(dims, mults, maskDataByte)) {
                    // Found an equivalent DitherLUT so use it and exit loop.
                    odLUT = lut;
                    break;
                }
                // Move on to the next Vector element.
                index++;
            }
        }

        // Create a new DitherLUT if an equivalent one was not found.
        if(odLUT == null) {
            odLUT = new DitherLUT(dims, mults, maskDataByte);
            // Cache a reference to the DitherLUT if there is room.
            if(ditherLUTCache.size() < DITHER_LUT_CACHE_LENGTH_MAX) {
                ditherLUTCache.add(new SoftReference(odLUT));
            }
        }
    }

    /**
     * Computes a tile of the dithered destination image.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Set format tags
        RasterFormatTag[] formatTags = null;
        if(ImageUtil.isBinary(getSampleModel()) &&
           !ImageUtil.isBinary(getSourceImage(0).getSampleModel())) {
            // XXX Workaround for bug 4521097. This branch of the if-block
            // should be deleted once bug 4668327 is fixed.
            RenderedImage[] sourceArray =
                new RenderedImage[] {getSourceImage(0)};
            RasterFormatTag[] sourceTags =
                RasterAccessor.findCompatibleTags(sourceArray, sourceArray[0]);
            RasterFormatTag[] destTags =
                RasterAccessor.findCompatibleTags(sourceArray, this);
            formatTags = new RasterFormatTag[] {sourceTags[0], destTags[1]};
        } else {
            // Retrieve format tags.
            formatTags = getFormatTags();
        }

        RasterAccessor src = new RasterAccessor(sources[0], destRect,  
                                                formatTags[0], 
                                                getSource(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect,  
                                                formatTags[1], getColorModel());

        switch (src.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src, dst);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(src, dst);
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
        default:
            throw new RuntimeException(JaiI18N.getString("OrderedDitherOpImage1"));
        }

        dst.copyDataToRaster();
    }

    /**
     * Computes a <code>Rectangle</code> of data for byte imagery.
     */
    private void computeRectByte(RasterAccessor src, RasterAccessor dst) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        byte[][] sData = src.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        byte[] dData = dst.getByteDataArray(0);

        int xMod = dst.getX() % maskWidth;
        int y0 = dst.getY();

        switch(odType) {
        case TYPE_OD_BYTE_LUT_3BAND:
        case TYPE_OD_BYTE_LUT_NBAND:
            int[] srcLineOffsets = (int[])sBandOffsets.clone();
            int[] srcPixelOffsets = (int[])srcLineOffsets.clone();
            int dLineOffset = dBandOffset;

            for(int h = 0; h < dheight; h++) {
                int yMod = (y0 + h)%maskHeight;

                if(odType == TYPE_OD_BYTE_LUT_3BAND) {
                    computeLineByteLUT3(sData, srcPixelOffsets, sPixelStride,
                                        dData, dLineOffset, dPixelStride,
                                        dwidth, xMod, yMod);
                } else {
                    computeLineByteLUTN(sData, srcPixelOffsets, sPixelStride,
                                        dData, dLineOffset, dPixelStride,
                                        dwidth, xMod, yMod);
                }

                for(int i = 0; i < sbands; i++) {
                    srcLineOffsets[i] += sLineStride;
                    srcPixelOffsets[i] = srcLineOffsets[i];
                }
                dLineOffset += dLineStride;
            }

            break;
        case TYPE_OD_GENERAL:
        default:
            computeRectByteGeneral(sData, sBandOffsets,
                                   sLineStride, sPixelStride,
                                   dData, dBandOffset,
                                   dLineStride, dPixelStride,
                                   dwidth, dheight,
                                   xMod, y0);
        }
    }

    /**
     * Dithers a line of 3-band byte data using a DitherLUT.
     */
    private void computeLineByteLUT3(byte[][] sData, int[] sPixelOffsets,
                                     int sPixelStride,
                                     byte[] dData, int dPixelOffset,
                                     int dPixelStride,
                                     int dwidth, int xMod, int yMod) {
        int ditherLUTBandStride = odLUT.ditherLUTBandStride;
        int ditherLUTRowStride = odLUT.ditherLUTRowStride;
        int ditherLUTColStride = odLUT.ditherLUTColStride;
        byte[] ditherLUT = odLUT.ditherLUT;

        int base = adjustedOffset;

        int dlut0 = yMod*ditherLUTRowStride;
        int dlut1 = dlut0 + ditherLUTBandStride;
        int dlut2 = dlut1 + ditherLUTBandStride;

        int dlutLimit = dlut0 + ditherLUTRowStride;

        int xDelta = xMod*ditherLUTColStride;
        int pDtab0 = dlut0 + xDelta;
        int pDtab1 = dlut1 + xDelta;
        int pDtab2 = dlut2 + xDelta;

        byte[] sData0 = sData[0];
        byte[] sData1 = sData[1];
        byte[] sData2 = sData[2];

        for(int count = dwidth; count > 0; count--) {
            int idx =
                (ditherLUT[pDtab0 + (sData0[sPixelOffsets[0]]&0xff)]&0xff) +
                (ditherLUT[pDtab1 + (sData1[sPixelOffsets[1]]&0xff)]&0xff) +
                (ditherLUT[pDtab2 + (sData2[sPixelOffsets[2]]&0xff)]&0xff);

            dData[dPixelOffset] = (byte)((idx + base)&0xff);

            sPixelOffsets[0] += sPixelStride;
            sPixelOffsets[1] += sPixelStride;
            sPixelOffsets[2] += sPixelStride;

            dPixelOffset += dPixelStride;

            pDtab0 += ditherLUTColStride;

            if(pDtab0 >= dlutLimit) {
                pDtab0 = dlut0;
                pDtab1 = dlut1;
                pDtab2 = dlut2;
            } else {
                pDtab1 += ditherLUTColStride;
                pDtab2 += ditherLUTColStride;
            }
        }
    }

    /**
     * Dithers a line of N-band byte data using a DitherLUT.
     */
    private void computeLineByteLUTN(byte[][] sData, int[] sPixelOffsets,
                                     int sPixelStride,
                                     byte[] dData, int dPixelOffset,
                                     int dPixelStride,
                                     int dwidth, int xMod, int yMod) {
        int ditherLUTBandStride = odLUT.ditherLUTBandStride;
        int ditherLUTRowStride = odLUT.ditherLUTRowStride;
        int ditherLUTColStride = odLUT.ditherLUTColStride;
        byte[] ditherLUT = odLUT.ditherLUT;

        int base = adjustedOffset;

        int dlutRow = yMod*ditherLUTRowStride;
        int dlutCol = dlutRow + xMod*ditherLUTColStride;
        int dlutLimit = dlutRow + ditherLUTRowStride;

        for(int count = dwidth; count > 0; count--) {
            int dlutBand = dlutCol;
            int idx = base;
            for(int i = 0; i < numBands; i++) {
                idx += (ditherLUT[dlutBand +
                                 (sData[i][sPixelOffsets[i]]&0xff)]&0xff);
                dlutBand += ditherLUTBandStride;
                sPixelOffsets[i] += sPixelStride;
            }

            dData[dPixelOffset] = (byte)(idx & 0xff);

            dPixelOffset += dPixelStride;

            dlutCol += ditherLUTColStride;

            if(dlutCol >= dlutLimit) {
                dlutCol = dlutRow;
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for byte imagery using the
     * general, unoptimized algorithm.
     */
    private void computeRectByteGeneral(byte[][] sData, int[] sBandOffsets,
                                        int sLineStride, int sPixelStride,
                                        byte[] dData, int dBandOffset,
                                        int dLineStride, int dPixelStride,
                                        int dwidth, int dheight,
                                        int xMod, int y0) {
        if(adjustedOffset > 0) {
            Arrays.fill(dData, (byte)(adjustedOffset & 0xff));
        }

        int sbands = sBandOffsets.length;
        for (int b = 0; b < sbands; b++) {
            byte[] s = sData[b];
            byte[] d = dData;

            byte[] maskData = maskDataByte[b];

            int sLineOffset = sBandOffsets[b];
            int dLineOffset = dBandOffset;

            for (int h = 0; h < dheight; h++) {
                int yMod = (y0 + h)%maskHeight;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = yMod*maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                for (int w = 0; w < dwidth; w++) {
                    int tmp = (s[sPixelOffset] & 0xff)*dims[b];
                    int frac = (int)(tmp & 0xff);
                    tmp >>= 8;
                    if(frac > (int)(maskData[maskIndex]&0xff)) {
                        tmp++;
                    }

                    // Accumulate the value into the destination data array.
                    int result = (d[dPixelOffset] & 0xff) + tmp*mults[b];
                    d[dPixelOffset] = (byte)(result & 0xff);

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if(++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;
            }
        }

        if(adjustedOffset < 0) {
            // Shift the result by the adjusted offset of the color map.
            int length = dData.length;
            for(int i = 0; i < length; i++) {
                dData[i] = (byte)((dData[i] & 0xff) + adjustedOffset);
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for signed short imagery.
     */
    private void computeRectShort(RasterAccessor src, RasterAccessor dst) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        short[][] sData = src.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        short[] dData = dst.getShortDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if(adjustedOffset != 0) {
            Arrays.fill(dData, (short)(adjustedOffset & 0xffff));
        }

        int xMod = dst.getX() % maskWidth;
        int y0 = dst.getY();

        for (int b = 0; b < sbands; b++) {
            short[] s = sData[b];
            short[] d = dData;

            int[] maskData = maskDataInt[b];

            int sLineOffset = sBandOffsets[b];
            int dLineOffset = dBandOffset;

            for (int h = 0; h < dheight; h++) {
                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = ((y0 + h) % maskHeight)*maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                for (int w = 0; w < dwidth; w++) {
                    int tmp = (s[sPixelOffset] - Short.MIN_VALUE)*dims[b];
                    int frac = (int)(tmp & 0xffff);

                    // Accumulate the value into the destination data array.
                    int result =
                        (int)(d[dPixelOffset]&0xffff) + (tmp >> 16)*mults[b];
                    if(frac > maskData[maskIndex]) {
                        result += mults[b];
                    }
                    d[dPixelOffset] = (short)(result & 0xffff);

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if(++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for unsigned short imagery.
     */
    private void computeRectUShort(RasterAccessor src, RasterAccessor dst) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        short[][] sData = src.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        short[] dData = dst.getShortDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if(adjustedOffset != 0) {
            Arrays.fill(dData, (short)(adjustedOffset & 0xffff));
        }

        int xMod = dst.getX() % maskWidth;
        int y0 = dst.getY();

        for (int b = 0; b < sbands; b++) {
            short[] s = sData[b];
            short[] d = dData;

            int[] maskData = maskDataInt[b];

            int sLineOffset = sBandOffsets[b];
            int dLineOffset = dBandOffset;

            for (int h = 0; h < dheight; h++) {
                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = ((y0 + h) % maskHeight)*maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                for (int w = 0; w < dwidth; w++) {
                    int tmp = (s[sPixelOffset] & 0xffff)*dims[b];
                    int frac = (int)(tmp & 0xffff);

                    // Accumulate the value into the destination data array.
                    int result =
                        (int)(d[dPixelOffset] & 0xffff) + (tmp >> 16)*mults[b];
                    if(frac > maskData[maskIndex]) {
                        result += mults[b];
                    }
                    d[dPixelOffset] = (short)(result & 0xffff);

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if(++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for integer imagery.
     */
    private void computeRectInt(RasterAccessor src, RasterAccessor dst) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        int[][] sData = src.getIntDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        int[] dData = dst.getIntDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if(adjustedOffset != 0) {
            Arrays.fill(dData, adjustedOffset);
        }

        int xMod = dst.getX() % maskWidth;
        int y0 = dst.getY();

        for (int b = 0; b < sbands; b++) {
            int[] s = sData[b];
            int[] d = dData;

            long[] maskData = maskDataLong[b];

            int sLineOffset = sBandOffsets[b];
            int dLineOffset = dBandOffset;

            for (int h = 0; h < dheight; h++) {
                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = ((y0 + h) % maskHeight)*maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                for (int w = 0; w < dwidth; w++) {
                    long tmp =
                        ((long)s[sPixelOffset] - (long)Integer.MIN_VALUE)*
                        dims[b];
                    long frac = (long)(tmp & 0xffffffff);

                    // Accumulate the value into the destination data array.
                    int result =
                        d[dPixelOffset] + ((int)(tmp >> 32))*mults[b];
                    if(frac > maskData[maskIndex]) {
                        result += mults[b];
                    }
                    d[dPixelOffset] = result;

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if(++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for float imagery.
     */
    private void computeRectFloat(RasterAccessor src, RasterAccessor dst) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        float[][] sData = src.getFloatDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        float[] dData = dst.getFloatDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if(adjustedOffset != 0) {
            Arrays.fill(dData, (float)adjustedOffset);
        }

        int xMod = dst.getX() % maskWidth;
        int y0 = dst.getY();

        for (int b = 0; b < sbands; b++) {
            float[] s = sData[b];
            float[] d = dData;

            float[] maskData = maskDataFloat[b];

            int sLineOffset = sBandOffsets[b];
            int dLineOffset = dBandOffset;

            for (int h = 0; h < dheight; h++) {
                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = ((y0 + h) % maskHeight)*maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                for (int w = 0; w < dwidth; w++) {
                    int tmp = (int)(s[sPixelOffset]*dims[b]);
                    float frac = s[sPixelOffset]*dims[b] - tmp;

                    // Accumulate the value into the destination data array.
                    float result = d[dPixelOffset] + tmp*mults[b];
                    if(frac > maskData[maskIndex]) {
                        result += mults[b];
                    }
                    d[dPixelOffset] = result;

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if(++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }
            }
        }
    }

    /**
     * Computes a <code>Rectangle</code> of data for double imagery.
     */
    private void computeRectDouble(RasterAccessor src, RasterAccessor dst) {
        int sbands = src.getNumBands();
        int sLineStride = src.getScanlineStride();
        int sPixelStride = src.getPixelStride();
        int[] sBandOffsets = src.getBandOffsets();
        double[][] sData = src.getDoubleDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int dBandOffset = dst.getBandOffset(0);
        double[] dData = dst.getDoubleDataArray(0);

        // Initialize the destination data to the color cube adjusted offset
        // to permit accumulation of the result for each band.
        if(adjustedOffset != 0) {
            Arrays.fill(dData, (double)adjustedOffset);
        }

        int xMod = dst.getX() % maskWidth;
        int y0 = dst.getY();

        for (int b = 0; b < sbands; b++) {
            double[] s = sData[b];
            double[] d = dData;

            float[] maskData = maskDataFloat[b];

            int sLineOffset = sBandOffsets[b];
            int dLineOffset = dBandOffset;

            for (int h = 0; h < dheight; h++) {
                int sPixelOffset = sLineOffset;
                int dPixelOffset = dLineOffset;

                sLineOffset += sLineStride;
                dLineOffset += dLineStride;

                // Determine the index of the first dither mask point in
                // this line for the current band.
                int maskYBase = ((y0 + h) % maskHeight)*maskWidth;

                // Determine the value one greater than the maximum valid
                // dither mask index for this band.
                int maskLimit = maskYBase + maskWidth;

                // Initialize the dither mask index which is a value
                // guaranteed to be in range.
                int maskIndex = maskYBase + xMod;

                for (int w = 0; w < dwidth; w++) {
                    int tmp = (int)(s[sPixelOffset]*dims[b]);
                    float frac = (float)(s[sPixelOffset]*dims[b] - tmp);

                    // Accumulate the value into the destination data array.
                    double result = d[dPixelOffset] + tmp*mults[b];
                    if(frac > maskData[maskIndex]) {
                        result += mults[b];
                    }
                    d[dPixelOffset] = result;

                    sPixelOffset += sPixelStride;
                    dPixelOffset += dPixelStride;

                    if(++maskIndex >= maskLimit) {
                        maskIndex = maskYBase;
                    }
                }
            }
        }
    }
}
