/*
 * $RCSfile: ErrorDiffusionOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/16 17:36:08 $
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
import java.util.Map;
import com.lightcrafts.mediax.jai.ColorCube;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.UntiledOpImage;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the error diffusion operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ErrorDiffusionDescriptor</code>.
 *
 * <p>This <code>OpImage</code> performs dithering of its source image into
 * a single band image using a specified color map and error filter. For each
 * pixel in the source image the nearest entry in the color map is found and
 * the index of this entry is assigned to the <code>OpImage</code> at that
 * location. The color quantization error is calculated by mapping the index
 * back through the color map. The error in each band is then "diffused" to
 * other neighboring pixels in the source image according to the specified
 * error filter.
 *
 * @see com.lightcrafts.mediax.jai.ColorCube
 * @see com.lightcrafts.mediax.jai.KernelJAI
 * @see com.lightcrafts.mediax.jai.LookupTableJAI
 *
 * @since EA2
 *
 */
final class ErrorDiffusionOpImage extends UntiledOpImage {
    /**
     * Smallest float value which when added to unity will yield something
     * other than unity.
     */
    private static final float FLOAT_EPSILON = 1.192092896E-07F;

    /**
     * Variables used in the optimized case of 3-band byte to 1-band byte
     * with a ColorCube color map and a Floyd-Steinberg kernel.
     */
    private static final int NBANDS = 3;
    private static final int NGRAYS = 256;
    private static final int OVERSHOOT = 256;
    private static final int UNDERSHOOT = 256;
    private static final int TOTALGRAYS = (NGRAYS + UNDERSHOOT + OVERSHOOT);
    private static final int ERR_SHIFT = 8;

    /**
     * The color map which maps the <code>ErrorDiffusionOpImage</code> to
     * its source.
     */
    protected LookupTableJAI colorMap;

    /**
     * The kernel associated with the selected error filter.
     */
    protected KernelJAI errorKernel;

    /**
     * The number of bands in the source image.
     */
    private int numBandsSource;

    /**
     * Flag indicating whether this is an optimized case.
     */
    private boolean isOptimizedCase = false;

    /**
     * Minimum valid pixel value
     */
    private float minPixelValue;

    /**
     * Maximum valid pixel value
     */
    private float maxPixelValue;

    /**
     * Determines whether a kernel is the Floyd-Steinberg kernel.
     *
     * @param kernel The <code>KernelJAI</code> to examine.
     * @return Whether the kernel argument is the Floyd-Steinberg kernel.
     */
    private static boolean isFloydSteinbergKernel(KernelJAI kernel) {
        int ky = kernel.getYOrigin();

        return (kernel.getWidth() == 3 &&
                kernel.getXOrigin() == 1 &&
                kernel.getHeight() - ky == 2 &&
                Math.abs(kernel.getElement(2, ky) - 7.0F/16.0F) <
                FLOAT_EPSILON &&
                Math.abs(kernel.getElement(0, ky+1) - 3.0F/16.0F) <
                FLOAT_EPSILON &&
                Math.abs(kernel.getElement(1, ky+1) - 5.0F/16.0F) <
                FLOAT_EPSILON &&
                Math.abs(kernel.getElement(2, ky+1) - 1.0F/16.0F) <
                FLOAT_EPSILON);
    }

