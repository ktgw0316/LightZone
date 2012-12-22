/*
 * $RCSfile: DFTOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:22 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.UntiledOpImage;
import com.lightcrafts.mediax.jai.operator.DFTDescriptor;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.media.jai.util.MathJAI;

/**
 * An <code>OpImage</code> implementing the forward and inverse discrete
 * Fourier transform (DFT) operations as described in
 * <code>com.lightcrafts.mediax.jai.operator.DFTDescriptor</code> and
 * <code>com.lightcrafts.mediax.jai.operator.IDFTDescriptor</code>.
 *
 * <p> The DFT operation is implemented using a one-dimensional decimation
 * in time fast Fourier transform (FFT) which is applied successively to the
 * rows and the columns of the image. All image dimensions are enlarged to the
 * next positive power of 2 greater than or equal to the respective dimension
 * unless the dimension is unity in which case it is not modified. Source
 * image values are padded with zeros when the dimension is smaller than the
 * output power-of-2 dimension.
 *
 * @since EA3
 *
 * @see com.lightcrafts.mediax.jai.UntiledOpImage
 * @see com.lightcrafts.mediax.jai.operator.DFTDescriptor
 * @see com.lightcrafts.mediax.jai.operator.IDFTDescriptor
 *
 */
public class DFTOpImage extends UntiledOpImage {
    /** The Fast Fourier Transform object. */
    FFT fft;

    /** Flag indicating whether the source image is complex. */
    protected boolean complexSrc;

    /** Flag indicating whether the destination image is complex. */
    protected boolean complexDst;

