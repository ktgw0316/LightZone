/*
 * $RCSfile: MlibDFTOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:53 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.UntiledOpImage;
import com.lightcrafts.mediax.jai.operator.DFTDescriptor;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.media.jai.util.MathJAI;
import com.sun.medialib.mlib.*;

/**
 * An OpImage class that performs a "DFT" and "IDFT" operations.
 *
 */
final class MlibDFTOpImage extends UntiledOpImage {
    /** The mediaLib mode of the Fourier transform. */
    private int DFTMode;

    /**
     * Override the dimension specification for the destination such that it
     * has width and height which are equal to non-negative powers of 2.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            EnumeratedParameter dataNature) {
        // Set the complex flags for source and destination.
        boolean isComplexSource =
            !dataNature.equals(DFTDescriptor.REAL_TO_COMPLEX);
        boolean isComplexDest =
            !dataNature.equals(DFTDescriptor.COMPLEX_TO_REAL);

        // Get the number of source bands.
        SampleModel srcSampleModel = source.getSampleModel();
        int numSourceBands = srcSampleModel.getNumBands();

        // Check for mediaLib support of this source.
        if((isComplexSource && numSourceBands != 2) ||
           (!isComplexSource && numSourceBands != 1)) {
            // This should never occur due to checks in
            // MlibDFTRIF and MlibIDFTRIF.
            throw new RuntimeException(JaiI18N.getString("MlibDFTOpImage0"));
        }

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

        // Initialize the SampleModel creation flag.
        boolean createNewSampleModel = false;

        // Determine the number of required bands.
        int requiredNumBands = numSourceBands;
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
            int[] bandOffsets = new int[numBands];
            // Force the band offsets to be monotonically increasing as
            // mediaLib expects the real part to be in band 0 and the
            // imaginary part in band 1.
            for(int b = 0; b < numBands; b++) {
                bandOffsets[b] = b;
            }

            int lineStride = newWidth*numBands;
            sm = RasterFactory.createPixelInterleavedSampleModel(dataType,
                                                                 newWidth,
                                                                 newHeight,
                                                                 numBands,
                                                                 lineStride,
                                                                 bandOffsets);
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
     * Constructs an MlibDFTOpImage.
     *
     * @param source    a RenderedImage.

     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     * @param dataNature the nature of the source/destination data transform
     *                   (real->complex, etc.).
     * @param isForward whether the transform is forward (negative exponent).
     * @param scaleType the scaling type of the transform.
     */
    public MlibDFTOpImage(RenderedImage source,
                          Map config,
                          ImageLayout layout,
                          EnumeratedParameter dataNature,
                          boolean isForward,
                          EnumeratedParameter scaleType) {
        super(source, config, layoutHelper(layout, source, dataNature));

        if(scaleType.equals(DFTDescriptor.SCALING_NONE)) {
            DFTMode = isForward ?
                Image.MLIB_DFT_SCALE_NONE :
                Image.MLIB_IDFT_SCALE_NONE;
        } else if(scaleType.equals(DFTDescriptor.SCALING_UNITARY)) {
            DFTMode = isForward ?
                Image.MLIB_DFT_SCALE_SQRT :
                Image.MLIB_IDFT_SCALE_SQRT;
        } else if(scaleType.equals(DFTDescriptor.SCALING_DIMENSIONS)) {
            DFTMode = isForward ?
                Image.MLIB_DFT_SCALE_MXN :
                Image.MLIB_IDFT_SCALE_MXN;
        } else {
            // This should never occur due to checks in DFTDescriptor and
            // IDFTDescriptor.
            throw new RuntimeException(JaiI18N.getString("MlibDFTOpImage1"));
        }
    }

    /*
     * This method is required as a workaround to the way MediaLibAccessor
     * works. MediaLibAccessor handles ComponentSampleModels differently
     * from other SampleModels. It does a test of whether the data are
     * consecutive without respect to the band ordering. This assumes that
     * the operation is independent of band ordering. This is not true for
     * the Fourier transform as the real and imaginary parts are in bands
     * 1 and 2, respectively.
     */
    public static boolean isAcceptableSampleModel(SampleModel sm) {
        if(!(sm instanceof ComponentSampleModel)) {
            return true;
        }

        ComponentSampleModel csm = (ComponentSampleModel)sm;

        int[] bandOffsets = csm.getBandOffsets();

        if(bandOffsets.length == 2 &&
           bandOffsets[1] == bandOffsets[0] + 1) {
            return true;
        }

        return false;
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
     * Calculate the destination image from the source image.
     *
     * @param source The source Raster; should be the whole image.
     * @param dest The destination WritableRaster; should be the whole image.
     * @param destRect The destination Rectangle; should equal the destination
     * image bounds.
     */
    protected void computeImage(Raster[] sources,
                                WritableRaster dest,
                                Rectangle destRect) {
        Raster source = sources[0];

        int formatTag =
            MediaLibAccessor.findCompatibleTag(new Raster[] {source}, dest);

        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(source, mapDestRect(destRect, 0), formatTag);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest, destRect, formatTag);

        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();

        for (int i = 0; i < dstML.length; i++) {
            Image.FourierTransform(dstML[i], srcML[i],
                                                   DFTMode);
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
}