    /**
     * Create the dither table for the 3-band to 1-band byte optimized case.
     *
     * @param colorCube The color cube to be used in dithering.
     * @return The dither table of the optimized algorithm.
     */
    private static int[] initFloydSteinberg24To8(ColorCube colorCube) {
        // Allocate memory for the dither table.
        int[] ditherTable = new int[NBANDS*TOTALGRAYS];

        float[] thresh = new float[NGRAYS];

        //
        // Get the colorcube parameters
        //
        int[] multipliers = colorCube.getMultipliers();
        int[] dimsLessOne = colorCube.getDimsLessOne();
        int offset = colorCube.getAdjustedOffset();

        //
        //  Construct tables for each band
        //
        for (int band = 0; band < NBANDS; band++) {
            int pTab = band*TOTALGRAYS;

            //
            // Calculate the binwidth for this band, i.e. the gray level step
            // from one quantization level to the next. Do this in scaled
            // integer to maintain precision.
            //
            float binWidth = 255.0F / dimsLessOne[band];

            //
            // Pre-calculate the thresholds, so we don't have to do
            // it in the inner loops. The threshold is always the
            // midpoint of each bin, since, in error diffusion, the dithering
            // is done by the error distribution process, not by varying
            // the dither threshold as in ordered dither.
            //
            for (int i = 0; i < dimsLessOne[band]; i++) {
                thresh[i] = (i + 0.5F) * binWidth;
            }
            thresh[dimsLessOne[band]] = 256.0F;

            //
            // Populate the range below gray level zero with the same entry
            // as that for zero. The error distribution can cause undershoots
            // of as much as 255.
            //
            int tableInc = 1 << ERR_SHIFT;
            int tableValue = (-UNDERSHOOT) << ERR_SHIFT;
            for (int gray = -UNDERSHOOT; gray < 0; gray++) {
                ditherTable[pTab++] = tableValue;
                tableValue += tableInc;
            }

            //
            // Populate the main range of 0...255.
            //
            int indexContrib = 0;
            float frepValue = 0.0F;
            int repValue;
            int binNum = 0;
            float threshold = thresh[0];
            int gray = 0;
            while (gray < 256) {
                //
                // Populate all the table values up to the next threshold.
                // Since the only thing which changes is the error,
                // and it changes by one scaled gray level, we can
                // just add the increment at each iteration.
                //
                int tableBase = indexContrib;
                repValue = (int)(frepValue + 0.5F);
                while ((float)gray < threshold) {
                    ditherTable[pTab++] =
                        ((gray - repValue) << ERR_SHIFT) + tableBase;
                    gray++;
                }

                //
                // Once the gray level crosses a threshold,
                // move to the next bin threshold. Also update
                // the color contribution index step and the
                // representative value, needed to compute the error.
                //
                threshold = thresh[++binNum];
                indexContrib += multipliers[band];
                frepValue += binWidth;
            }

            //
            // Populate the range above gray level 255 with the same entry
            // as that for 255. As in the under-range case, the error
            // distribution can cause overshoots as high as 255 over max.
            //
            indexContrib -= multipliers[band];
            repValue = 255;
            tableValue = ((256 - repValue) << ERR_SHIFT) | indexContrib;

            for (gray = 256; gray < (256 + OVERSHOOT); gray++) {
                ditherTable[pTab++] = tableValue;
                tableValue += tableInc;
            }

        } // End band loop

        //
        // Add in the colormap offset value to the index contribution
        // for the first band. This eliminates the need to add it in
        // when we do the error diffusion.
        //
        int pTab = 0;
        for (int count = TOTALGRAYS; count != 0; count--) {
            ditherTable[pTab] += offset;
            pTab++;
        }

        return ditherTable;
    }

