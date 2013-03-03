/*
 * $RCSfile: ComplexArithmeticOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:18 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing complex multiplication and division
 * as described in
 * <code>com.lightcrafts.mediax.jai.operator.MultiplyComplexDescriptor</code> and
 * <code>com.lightcrafts.mediax.jai.operator.DivideComplexDescriptor</code>.
 *
 * @since EA4
 *
 * @see com.lightcrafts.mediax.jai.PointOpImage
 * @see com.lightcrafts.mediax.jai.operator.MultiplyComplexDescriptor
 * @see com.lightcrafts.mediax.jai.operator.DivideComplexDescriptor
 *
 */
final class ComplexArithmeticOpImage extends PointOpImage {
    /** Flag indicating division (true) or multiplication (false). */
    protected boolean isDivision = false;

    /* Array of indices into real bands of first source. */
    private int[] s1r;

    /* Array of indices into imaginary bands of first source. */
    private int[] s1i;

    /* Array of indices into real bands of second source. */
    private int[] s2r;

    /* Array of indices into imaginary bands of second source. */
    private int[] s2i;

    /**
     * Force the destination band count to be even.
     */

    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source){
        // Create an ImageLayout or clone the one passed in.
        ImageLayout il;
        if (layout == null) {
            il = new ImageLayout();
        } else {
            il = (ImageLayout)layout.clone();
        }

        if(il.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
            SampleModel sm = il.getSampleModel(null);
            int nBands = sm.getNumBands();
            if(nBands % 2 != 0) {
                nBands++;
                sm = RasterFactory.createComponentSampleModel(sm,
                                                              sm.getTransferType(),
                                                              sm.getWidth(),
                                                              sm.getHeight(),
                                                              nBands);
		il.setSampleModel(sm);   // newly added

                // Clear the ColorModel mask if needed.
                ColorModel cm = layout.getColorModel(null);
                if(cm != null &&
                   !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
                    // Clear the mask bit if incompatible.
                    il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
                }
            }
        }

        return il;
    }

    /**
     * Constructs a <code>ComplexArithmeticOpImage</code> object.
     *
     * <p>The <code>layout</code> parameter may optionally contains the
     * tile grid layout, sample model, and/or color model. The image
     * dimension is determined by the intersection of the bounding boxes
     * of the two source images.
     *
     * <p>The image layout of the first source image, <code>source1</code>,
     * is used as the fall-back for the image layout of the destination
     * image. Any layout parameters not specified in the <code>layout</code>
     * argument are set to the same value as that of <code>source1</code>.
     *
     * @param source1    The first source image.
     * @param source2    The second source image.
     * @param layout     The destination image layout.
     * @param isDivision Whether the operation is division; if not, it's
     * multiplication.
     */
    public ComplexArithmeticOpImage(RenderedImage source1,
                                    RenderedImage source2,
                                    Map config,
                                    ImageLayout layout,
                                    boolean isDivision) {
        super(source1, source2, layoutHelper(layout, source1), config, true);

        // Cache the division parameter.
        this.isDivision = isDivision;

        // Get the source band counts.
        int numBands1 = source1.getSampleModel().getNumBands();
        int numBands2 = source2.getSampleModel().getNumBands();

        // Handle the special case (cf. descriptor).
	int numBandsDst = Math.min(numBands1, numBands2); 

	int numBandsFromHint = 0;
	if(layout != null)
	   numBandsFromHint = layout.getSampleModel(null).getNumBands();

        if(layout != null && layout.isValid(ImageLayout.SAMPLE_MODEL_MASK) &&
	   ((numBands1 == 2 && numBands2 > 2) ||
	    (numBands2 == 2 && numBands1 > 2) ||
	    (numBands1 >= numBandsFromHint && numBands2 >= numBandsFromHint && numBandsFromHint > 0)) ){
	        if(numBandsFromHint % 2 == 0){
		  numBandsDst = numBandsFromHint;
		  // Clamp the destination band count to the maximum
		  // number of bands in the sources.
		  numBandsDst = Math.min(Math.max(numBands1, numBands2),
					 numBandsDst);
		}
	}
	
	if(numBandsDst != sampleModel.getNumBands()){
            sampleModel =
                RasterFactory.createComponentSampleModel(
                        sampleModel,
                        sampleModel.getTransferType(),
                        sampleModel.getWidth(),
                        sampleModel.getHeight(),
                        numBandsDst);

            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
	}

        // Initialize index arrays.
        int numElements = sampleModel.getNumBands()/2;
        s1r = new int[numElements];
        s1i = new int[numElements];
        s2r = new int[numElements];
        s2i = new int[numElements];
        int s1Inc = numBands1 > 2 ? 2 : 0;
        int s2Inc = numBands2 > 2 ? 2 : 0;
        int i1 = 0;
        int i2 = 0;
        for(int b = 0; b < numElements; b++) {
            s1r[b] = i1;
            s1i[b] = i1 + 1;
            s2r[b] = i2;
            s2i[b] = i2 + 1;
            i1 += s1Inc;
            i2 += s2Inc;
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Calculate the product or quotient of the source images.
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

        RasterAccessor src1Accessor =
            new RasterAccessor(sources[0], destRect,  
                               formatTags[0], 
                               getSourceImage(0).getColorModel());
        RasterAccessor src2Accessor =
            new RasterAccessor(sources[1], destRect,  
                               formatTags[1], 
                               getSourceImage(1).getColorModel());
        RasterAccessor dstAccessor = 
            new RasterAccessor(dest, destRect,  
                               formatTags[2], getColorModel());

        // Branch to the method appropriate to the accessor data type.
        switch(dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src1Accessor, src2Accessor, dstAccessor);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src1Accessor, src2Accessor, dstAccessor);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(src1Accessor, src2Accessor, dstAccessor);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src1Accessor, src2Accessor, dstAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(src1Accessor, src2Accessor, dstAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(src1Accessor, src2Accessor, dstAccessor);
            break;
        default:
            // NB: This statement should be unreachable.
            throw new RuntimeException(JaiI18N.getString("ComplexArithmeticOpImage0"));
        }

        if (dstAccessor.needsClamping()) {
            dstAccessor.clampDataArrays();
        }
        // Make sure that the output data is copied to the destination.
        dstAccessor.copyDataToRaster();
    }

    private void computeRectDouble(RasterAccessor src1Accessor,
                                   RasterAccessor src2Accessor,
                                   RasterAccessor dstAccessor) {
        // Set the size of the rectangle.
        int numRows = dstAccessor.getHeight();
        int numCols = dstAccessor.getWidth();

        // Set pixel and line strides.
        int src1PixelStride = src1Accessor.getPixelStride();
        int src1ScanlineStride = src1Accessor.getScanlineStride();
        int src2PixelStride = src2Accessor.getPixelStride();
        int src2ScanlineStride = src2Accessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();

        // Loop over the destination bands.
        int numElements = sampleModel.getNumBands()/2;
        for(int element = 0; element < numElements; element++) {
            // Set band indices.
            int realBand = 2*element;
            int imagBand = realBand + 1;

            // Get the source and destination arrays for this element.
            double[] src1Real = src1Accessor.getDoubleDataArray(s1r[element]);
            double[] src1Imag = src1Accessor.getDoubleDataArray(s1i[element]);
            double[] src2Real = src2Accessor.getDoubleDataArray(s2r[element]);
            double[] src2Imag = src2Accessor.getDoubleDataArray(s2i[element]);
            double[] dstReal = dstAccessor.getDoubleDataArray(realBand);
            double[] dstImag = dstAccessor.getDoubleDataArray(imagBand);

            // Initialize the data offsets for this element.
            int src1OffsetReal = src1Accessor.getBandOffset(s1r[element]);
            int src1OffsetImag = src1Accessor.getBandOffset(s1i[element]);
            int src2OffsetReal = src2Accessor.getBandOffset(s2r[element]);
            int src2OffsetImag = src2Accessor.getBandOffset(s2i[element]);
            int dstOffsetReal = dstAccessor.getBandOffset(realBand);
            int dstOffsetImag = dstAccessor.getBandOffset(imagBand);

            // Initialize the line offsets for looping.
            int src1LineReal = src1OffsetReal;
            int src1LineImag = src1OffsetImag;
            int src2LineReal = src2OffsetReal;
            int src2LineImag = src2OffsetImag;
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int src1PixelReal = src1LineReal;
                int src1PixelImag = src1LineImag;
                int src2PixelReal = src2LineReal;
                int src2PixelImag = src2LineImag;
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;

                // Switch per line depending on operation type.
                if(isDivision) { // Division
                    for(int col = 0; col < numCols; col++) {
                        double a = src1Real[src1PixelReal];
                        double b = src1Imag[src1PixelImag];
                        double c = src2Real[src2PixelReal];
                        double d = src2Imag[src2PixelImag];

                        double denom = c*c + d*d;
                        dstReal[dstPixelReal] = (a*c + b*d)/denom;
                        dstImag[dstPixelImag] = (b*c - a*d)/denom;

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                } else { // Multiplication
                    for(int col = 0; col < numCols; col++) {
                        double a = src1Real[src1PixelReal];
                        double b = src1Imag[src1PixelImag];
                        double c = src2Real[src2PixelReal];
                        double d = src2Imag[src2PixelImag];

                        dstReal[dstPixelReal] = a*c - b*d;
                        dstImag[dstPixelImag] = a*d + b*c;

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                }

                // Increment the line offsets.
                src1LineReal += src1ScanlineStride;
                src1LineImag += src1ScanlineStride;
                src2LineReal += src2ScanlineStride;
                src2LineImag += src2ScanlineStride;
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
            }
        }
    }

    private void computeRectFloat(RasterAccessor src1Accessor,
                                  RasterAccessor src2Accessor,
                                  RasterAccessor dstAccessor) {
        // Set the size of the rectangle.
        int numRows = dstAccessor.getHeight();
        int numCols = dstAccessor.getWidth();

        // Set pixel and line strides.
        int src1PixelStride = src1Accessor.getPixelStride();
        int src1ScanlineStride = src1Accessor.getScanlineStride();
        int src2PixelStride = src2Accessor.getPixelStride();
        int src2ScanlineStride = src2Accessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();

        // Loop over the destination bands.
        int numElements = sampleModel.getNumBands()/2;
        for(int element = 0; element < numElements; element++) {
            // Set band indices.
            int realBand = 2*element;
            int imagBand = realBand + 1;

            // Get the source and destination arrays for this element.
            float[] src1Real = src1Accessor.getFloatDataArray(s1r[element]);
            float[] src1Imag = src1Accessor.getFloatDataArray(s1i[element]);
            float[] src2Real = src2Accessor.getFloatDataArray(s2r[element]);
            float[] src2Imag = src2Accessor.getFloatDataArray(s2i[element]);
            float[] dstReal = dstAccessor.getFloatDataArray(realBand);
            float[] dstImag = dstAccessor.getFloatDataArray(imagBand);

            // Initialize the data offsets for this element.
            int src1OffsetReal = src1Accessor.getBandOffset(s1r[element]);
            int src1OffsetImag = src1Accessor.getBandOffset(s1i[element]);
            int src2OffsetReal = src2Accessor.getBandOffset(s2r[element]);
            int src2OffsetImag = src2Accessor.getBandOffset(s2i[element]);
            int dstOffsetReal = dstAccessor.getBandOffset(realBand);
            int dstOffsetImag = dstAccessor.getBandOffset(imagBand);

            // Initialize the line offsets for looping.
            int src1LineReal = src1OffsetReal;
            int src1LineImag = src1OffsetImag;
            int src2LineReal = src2OffsetReal;
            int src2LineImag = src2OffsetImag;
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int src1PixelReal = src1LineReal;
                int src1PixelImag = src1LineImag;
                int src2PixelReal = src2LineReal;
                int src2PixelImag = src2LineImag;
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;

                // Switch per line depending on operation type.
                if(isDivision) { // Division
                    for(int col = 0; col < numCols; col++) {
                        float a = src1Real[src1PixelReal];
                        float b = src1Imag[src1PixelImag];
                        float c = src2Real[src2PixelReal];
                        float d = src2Imag[src2PixelImag];

                        float denom = c*c + d*d;
                        dstReal[dstPixelReal] = (a*c + b*d)/denom;
                        dstImag[dstPixelImag] = (b*c - a*d)/denom;

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                } else { // Multiplication
                    for(int col = 0; col < numCols; col++) {
                        float a = src1Real[src1PixelReal];
                        float b = src1Imag[src1PixelImag];
                        float c = src2Real[src2PixelReal];
                        float d = src2Imag[src2PixelImag];

                        dstReal[dstPixelReal] = a*c - b*d;
                        dstImag[dstPixelImag] = a*d + b*c;

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                }

                // Increment the line offsets.
                src1LineReal += src1ScanlineStride;
                src1LineImag += src1ScanlineStride;
                src2LineReal += src2ScanlineStride;
                src2LineImag += src2ScanlineStride;
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
            }
        }
    }

    private void computeRectInt(RasterAccessor src1Accessor,
                                RasterAccessor src2Accessor,
                                RasterAccessor dstAccessor) {
        // Set the size of the rectangle.
        int numRows = dstAccessor.getHeight();
        int numCols = dstAccessor.getWidth();

        // Set pixel and line strides.
        int src1PixelStride = src1Accessor.getPixelStride();
        int src1ScanlineStride = src1Accessor.getScanlineStride();
        int src2PixelStride = src2Accessor.getPixelStride();
        int src2ScanlineStride = src2Accessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();

        // Loop over the destination bands.
        int numElements = sampleModel.getNumBands()/2;
        for(int element = 0; element < numElements; element++) {
            // Set band indices.
            int realBand = 2*element;
            int imagBand = realBand + 1;

            // Get the source and destination arrays for this element.
            int[] src1Real = src1Accessor.getIntDataArray(s1r[element]);
            int[] src1Imag = src1Accessor.getIntDataArray(s1i[element]);
            int[] src2Real = src2Accessor.getIntDataArray(s2r[element]);
            int[] src2Imag = src2Accessor.getIntDataArray(s2i[element]);
            int[] dstReal = dstAccessor.getIntDataArray(realBand);
            int[] dstImag = dstAccessor.getIntDataArray(imagBand);

            // Initialize the data offsets for this element.
            int src1OffsetReal = src1Accessor.getBandOffset(s1r[element]);
            int src1OffsetImag = src1Accessor.getBandOffset(s1i[element]);
            int src2OffsetReal = src2Accessor.getBandOffset(s2r[element]);
            int src2OffsetImag = src2Accessor.getBandOffset(s2i[element]);
            int dstOffsetReal = dstAccessor.getBandOffset(realBand);
            int dstOffsetImag = dstAccessor.getBandOffset(imagBand);

            // Initialize the line offsets for looping.
            int src1LineReal = src1OffsetReal;
            int src1LineImag = src1OffsetImag;
            int src2LineReal = src2OffsetReal;
            int src2LineImag = src2OffsetImag;
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int src1PixelReal = src1LineReal;
                int src1PixelImag = src1LineImag;
                int src2PixelReal = src2LineReal;
                int src2PixelImag = src2LineImag;
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;

                // Switch per line depending on operation type.
                if(isDivision) { // Division
                    for(int col = 0; col < numCols; col++) {
                        int a = src1Real[src1PixelReal];
                        int b = src1Imag[src1PixelImag];
                        int c = src2Real[src2PixelReal];
                        int d = src2Imag[src2PixelImag];

                        float denom = c*c + d*d;
                        dstReal[dstPixelReal] =
                            ImageUtil.clampRoundInt((a*c + b*d)/denom);
                        dstImag[dstPixelImag] =
                            ImageUtil.clampRoundInt((b*c - a*d)/denom);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                } else { // Multiplication
                    for(int col = 0; col < numCols; col++) {
                        long a = src1Real[src1PixelReal];
                        long b = src1Imag[src1PixelImag];
                        long c = src2Real[src2PixelReal];
                        long d = src2Imag[src2PixelImag];

                        dstReal[dstPixelReal] = ImageUtil.clampInt(a*c - b*d);
                        dstImag[dstPixelImag] = ImageUtil.clampInt(a*d + b*c);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                }

                // Increment the line offsets.
                src1LineReal += src1ScanlineStride;
                src1LineImag += src1ScanlineStride;
                src2LineReal += src2ScanlineStride;
                src2LineImag += src2ScanlineStride;
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
            }
        }
    }

    private void computeRectUShort(RasterAccessor src1Accessor,
                                   RasterAccessor src2Accessor,
                                   RasterAccessor dstAccessor) {
        // Set the size of the rectangle.
        int numRows = dstAccessor.getHeight();
        int numCols = dstAccessor.getWidth();

        // Set pixel and line strides.
        int src1PixelStride = src1Accessor.getPixelStride();
        int src1ScanlineStride = src1Accessor.getScanlineStride();
        int src2PixelStride = src2Accessor.getPixelStride();
        int src2ScanlineStride = src2Accessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();

        // Loop over the destination bands.
        int numElements = sampleModel.getNumBands()/2;
        for(int element = 0; element < numElements; element++) {
            // Set band indices.
            int realBand = 2*element;
            int imagBand = realBand + 1;

            // Get the source and destination arrays for this element.
            short[] src1Real = src1Accessor.getShortDataArray(s1r[element]);
            short[] src1Imag = src1Accessor.getShortDataArray(s1i[element]);
            short[] src2Real = src2Accessor.getShortDataArray(s2r[element]);
            short[] src2Imag = src2Accessor.getShortDataArray(s2i[element]);
            short[] dstReal = dstAccessor.getShortDataArray(realBand);
            short[] dstImag = dstAccessor.getShortDataArray(imagBand);

            // Initialize the data offsets for this element.
            int src1OffsetReal = src1Accessor.getBandOffset(s1r[element]);
            int src1OffsetImag = src1Accessor.getBandOffset(s1i[element]);
            int src2OffsetReal = src2Accessor.getBandOffset(s2r[element]);
            int src2OffsetImag = src2Accessor.getBandOffset(s2i[element]);
            int dstOffsetReal = dstAccessor.getBandOffset(realBand);
            int dstOffsetImag = dstAccessor.getBandOffset(imagBand);

            // Initialize the line offsets for looping.
            int src1LineReal = src1OffsetReal;
            int src1LineImag = src1OffsetImag;
            int src2LineReal = src2OffsetReal;
            int src2LineImag = src2OffsetImag;
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int src1PixelReal = src1LineReal;
                int src1PixelImag = src1LineImag;
                int src2PixelReal = src2LineReal;
                int src2PixelImag = src2LineImag;
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;

                // Switch per line depending on operation type.
                if(isDivision) { // Division
                    for(int col = 0; col < numCols; col++) {
                        int a = src1Real[src1PixelReal]&0xffff;
                        int b = src1Imag[src1PixelImag]&0xffff;
                        int c = src2Real[src2PixelReal]&0xffff;
                        int d = src2Imag[src2PixelImag]&0xffff;

                        int denom = c*c + d*d;
                        dstReal[dstPixelReal] = ImageUtil.clampUShort((a*c + b*d)/denom);
                        dstImag[dstPixelImag] = ImageUtil.clampUShort((b*c - a*d)/denom);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                } else { // Multiplication
                    for(int col = 0; col < numCols; col++) {
                        int a = src1Real[src1PixelReal]&0xffff;
                        int b = src1Imag[src1PixelImag]&0xffff;
                        int c = src2Real[src2PixelReal]&0xffff;
                        int d = src2Imag[src2PixelImag]&0xffff;

                        dstReal[dstPixelReal] = ImageUtil.clampUShort(a*c - b*d);
                        dstImag[dstPixelImag] = ImageUtil.clampUShort(a*d + b*c);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                }

                // Increment the line offsets.
                src1LineReal += src1ScanlineStride;
                src1LineImag += src1ScanlineStride;
                src2LineReal += src2ScanlineStride;
                src2LineImag += src2ScanlineStride;
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
            }
        }
    }

    private void computeRectShort(RasterAccessor src1Accessor,
                                  RasterAccessor src2Accessor,
                                  RasterAccessor dstAccessor) {
        // Set the size of the rectangle.
        int numRows = dstAccessor.getHeight();
        int numCols = dstAccessor.getWidth();

        // Set pixel and line strides.
        int src1PixelStride = src1Accessor.getPixelStride();
        int src1ScanlineStride = src1Accessor.getScanlineStride();
        int src2PixelStride = src2Accessor.getPixelStride();
        int src2ScanlineStride = src2Accessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();

        // Loop over the destination bands.
        int numElements = sampleModel.getNumBands()/2;
        for(int element = 0; element < numElements; element++) {
            // Set band indices.
            int realBand = 2*element;
            int imagBand = realBand + 1;

            // Get the source and destination arrays for this element.
            short[] src1Real = src1Accessor.getShortDataArray(s1r[element]);
            short[] src1Imag = src1Accessor.getShortDataArray(s1i[element]);
            short[] src2Real = src2Accessor.getShortDataArray(s2r[element]);
            short[] src2Imag = src2Accessor.getShortDataArray(s2i[element]);
            short[] dstReal = dstAccessor.getShortDataArray(realBand);
            short[] dstImag = dstAccessor.getShortDataArray(imagBand);

            // Initialize the data offsets for this element.
            int src1OffsetReal = src1Accessor.getBandOffset(s1r[element]);
            int src1OffsetImag = src1Accessor.getBandOffset(s1i[element]);
            int src2OffsetReal = src2Accessor.getBandOffset(s2r[element]);
            int src2OffsetImag = src2Accessor.getBandOffset(s2i[element]);
            int dstOffsetReal = dstAccessor.getBandOffset(realBand);
            int dstOffsetImag = dstAccessor.getBandOffset(imagBand);

            // Initialize the line offsets for looping.
            int src1LineReal = src1OffsetReal;
            int src1LineImag = src1OffsetImag;
            int src2LineReal = src2OffsetReal;
            int src2LineImag = src2OffsetImag;
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int src1PixelReal = src1LineReal;
                int src1PixelImag = src1LineImag;
                int src2PixelReal = src2LineReal;
                int src2PixelImag = src2LineImag;
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;

                // Switch per line depending on operation type.
                if(isDivision) { // Division
                    for(int col = 0; col < numCols; col++) {
                        int a = src1Real[src1PixelReal];
                        int b = src1Imag[src1PixelImag];
                        int c = src2Real[src2PixelReal];
                        int d = src2Imag[src2PixelImag];

                        int denom = c*c + d*d;
                        dstReal[dstPixelReal] = ImageUtil.clampShort((a*c + b*d)/denom);
                        dstImag[dstPixelImag] = ImageUtil.clampShort((b*c - a*d)/denom);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                } else { // Multiplication
                    for(int col = 0; col < numCols; col++) {
                        int a = src1Real[src1PixelReal];
                        int b = src1Imag[src1PixelImag];
                        int c = src2Real[src2PixelReal];
                        int d = src2Imag[src2PixelImag];

                        dstReal[dstPixelReal] = ImageUtil.clampShort(a*c - b*d);
                        dstImag[dstPixelImag] = ImageUtil.clampShort(a*d + b*c);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                }

                // Increment the line offsets.
                src1LineReal += src1ScanlineStride;
                src1LineImag += src1ScanlineStride;
                src2LineReal += src2ScanlineStride;
                src2LineImag += src2ScanlineStride;
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
            }
        }
    }

    private void computeRectByte(RasterAccessor src1Accessor,
                                 RasterAccessor src2Accessor,
                                 RasterAccessor dstAccessor) {
        // Set the size of the rectangle.
        int numRows = dstAccessor.getHeight();
        int numCols = dstAccessor.getWidth();

        // Set pixel and line strides.
        int src1PixelStride = src1Accessor.getPixelStride();
        int src1ScanlineStride = src1Accessor.getScanlineStride();
        int src2PixelStride = src2Accessor.getPixelStride();
        int src2ScanlineStride = src2Accessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();

        // Loop over the destination bands.
        int numElements = sampleModel.getNumBands()/2;
        for(int element = 0; element < numElements; element++) {
            // Set band indices.
            int realBand = 2*element;
            int imagBand = realBand + 1;

            // Get the source and destination arrays for this element.
            byte[] src1Real = src1Accessor.getByteDataArray(s1r[element]);
            byte[] src1Imag = src1Accessor.getByteDataArray(s1i[element]);
            byte[] src2Real = src2Accessor.getByteDataArray(s2r[element]);
            byte[] src2Imag = src2Accessor.getByteDataArray(s2i[element]);
            byte[] dstReal = dstAccessor.getByteDataArray(realBand);
            byte[] dstImag = dstAccessor.getByteDataArray(imagBand);

            // Initialize the data offsets for this element.
            int src1OffsetReal = src1Accessor.getBandOffset(s1r[element]);
            int src1OffsetImag = src1Accessor.getBandOffset(s1i[element]);
            int src2OffsetReal = src2Accessor.getBandOffset(s2r[element]);
            int src2OffsetImag = src2Accessor.getBandOffset(s2i[element]);
            int dstOffsetReal = dstAccessor.getBandOffset(realBand);
            int dstOffsetImag = dstAccessor.getBandOffset(imagBand);

            // Initialize the line offsets for looping.
            int src1LineReal = src1OffsetReal;
            int src1LineImag = src1OffsetImag;
            int src2LineReal = src2OffsetReal;
            int src2LineImag = src2OffsetImag;
            int dstLineReal = dstOffsetReal;
            int dstLineImag = dstOffsetImag;

            for(int row = 0; row < numRows; row++) {
                // Initialize pixel offsets for this row.
                int src1PixelReal = src1LineReal;
                int src1PixelImag = src1LineImag;
                int src2PixelReal = src2LineReal;
                int src2PixelImag = src2LineImag;
                int dstPixelReal = dstLineReal;
                int dstPixelImag = dstLineImag;

                // Switch per line depending on operation type.
                if(isDivision) { // Division
                    for(int col = 0; col < numCols; col++) {
                        int a = src1Real[src1PixelReal]&0xff;
                        int b = src1Imag[src1PixelImag]&0xff;
                        int c = src2Real[src2PixelReal]&0xff;
                        int d = src2Imag[src2PixelImag]&0xff;

                        int denom = c*c + d*d;
                        dstReal[dstPixelReal] = ImageUtil.clampByte((a*c + b*d)/denom);
                        dstImag[dstPixelImag] = ImageUtil.clampByte((b*c - a*d)/denom);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                } else { // Multiplication
                    for(int col = 0; col < numCols; col++) {
                        int a = src1Real[src1PixelReal]&0xff;
                        int b = src1Imag[src1PixelImag]&0xff;
                        int c = src2Real[src2PixelReal]&0xff;
                        int d = src2Imag[src2PixelImag]&0xff;

                        dstReal[dstPixelReal] = ImageUtil.clampByte(a*c - b*d);
                        dstImag[dstPixelImag] = ImageUtil.clampByte(a*d + b*c);

                        src1PixelReal += src1PixelStride;
                        src1PixelImag += src1PixelStride;
                        src2PixelReal += src2PixelStride;
                        src2PixelImag += src2PixelStride;
                        dstPixelReal += dstPixelStride;
                        dstPixelImag += dstPixelStride;
                    }
                }

                // Increment the line offsets.
                src1LineReal += src1ScanlineStride;
                src1LineImag += src1ScanlineStride;
                src2LineReal += src2ScanlineStride;
                src2LineImag += src2ScanlineStride;
                dstLineReal += dstScanlineStride;
                dstLineImag += dstScanlineStride;
            }
        }
    }
}