    /**
     * Override the dimension specification for the destination such that it
     * has width and height which are equal to non-negative powers of 2.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            EnumeratedParameter dataNature) {
        // Create an ImageLayout or clone the one passed in.
        ImageLayout il = layout == null ?
            new ImageLayout() : (ImageLayout)layout.clone();

        // Force the origin to coincide with that of the source.
        il.setMinX(source.getMinX());
        il.setMinY(source.getMinY());

        // Recalculate the non-unity dimensions to be a positive power of 2.
        // XXX This calculation should not be effected if an implementation
        // of the FFT which supports arbitrary dimensions is used.
        int currentWidth = il.getWidth(source);
        int currentHeight = il.getHeight(source);
        int newWidth;
        int newHeight;
        if(currentWidth == 1 && currentHeight == 1) {
            newWidth = newHeight = 1;
        } else if(currentWidth == 1 && currentHeight > 1) {
            newWidth = 1;
            newHeight = MathJAI.nextPositivePowerOf2(currentHeight);
        } else if(currentWidth > 1 && currentHeight == 1) {
            newWidth = MathJAI.nextPositivePowerOf2(currentWidth);
            newHeight = 1;
        } else { // Neither dimension equal to unity.
            newWidth = MathJAI.nextPositivePowerOf2(currentWidth);
            newHeight = MathJAI.nextPositivePowerOf2(currentHeight);
        }
        il.setWidth(newWidth);
        il.setHeight(newHeight);

        // Set the complex flags for source and destination.
        boolean isComplexSource =
            !dataNature.equals(DFTDescriptor.REAL_TO_COMPLEX);
        boolean isComplexDest =
            !dataNature.equals(DFTDescriptor.COMPLEX_TO_REAL);

        // Initialize the SampleModel creation flag.
        boolean createNewSampleModel = false;

        // Determine the number of required bands.
        SampleModel srcSampleModel = source.getSampleModel();
        int requiredNumBands = srcSampleModel.getNumBands();
        if(isComplexSource && !isComplexDest) {
            requiredNumBands /= 2;
        } else if(!isComplexSource && isComplexDest) {
            requiredNumBands *= 2;
        }

        // Set the number of bands.
        SampleModel sm = il.getSampleModel(source);
        int numBands = sm.getNumBands();
        if(numBands != requiredNumBands) {
            numBands = requiredNumBands;
            createNewSampleModel = true;
        }

        // Force the image to contain floating point data.
        int dataType = sm.getTransferType();
        if(dataType != DataBuffer.TYPE_FLOAT &&
           dataType != DataBuffer.TYPE_DOUBLE) {
            dataType = DataBuffer.TYPE_FLOAT;
            createNewSampleModel = true;
        }

        // Create a new SampleModel for the destination if necessary.
        if(createNewSampleModel) {
            sm = RasterFactory.createComponentSampleModel(sm,
                                                          dataType,
                                                          newWidth,
                                                          newHeight,
                                                          numBands);
            il.setSampleModel(sm);

            // Clear the ColorModel mask if needed.
            ColorModel cm = il.getColorModel(null);
            if(cm != null &&
               !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
                // Clear the mask bit if incompatible.
                il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            }
        }

        return il;
    }

    /**
     * Constructs a <code>DFTOpImage</code> object.
     *
     * <p>The image dimensions are the respective next positive powers of 2
     * greater than or equal to the dimensions of the source image. The tile
     * grid layout, SampleModel, and ColorModel may optionally be specified
     * by an ImageLayout object.
     *
     * @param source A RenderedImage.
     * @param layout An ImageLayout optionally containing the tile grid layout,
     * SampleModel, and ColorModel, or null.
     * @param fft The Fast Fourier Transform object.
     *
     * @see DFTDescriptor.
     */
    public DFTOpImage(RenderedImage source,
                      Map config,
                      ImageLayout layout,
                      EnumeratedParameter dataNature,
                      FFT fft) {
        super(source, config, layoutHelper(layout, source, dataNature));

        // Cache the FFT object.
        this.fft = fft;

        // Set the complex flags for source and destination.
        complexSrc = !dataNature.equals(DFTDescriptor.REAL_TO_COMPLEX);
        complexDst = !dataNature.equals(DFTDescriptor.COMPLEX_TO_REAL);
    }

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * @param destPt the position in destination image coordinates
     * to map to source image coordinates.
     *
     * @return <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return null;
    }

    /**
     * Computes the destination point corresponding to the supplied point.
     *
     * @return <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>sourcePt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return null;
    }

    /**
     * Calculate the discrete Fourier transform of the source image.
     *
     * @param source The source Raster; should be the whole image.
     * @param dest The destination WritableRaster; should be the whole image.
     * @param destRect The destination Rectangle; should be the image bounds.
     */
    protected void computeImage(Raster[] sources,
                                WritableRaster dest,
                                Rectangle destRect) {
        Raster source = sources[0];

        // Degenerate case.
        if(destRect.width == 1 && destRect.height == 1) {
            int nDstBands = sampleModel.getNumBands();
            double[] srcPixel =
                new double[source.getSampleModel().getNumBands()];
            source.getPixel(destRect.x, destRect.y, srcPixel);
            if(complexSrc && complexDst) { // Complex -> Complex
                dest.setPixel(destRect.x, destRect.y, srcPixel);
            } else if(complexSrc) { // Complex -> Real.
                for(int i = 0; i < nDstBands; i++) {
                    // Set destination to real part.
                    dest.setSample(destRect.x, destRect.y, i, srcPixel[2*i]);
                }
            } else if(complexDst) { // Real -> Complex
                for(int i = 0; i < nDstBands; i++) {
                    // Set destination real part to source.
                    dest.setSample(destRect.x, destRect.y, i,
                                   i % 2 == 0 ? srcPixel[i/2] : 0.0);
                }
            } else { // Real -> Real.
                // NB This statement should be unreachable.
                throw new RuntimeException(JaiI18N.getString("DFTOpImage1"));
            }
            return;
        }

        // Initialize to first non-unity length to be encountered.
        fft.setLength(destRect.width > 1 ? getWidth() : getHeight());

        // Get some information about the source image.
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        int srcX = source.getMinX();
        int srcY = source.getMinY();

        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor srcAccessor =
            new RasterAccessor(source,
                               new Rectangle(srcX, srcY,
                                             srcWidth, srcHeight),
                               formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        // Set data type flags.
        int srcDataType = srcAccessor.getDataType();
        int dstDataType = dstAccessor.getDataType();

        // Set pixel and line strides.
        int srcPixelStride = srcAccessor.getPixelStride();
        int srcScanlineStride = srcAccessor.getScanlineStride();
        int dstPixelStride = dstAccessor.getPixelStride();
        int dstScanlineStride = dstAccessor.getScanlineStride();
        int dstPixelStrideImag = 1;
        int dstLineStrideImag = destRect.width;
        if(complexDst) {
            dstPixelStrideImag = dstPixelStride;
            dstLineStrideImag = dstScanlineStride;
        }

        // Set indices and strides for image bands (real/imaginary).
        int srcBandIndex = 0;
        int srcBandStride = complexSrc ? 2 : 1;
        int dstBandIndex = 0;
        int dstBandStride = complexDst ? 2 : 1;

        // Get the number of components.
        int numComponents = (complexDst ?
                             dest.getSampleModel().getNumBands() / 2 :
                             dest.getSampleModel().getNumBands());

        // Loop over the components.
        for(int comp = 0; comp < numComponents; comp++) {
            // Get the real source data for this component.
            Object srcReal = srcAccessor.getDataArray(srcBandIndex);

            // Get the imaginary source data for this component if present.
            Object srcImag = null;
            if(complexSrc) {
                srcImag = srcAccessor.getDataArray(srcBandIndex+1);
            }

            // Specify the destination components.
            Object dstReal = dstAccessor.getDataArray(dstBandIndex);
            Object dstImag = null;
            if(complexDst) {
                dstImag = dstAccessor.getDataArray(dstBandIndex+1);
            } else {
                // Need to allocate an array for the entire band anyway
                // even though the destination is real because it is needed
                // for storage of the result of the row transforms.
                if(dstDataType == DataBuffer.TYPE_FLOAT) {
                    dstImag = new float[destRect.width*destRect.height];
                } else {
                    dstImag = new double[destRect.width*destRect.height];
                }
            }

            if(destRect.width > 1) {
                // Set the FFT length.
                fft.setLength(getWidth());

                // Initialize the source offsets for this component.
                int srcOffsetReal =
                    srcAccessor.getBandOffset(srcBandIndex);
                int srcOffsetImag = 0;
                if(complexSrc) {
                    srcOffsetImag =
                        srcAccessor.getBandOffset(srcBandIndex+1);
                }

                // Initialize destination offsets and strides.
                int dstOffsetReal =
                    dstAccessor.getBandOffset(dstBandIndex);
                int dstOffsetImag = 0;
                if(complexDst) {
                    dstOffsetImag =
                        dstAccessor.getBandOffset(dstBandIndex+1);
                }

                // Perform the row transforms.
                for(int row = 0; row < srcHeight; row++) {
                    // Set the input data of the FFT.
                    fft.setData(srcDataType,
                                srcReal, srcOffsetReal, srcPixelStride,
                                srcImag, srcOffsetImag, srcPixelStride,
                                srcWidth);

                    // Calculate the DFT of the row.
                    fft.transform();

                    // Get the output data of the FFT.
                    fft.getData(dstDataType,
                                dstReal, dstOffsetReal, dstPixelStride,
                                dstImag, dstOffsetImag, dstPixelStrideImag);

                    // Increment the data offsets.
                    srcOffsetReal += srcScanlineStride;
                    srcOffsetImag += srcScanlineStride;
                    dstOffsetReal += dstScanlineStride;
                    dstOffsetImag += dstLineStrideImag;
                }
            }

            if(destRect.width == 1) { // destRect.height > 1
                // NB 1) destRect.height has to be greater than one or this
                // would be the degenerate case of a single point which is
                // handled above. 2) There is no need to do setLength() on
                // the FFT object here as the length will already have been
                // set to the maximum of destRect.width amd destRect.height
                // which must be destRect.height.

                // Initialize the source offsets for this component.
                int srcOffsetReal =
                    srcAccessor.getBandOffset(srcBandIndex);
                int srcOffsetImag = 0;
                if(complexSrc) {
                    srcOffsetImag =
                        srcAccessor.getBandOffset(srcBandIndex+1);
                }

                // Initialize destination offsets and strides.
                int dstOffsetReal =
                    dstAccessor.getBandOffset(dstBandIndex);
                int dstOffsetImag = 0;
                if(complexDst) {
                    dstOffsetImag =
                        dstAccessor.getBandOffset(dstBandIndex+1);
                }

                // Set the input data of the FFT.
                fft.setData(srcDataType,
                            srcReal, srcOffsetReal, srcScanlineStride,
                            srcImag, srcOffsetImag, srcScanlineStride,
                            srcHeight);

                // Calculate the DFT of the column.
                fft.transform();

                // Get the output data of the FFT.
                fft.getData(dstDataType,
                            dstReal, dstOffsetReal, dstScanlineStride,
                            dstImag, dstOffsetImag, dstLineStrideImag);
            } else if(destRect.height > 1) { // destRect.width > 1
                // Reset the FFT length.
                fft.setLength(getHeight());

                // Initialize destination offsets and strides.
                int dstOffsetReal =
                    dstAccessor.getBandOffset(dstBandIndex);
                int dstOffsetImag = 0;
                if(complexDst) {
                    dstOffsetImag =
                        dstAccessor.getBandOffset(dstBandIndex+1);
                }

                // Perform the column transforms.
                for(int col = 0; col < destRect.width; col++) {
                    // Set the input data of the FFT.
                    fft.setData(dstDataType,
                                dstReal, dstOffsetReal, dstScanlineStride,
                                dstImag, dstOffsetImag, dstLineStrideImag,
                                destRect.height);

                    // Calculate the DFT of the column.
                    fft.transform();

                    // Get the output data of the FFT.
                    fft.getData(dstDataType,
                                dstReal, dstOffsetReal, dstScanlineStride,
                                complexDst ? dstImag : null,
                                dstOffsetImag, dstLineStrideImag);

                    // Increment the data offset.
                    dstOffsetReal += dstPixelStride;
                    dstOffsetImag += dstPixelStrideImag;
                }
            }

            // Increment the indices of the real bands in both images.
            srcBandIndex += srcBandStride;
            dstBandIndex += dstBandStride;
        }

        if (dstAccessor.needsClamping()) {
            dstAccessor.clampDataArrays();
        }

        // Make sure that the output data is copied to the destination.
        dstAccessor.copyDataToRaster();
    }
}