    /**
     * Force the destination image to be single-banded.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            LookupTableJAI colorMap) {
        // Create or clone the layout.
        ImageLayout il = layout == null ?
	    new ImageLayout() : (ImageLayout)layout.clone();

        // Force the destination and source origins and dimensions to coincide.
        il.setMinX(source.getMinX());
        il.setMinY(source.getMinY());
        il.setWidth(source.getWidth());
        il.setHeight(source.getHeight());

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
            sm =
                RasterFactory.createComponentSampleModel(sm,
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

        // Determine whether a larger bit depth is needed.
        int numColorMapBands = colorMap.getNumBands();
        int maxIndex = 0;
        for(int i = 0; i < numColorMapBands; i++) {
            maxIndex = Math.max(colorMap.getOffset(i) +
                                colorMap.getNumEntries() - 1,
                                maxIndex);
        }

        // Create a deeper SampleModel if needed.
        if((maxIndex > 255 && sm.getDataType() == DataBuffer.TYPE_BYTE) ||
           (maxIndex > 65535 && sm.getDataType() != DataBuffer.TYPE_INT)) {
            int dataType = maxIndex > 65535 ?
                DataBuffer.TYPE_INT : DataBuffer.TYPE_USHORT;
            sm =
                RasterFactory.createComponentSampleModel(sm,
                                                         dataType,
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
        // b. source and colormap have byte data type;
        // c. the colormap has 3 bands;
        // d. destination has byte or ushort data type.
        if((layout == null || !il.isValid(ImageLayout.COLOR_MODEL_MASK)) &&
           source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE &&
           (sm.getDataType() == DataBuffer.TYPE_BYTE ||
            sm.getDataType() == DataBuffer.TYPE_USHORT) &&
           colorMap.getDataType() == DataBuffer.TYPE_BYTE &&
           colorMap.getNumBands() == 3) {
            ColorModel cm = source.getColorModel();
            if(cm == null ||
               (cm != null && cm.getColorSpace().isCS_sRGB())) {
                int size = colorMap.getNumEntries();
                byte[][] cmap = new byte[3][maxIndex+1];
                for(int i = 0; i < 3; i++) {
                    byte[] band = cmap[i];
                    byte[] data = colorMap.getByteData(i);
                    int offset = colorMap.getOffset(i);
                    int end = offset + size;
                    for(int j = offset; j < end; j++) {
                        band[j] = data[j - offset];
                    }
                }

                int numBits =
                    sm.getDataType() == DataBuffer.TYPE_BYTE ? 8 : 16;
                il.setColorModel(new IndexColorModel(numBits, maxIndex + 1,
                                                     cmap[0], cmap[1],
                                                     cmap[2]));
            }
        }

	return il;
    }

    /**
     * Constructs an ErrorDiffusionOpImage object.
     *
     * <p>The image dimensions are derived from the source image. The tile
     * grid layout, SampleModel, and ColorModel may optionally be specified
     * by an ImageLayout object. The calculation assumes that the entire
     * color quantization error is distributed to the right and below the
     * current pixel and the filter kernel values are handled appropriately.
     *
     * @param source A RenderedImage.
     * @param layout An ImageLayout optionally containing the tile grid layout,
     * SampleModel, and ColorModel, or null.
     * @param colorMap The color map to use which must have a number of bands
     * equal to the number of bands in the source image. The offset of this
     * <code>LookupTableJAI</code> must be the same for all bands.
     * @param errorKernel The error filter kernel. This must have values
     * between 0.0 and 1.0. Only the entries to the right of and on the same
     * row as the key entry, and those entries below of the row of the key
     * entry are used; all other values are ignored. The values used must sum
     * to 1.0. Note that if a 1-by-1 error filter kernel is supplied, the value
     * of the unique kernel element is irrelevant and the output of the
     * algorithm will simply be the index in the supplied color map of the
     * nearest matching color to the source pixel at the same position.
     */
    public ErrorDiffusionOpImage(RenderedImage source,
                                 Map config,
                                 ImageLayout layout,
                                 LookupTableJAI colorMap,
                                 KernelJAI errorKernel) {
	super(source, config, layoutHelper(layout, source, colorMap));

        // Get the source sample model.
        SampleModel srcSampleModel = source.getSampleModel();

        // Cache the number of bands in the source.
        numBandsSource = srcSampleModel.getNumBands();

        // Set a reference to the LookupTableJAI.
        this.colorMap = colorMap;

        // Set a reference to the KernelJAI.
        this.errorKernel = errorKernel;

        // Determine whether this is an (read "the") optimized case.
        isOptimizedCase =
            (sampleModel.getTransferType() == DataBuffer.TYPE_BYTE &&
             srcSampleModel.getTransferType() == DataBuffer.TYPE_BYTE &&
             numBandsSource == 3 &&
             colorMap instanceof ColorCube &&
             isFloydSteinbergKernel(errorKernel));

        // Determine minumum and maximum valid pixel values
        switch (colorMap.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            // Treat byte types as unsigned bytes
            minPixelValue = 0;
            maxPixelValue = -Byte.MIN_VALUE + Byte.MAX_VALUE;
            break;
        case DataBuffer.TYPE_SHORT:
            minPixelValue = Short.MIN_VALUE;
            maxPixelValue = Short.MAX_VALUE;
            break;
        case DataBuffer.TYPE_USHORT:
            minPixelValue = 0;
            maxPixelValue = -Short.MIN_VALUE + Short.MAX_VALUE;
            break;
        case DataBuffer.TYPE_INT:
            minPixelValue = Integer.MIN_VALUE;
            maxPixelValue = Integer.MAX_VALUE;
            break;
        case DataBuffer.TYPE_FLOAT:
            minPixelValue = 0;
            maxPixelValue = Float.MAX_VALUE;
            break;
        case DataBuffer.TYPE_DOUBLE:
            minPixelValue = 0;
            maxPixelValue = Float.MAX_VALUE;
            break;
        default:
            throw new RuntimeException(
                            JaiI18N.getString("ErrorDiffusionOpImage0"));
        }

    }

    /**
     * Performs error diffusion on a specified rectangle. The sources are
     * cobbled. As error diffusion must be calculated on a line-by-line basis
     * starting at the upper left corner of the image, all image lines through
     * and including the last line of the tile containing the requested
     * <code>Rectangle</code> are calculated.
     *
     * @param sources The source image Raster.
     * @param dest A WritableRaster tile containing the area to be computed.
     * @param destRect The rectangle within dest to be processed.
     */
    protected void computeImage(Raster[] sources,
                                WritableRaster dest,
                                Rectangle destRect) {
        Raster source = sources[0];

        if (isOptimizedCase) {
            computeImageOptimized(source, dest, destRect);
        } else {
            computeImageDefault(source, dest, destRect);
        }
    }

    protected void computeImageDefault(Raster source,
                                       WritableRaster dest,
                                       Rectangle destRect) {
        // Set X-coordinate range.
        int startX = minX;
        int endX = startX + width - 1;

        // Set Y-coordinate range.
        int startY = minY;
        int endY = startY + height - 1;

        // Set the number of lines in the calculation buffer.
        int numLinesBuffer =
            errorKernel.getHeight() - errorKernel.getYOrigin();

        // Allocate memory for the calculation buffer.
        float[][] bufMem = new float[numLinesBuffer][width*numBandsSource];

        // Allocate memory for the buffer index array.
        int[] bufIdx = new int[numLinesBuffer];

        // Initialize the buffer index array and the rolling buffer.
        for(int idx = 0; idx < numLinesBuffer; idx++) {
            bufIdx[idx] = idx;
            source.getPixels(startX, startY + idx, width, 1, bufMem[idx]);
        }

        // Set variable to indicate index of last rolling buffer line.
        int lastLineBuffer = numLinesBuffer - 1;

        // Initialize some kernel-dependent constants.
        int kernelWidth = errorKernel.getWidth();
        float[] kernelData = errorKernel.getKernelData();
        int diffuseRight = kernelWidth - errorKernel.getXOrigin() - 1;
        int diffuseBelow =
            errorKernel.getHeight() - errorKernel.getYOrigin() - 1;
        int kernelOffsetRight =
            errorKernel.getYOrigin()*kernelWidth +
            errorKernel.getXOrigin() + 1;
        int kernelOffsetBelow = (errorKernel.getYOrigin() + 1)*kernelWidth;

        // Set up some arrays for looping.
        float[] currentPixel = new float[numBandsSource];
        int offset = colorMap.getOffset();
        float[] qError = new float[numBandsSource];

        // Loop over lines.
        int[] dstData = new int[width];
        for (int y = startY; y <= endY; y++) {
            int currentIndex = bufIdx[0];
            float[] currentLine = bufMem[currentIndex];

            // Loop over pixels.
            int dstOffset = 0;
            for (int x = startX, z = 0; x <= endX; x++) {
                // Copy all samples of the current pixel.
                for (int b = 0; b < numBandsSource; b++) {
                    currentPixel[b] = currentLine[z++];

                    // Clamp the current sample to the valid range
                    if (currentPixel[b] < minPixelValue ||
                        currentPixel[b] > maxPixelValue) {
                        currentPixel[b] = java.lang.Math.max(currentPixel[b],
                                                             minPixelValue);
                        currentPixel[b] = java.lang.Math.min(currentPixel[b],
                                                             maxPixelValue);
                    }
                }

                // Find the index of the nearest color in the map.
                int nearestIndex = colorMap.findNearestEntry(currentPixel);

                // Save the index in the output data buffer.
                dstData[dstOffset++] = nearestIndex;

                // Calculate the error between the nearest and actual colors.
                boolean isQuantizationError = false;
                for (int b = 0; b < numBandsSource; b++) {
                    qError[b] =
                        currentPixel[b] -
                        colorMap.lookupFloat(b, nearestIndex);
                    if (qError[b] != 0.0F) {
                        isQuantizationError = true;
                    }
                }

                // If there was error in at least one band, distribute it.
                if (isQuantizationError) {
                    // Distribute error to the right of key entry.
                    int rightCount = Math.min(diffuseRight, endX - x);
                    int kernelOffset = kernelOffsetRight;
                    int sampleOffset = z;
                    for (int u = 1; u <= rightCount; u++) {
                        for (int b = 0; b < numBandsSource; b++) {
                            currentLine[sampleOffset++] +=
                                qError[b]*kernelData[kernelOffset];
                        }
                        kernelOffset++;
                    }

                    // Distribute error below key entry.
                    int offsetLeft = Math.min(x - startX, diffuseRight);
                    int count =
                        Math.min(x + diffuseRight, endX) -
                        Math.max(x - diffuseRight, startX) + 1;
                    for (int v = 1; v <= diffuseBelow; v++) {
                        float[] line = bufMem[bufIdx[v]];
                        kernelOffset = kernelOffsetBelow;
                        sampleOffset = z - (offsetLeft + 1)*numBandsSource;
                        for (int u = 1; u <= count; u++) {
                            for (int b = 0; b < numBandsSource; b++) {
                                line[sampleOffset++] +=
                                    qError[b]*kernelData[kernelOffset];
                            }
                            kernelOffset++;
                        }
                    }
                }
            }

            //
            // Save data for the current destination line.
            //
            dest.setSamples(startX, y, destRect.width, 1, 0, dstData);

            // Rotate the buffer indexes.
            for (int k = 0; k < lastLineBuffer; k++) {
                bufIdx[k] = bufIdx[k+1];
            }
            bufIdx[lastLineBuffer] = currentIndex;

            // If available, load next image line into the last buffer line.
            if (y + numLinesBuffer < getMaxY()) {
                source.getPixels(startX, y + numLinesBuffer, width, 1,
                                 bufMem[bufIdx[lastLineBuffer]]);
            }
        }
    }

    protected void computeImageOptimized(Raster source,
                                         WritableRaster dest,
                                         Rectangle destRect) {
        // Set X-coordinate range.
        int startX = minX;
        int endX = startX + width - 1;

        // Set Y-coordinate range.
        int startY = minY;
        int endY = startY + height - 1;

        // Initialize the dither table.
        int[] ditherTable = initFloydSteinberg24To8((ColorCube)colorMap);

        // Initialize the padded source width.
        int sourceWidthPadded = source.getWidth() + 2;

        // Allocate memory for the error buffer.
        int[] errBuf = new int[sourceWidthPadded*NBANDS];

        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor srcAccessor =
            new RasterAccessor(source,
                               new Rectangle(startX, startY,
                                             source.getWidth(),
                                             source.getHeight()),
                               formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        // Set pixel and line strides.
        int srcPixelStride = srcAccessor.getPixelStride();
        int srcScanlineStride = srcAccessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();

        // Set data arrays.
        byte[] srcData0 = srcAccessor.getByteDataArray(0);
        byte[] srcData1 = srcAccessor.getByteDataArray(1);
        byte[] srcData2 = srcAccessor.getByteDataArray(2);
        byte[] dstData = dstAccessor.getByteDataArray(0);

        // Initialize line offset in each band.
        int srcLine0 = srcAccessor.getBandOffset(0);
        int srcLine1 = srcAccessor.getBandOffset(1);
        int srcLine2 = srcAccessor.getBandOffset(2);
        int dstLine = dstAccessor.getBandOffset(0);

        //
        //  For each line, calculate and distribute the error into
        //  a 3 line error buffer (one line for each band).
        //  Also accumulate the contributions of the 3 bands
        //  into the same line of the temporary output buffer.
        //
        //  The error buffer starts out with all zeroes as the
        //  amount of error to propagate forward.
        //
        for (int y = startY; y <= endY; y++) {
            // Initialize pixel offset in each line in each band.
            int srcPixel0 = srcLine0;
            int srcPixel1 = srcLine1;
            int srcPixel2 = srcLine2;
            int dstPixel = dstLine;

            //
            // Determine the error and index contribution for
            // the each band. Keep the transitory errors
            // (errA, errC and errD) in local variables
            // (hopefully registers). The calculated value
            // of errB gets put into the error buffer, to be used
            // on the next line.
            //
            // This is the logic here. Floyd-Steinberg dithering
            // distributes errors to four neighboring pixels,
            // as shown below. X is the pixel being operated on.
            //
            //    7/16 of the error goes to pixel A
            //    3/16 of the error goes to pixel B
            //    5/16 of the error goes to pixel C
            //    1/16 of the error goes to pixel D
            //
            //         X A
            //       B C D
            //
            // The error distributed to pixel A is reused immediately
            // in the calculation of the next pixel on the same line.
            // The errors distributed to B, C and D will be used on the
            // following line. As we move from left to right, the
            // new error distributed to B gets added to the error
            // at the previous C. Likewise, the new C error gets added
            // to the previous D error. So only the errors propagating
            // to position B survive in the saved error buffer. The
            // only exception is at the line end, where error C must be
            // saved. The scheme is shown below.
            //
            //      XA
            //     BCD
            //      BCD
            //       BCD
            //        BCD
            //
            // Treat the error buffer as pixel sequential.
            // This lets us use a single pointer with offsets
            // for the entries for all three bands.
            //

            //
            // Zero the error holders for all bands
            // The bands are called Red, Grn and Blu, but are
            // really just the first, second and third bands.
            //
            int errRedA = 0;
            int errRedC = 0;
            int errRedD = 0;
            int errGrnA = 0;
            int errGrnC = 0;
            int errGrnD = 0;
            int errBluA = 0;
            int errBluC = 0;
            int errBluD = 0;

            int pErr = 0;
            int dstOffset = 0;
            for (int x = startX; x <= endX; x++) {
                //
                // First band (Red)
                // The color index is initialized here.
                // Set the table pointer to the "Red" band
                //
                int pTab = UNDERSHOOT;

                int adjVal =
                    ((errRedA + errBuf[pErr+3] + 8) >> 4) +
                    (int)(srcData0[srcPixel0] & 0xff);
                srcPixel0 += srcPixelStride;
                int tabval = ditherTable[pTab+adjVal];
                int err = tabval >> 8;
                int err1 = err;
                int index = (tabval & 0xff);
                int err2 = err + err;
                errBuf[pErr] = errRedC + (err += err2); // 3/16 (B)
                errRedC = errRedD + (err += err2); // 5/16 (C)
                errRedD = err1;                    // 1/16 (D)
                errRedA = (err += err2);           // 7/16 (A)

                //
                // Second band (Green)
                // Set the table pointer to the "Green" band
                // The color index is incremented here.
                //
                pTab += TOTALGRAYS;

                adjVal =
                    ((errGrnA + errBuf[pErr+4] + 8) >> 4) +
                    (int)(srcData1[srcPixel1] & 0xff);
                srcPixel1 += srcPixelStride;
                tabval = ditherTable[pTab+adjVal];
                err = tabval >> 8;
                err1 = err;
                index += (tabval & 0xff);
                err2 = err + err;
                errBuf[pErr+1] = errGrnC + (err += err2);
                errGrnC = errGrnD + (err += err2);
                errGrnD = err1;
                errGrnA = (err += err2);

                pTab += TOTALGRAYS;

                //
                // Third band (Blue)
                // Set the table pointer to the "Blue" band
                // The color index is incremented here.
                //
                adjVal =
                    ((errBluA + errBuf[pErr+5] + 8) >> 4) +
                    (int)(srcData2[srcPixel2] & 0xff);
                srcPixel2 += srcPixelStride;
                tabval = ditherTable[pTab+adjVal];
                err = tabval >> 8;
                err1 = err;
                index += (tabval & 0xff);
                err2 = err + err;
                errBuf[pErr+2] = errBluC + (err += err2);
                errBluC = errBluD + (err += err2);
                errBluD = err1;
                errBluA = (err += err2);

                // Save the result in the output data buffer.
                dstData[dstPixel] = (byte)(index&0xff);
                dstPixel += dstPixelStride;

                pErr += 3;

            } // End pixel loop

            //
            // Save last error in line
            //
            int last = 3 * (sourceWidthPadded - 2);
            errBuf[last]   = errRedC;
            errBuf[last+1] = errGrnC;
            errBuf[last+2] = errBluC;

            // Increment offset in each band to next line.
            srcLine0 += srcScanlineStride;
            srcLine1 += srcScanlineStride;
            srcLine2 += srcScanlineStride;
            dstLine += dstScanlineStride;
        } // End scanline loop


        // Make sure that the output data is copied to the destination.
        dstAccessor.copyDataToRaster();
    }
}
